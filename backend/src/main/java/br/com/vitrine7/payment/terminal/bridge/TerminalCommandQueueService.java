package br.com.vitrine7.payment.terminal.bridge;

import br.com.vitrine7.checkout.service.CheckoutFinalizationService;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentResult;
import br.com.vitrine7.payment.terminal.bridge.dto.TerminalCommandDtos;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalTransactionEntity;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import br.com.vitrine7.payment.terminal.provider.ProviderPaymentStatus;
import br.com.vitrine7.payment.terminal.repository.PaymentTerminalTransactionRepository;
import br.com.vitrine7.payment.terminal.service.TerminalPaymentCompletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TerminalCommandQueueService {
    private static final Set<String> METADATA_WHITELIST =
            Set.of(
                    "simulated",
                    "outcome",
                    "cardBrand",
                    "entryMode",
                    "applicationLabel",
                    "terminalReference",
                    "userReference",
                    "transactionDate",
                    "transactionTime",
                    "terminalSerialNumber",
                    "queryType",
                    "operationType"
            );
    private static final Pattern CARD_NUMBER = Pattern.compile("(?<!\\d)\\d{13,19}(?!\\d)");

    private final TerminalCommandRepository repository;
    private final TerminalDeviceRepository deviceRepository;
    private final PaymentTerminalTransactionRepository transactionRepository;
    private final TerminalPaymentCompletionService completionService;
    private final CheckoutFinalizationService finalizationService;
    private final TerminalBridgeProperties properties;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public UUID createInitiation(UUID paymentId) {
        TerminalCommandRepository.CommandTransaction tx = repository.findTransactionByPayment(paymentId);
        if (tx == null) throw new NotFoundException("PAYMENT_TERMINAL_COMMAND_NOT_FOUND", "Transacao nao encontrada para o comando.");
        TerminalCommandRepository.CommandSnapshot existing = repository.findByTransaction(tx.id());
        if (existing != null) return existing.id();

        TerminalDeviceRepository.DeviceSelection device = deviceRepository.selectAvailable(
                PaymentProviderCode.valueOf(tx.providerCode()), tx.profileId(), tx.terminalReference(),
                tx.method(), now().minus(properties.offlineAfter()));
        if (device == null)
            throw new BusinessException("PAYMENT_TERMINAL_DEVICE_NOT_AVAILABLE", "Nenhum dispositivo compativel esta disponivel.");

        UUID id = UUID.randomUUID();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("commandId", id.toString());
        payload.put(
                "transactionId",
                tx.id().toString()
        );

        payload.put(
                "userReference",
                paymentUserReference(tx.id())
        );

        payload.put(
                "amountCents",
                tx.amountCents()
        );
        payload.put("paymentMethod", tx.method());
        payload.put("timeoutSeconds", properties.commandTimeout().toSeconds());
        payload.put("providerCode", tx.providerCode());
        payload.put("environment", tx.environment());
        payload.put("configurationVersion", tx.configurationVersion());

        repository.create(id, device.id(), tx, payload, now().plus(properties.commandTimeout()));
        return id;
    }

    @Transactional
    public UUID createReconciliationQuery(
            UUID transactionId,
            UUID deviceId
    ) {
        TerminalCommandRepository.CommandTransaction
                transaction =
                repository.findTransaction(transactionId);

        if (transaction == null) {
            throw new NotFoundException(
                    "PAYMENT_TERMINAL_TRANSACTION_NOT_FOUND",
                    "Transacao nao encontrada para conciliacao."
            );
        }

        UUID commandId =
                reconciliationCommandId(transactionId);

        ObjectNode payload =
                mapper.createObjectNode();

        payload.put(
                "commandId",
                commandId.toString()
        );

        payload.put(
                "transactionId",
                transaction.id().toString()
        );

        payload.put(
                "expectedUserReference",
                paymentUserReference(
                        transaction.id()
                )
        );

        payload.put(
                "providerCode",
                transaction.providerCode()
        );

        payload.put(
                "timeoutSeconds",
                properties.commandTimeout()
                        .toSeconds()
        );

        repository.createQuery(
                commandId,
                deviceId,
                transaction,
                payload,
                now().plus(
                        properties.commandTimeout()
                )
        );

        return commandId;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TerminalCommandDtos.Delivery reserveNext(UUID deviceId) {
        TerminalCommandDtos.Delivery delivery = repository.reserveNext(deviceId, now());
        if (delivery != null) repository.mirrorDelivery(delivery.commandId(), "DELIVERED");
        return delivery;
    }

    @Transactional
    public TerminalCommandDtos.Acknowledgement acknowledge(UUID commandId, UUID deviceId) {
        OffsetDateTime now = now();
        TerminalCommandRepository.CommandSnapshot command = requireOwned(commandId, deviceId);
        if (command.status().equals("EXPIRED") || !command.expiresAt().isAfter(now))
            throw new BusinessException("PAYMENT_TERMINAL_COMMAND_EXPIRED", "Comando expirado.");
        if (Set.of("COMPLETED", "FAILED", "CANCELLED").contains(command.status()))
            throw new BusinessException("PAYMENT_TERMINAL_COMMAND_CONFLICT", "Comando ja finalizado.");
        if (!command.status().equals("ACKNOWLEDGED")) {
            if (repository.acknowledge(commandId, deviceId, now) == 0)
                throw new BusinessException("PAYMENT_TERMINAL_COMMAND_CONFLICT", "Comando nao pode ser confirmado neste estado.");
            repository.mirrorDelivery(commandId, "ACKNOWLEDGED");
        }
        return new TerminalCommandDtos.Acknowledgement(commandId, "ACKNOWLEDGED", now);
    }

    public TerminalCommandDtos.ResultResponse submitResult(
            UUID commandId,
            UUID deviceId,
            TerminalCommandDtos.ResultRequest request
    ) {
        TerminalCommandRepository.CommandSnapshot before =
                requireOwned(commandId, deviceId);

        TerminalCommandDtos.ResultRequest effectiveRequest =
                reconcileQueryResult(
                        before,
                        request
                );

        JsonNode normalized =
                normalize(effectiveRequest);

        OffsetDateTime now = now();

        if (Set.of("COMPLETED", "FAILED").contains(before.status())) {

            if (!before.result().equals(normalized)) {
                throw new BusinessException(
                        "PAYMENT_TERMINAL_COMMAND_CONFLICT",
                        "Resultado conflitante para o comando."
                );
            }

            completeFinancialFlowIfRequired(
                    before,
                    effectiveRequest,
                    now
            );

            return new TerminalCommandDtos.ResultResponse(
                    commandId,
                    before.status(),
                    true
            );
        }

        boolean expired =
                before.status().equals("EXPIRED")
                        || !before.expiresAt()
                                .isAfter(now);

        if (before.status().equals("CANCELLED")) {
            throw new BusinessException(
                    "PAYMENT_TERMINAL_COMMAND_CONFLICT",
                    "Comando cancelado."
            );
        }

        if (expired
                && (
                effectiveRequest.status()
                        == ProviderPaymentStatus.PENDING
                        || effectiveRequest.status()
                        == ProviderPaymentStatus.PROCESSING
        )) {

            throw new BusinessException(
                    "PAYMENT_TERMINAL_COMMAND_EXPIRED",
                    "Comando expirado."
            );
        }

        if (effectiveRequest.status()
                == ProviderPaymentStatus.PENDING
                || effectiveRequest.status()
                == ProviderPaymentStatus.PROCESSING) {

            transactionTemplate.executeWithoutResult(
                    ignored -> {
                        if (repository.recordProgress(
                                commandId,
                                deviceId,
                                normalized,
                                now
                        ) == 0) {
                            throw new BusinessException(
                                    "PAYMENT_TERMINAL_COMMAND_CONFLICT",
                                    "Comando foi alterado por outra requisicao."
                            );
                        }

                        repository.mirrorDelivery(
                                commandId,
                                "ACKNOWLEDGED"
                        );
                    }
            );

            return new TerminalCommandDtos.ResultResponse(
                    commandId,
                    "ACKNOWLEDGED",
                    false
            );
        }

        String commandStatus =
                effectiveRequest.status()
                        == ProviderPaymentStatus.ERROR
                        || effectiveRequest.status()
                        == ProviderPaymentStatus.UNKNOWN
                        ? "FAILED"
                        : "COMPLETED";

        completeFinancialFlowIfRequired(
                before,
                effectiveRequest,
                now
        );

        AtomicBoolean replayed =
                new AtomicBoolean(false);

        transactionTemplate.executeWithoutResult(
                ignored -> finishAtomically(
                        commandId,
                        deviceId,
                        normalized,
                        commandStatus,
                        now,
                        replayed
                )
        );

        TerminalCommandRepository.CommandSnapshot finished =
                repository.find(commandId);

        return new TerminalCommandDtos.ResultResponse(
                commandId,
                finished.status(),
                replayed.get()
        );
    }

    private TerminalCommandDtos.ResultRequest
    reconcileQueryResult(
            TerminalCommandRepository.CommandSnapshot command,
            TerminalCommandDtos.ResultRequest request
    ) {
        if (!command.type().equals("QUERY_PAYMENT")
                || request.status()
                != ProviderPaymentStatus.APPROVED) {

            return request;
        }

        String expectedReference =
                paymentUserReference(
                        command.transactionId()
                );

        String returnedReference =
                text(
                        request.metadata(),
                        "userReference"
                );

        if (returnedReference != null
                && expectedReference.equalsIgnoreCase(
                        returnedReference.trim()
                )) {

            return request;
        }

        return new TerminalCommandDtos.ResultRequest(
                ProviderPaymentStatus.UNKNOWN,
                null,
                request.providerRequestId(),
                null,
                "PAYMENT_TERMINAL_RECONCILIATION_MISMATCH",
                "A ultima transacao aprovada nao pertence "
                        + "ao pagamento pendente.",
                request.metadata().deepCopy()
        );
    }

    private void completeFinancialFlowIfRequired(
            TerminalCommandRepository.CommandSnapshot command,
            TerminalCommandDtos.ResultRequest request,
            OffsetDateTime now
    ) {
        boolean shouldComplete =
                command.type().equals(
                        "INITIATE_PAYMENT"
                )
                        || (
                        command.type().equals(
                                "QUERY_PAYMENT"
                        )
                                && request.status()
                                == ProviderPaymentStatus.APPROVED
                );

        if (!shouldComplete) {
            return;
        }

        completeFinancialFlow(
                command.transactionId(),
                request,
                now
        );

        if (command.type().equals(
                "QUERY_PAYMENT"
        )) {
            repository.mirrorTransactionDelivery(
                    command.transactionId(),
                    "COMPLETED"
            );
        }
    }

    private void finishAtomically(
            UUID commandId,
            UUID deviceId,
            JsonNode normalized,
            String commandStatus,
            OffsetDateTime now,
            AtomicBoolean replayed
    ) {
        int updated = repository.finish(
                commandId,
                deviceId,
                commandStatus,
                normalized,
                text(normalized, "failureCode"),
                text(normalized, "failureMessage"),
                now
        );

        if (updated == 1) {
            repository.mirrorDelivery(
                    commandId,
                    commandStatus
            );
            return;
        }

        TerminalCommandRepository.CommandSnapshot current =
                repository.find(commandId);

        if (current != null
                && Set.of("COMPLETED", "FAILED").contains(current.status())
                && current.result().equals(normalized)) {

            replayed.set(true);
            return;
        }

        throw new BusinessException(
                "PAYMENT_TERMINAL_COMMAND_CONFLICT",
                "Comando foi alterado por outra requisicao."
        );
    }

    public ProviderPaymentResult waitForResult(UUID commandId) {
        OffsetDateTime deadline = now().plus(properties.commandTimeout());
        while (now().isBefore(deadline)) {
            TerminalCommandRepository.CommandSnapshot command = repository.find(commandId);
            if (command == null) throw new NotFoundException("PAYMENT_TERMINAL_COMMAND_NOT_FOUND", "Comando nao encontrado.");
            if (Set.of("COMPLETED", "FAILED").contains(command.status())) return toProviderResult(command.result());
            if (command.status().equals("EXPIRED") || command.status().equals("CANCELLED"))
                throw new BusinessException(command.status().equals("EXPIRED") ? "PAYMENT_TERMINAL_COMMAND_EXPIRED" : "PAYMENT_TERMINAL_COMMAND_CONFLICT",
                        "Comando nao pode mais ser concluido.");
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        throw new BusinessException("PAYMENT_TERMINAL_COMMAND_EXPIRED", "Tempo limite do comando excedido.");
    }

    private void completeFinancialFlow(UUID transactionId, TerminalCommandDtos.ResultRequest request, OffsetDateTime now) {
        PaymentTerminalTransactionEntity tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("PAYMENT_TERMINAL_COMMAND_NOT_FOUND", "Transacao do comando nao encontrada."));
        if (request.status() == ProviderPaymentStatus.ERROR || request.status() == ProviderPaymentStatus.UNKNOWN) {
            completionService.failCommunication(tx.getCheckoutSessionId(), tx.getPaymentId(),
                    safe(request.failureCode(), 80) == null ? "PAYMENT_TERMINAL_RESULT_ERROR" : safe(request.failureCode(), 80),
                    safe(request.failureMessage(), 255));
        } else {
            completionService.complete(tx.getCheckoutSessionId(), tx.getPaymentId(),
                    new ProviderPaymentResult(request.status(), safe(request.providerReference(), 120),
                            safe(request.providerRequestId(), 120), safe(request.failureCode(), 80),
                            safe(request.failureMessage(), 255), metadataMap(request.metadata()), now),
                    tx.getCreatedByUserId());
        }
        if (request.status() == ProviderPaymentStatus.APPROVED)
            finalizationService.finalizeCheckoutIfSupported(tx.getCheckoutSessionId(), tx.getCreatedByUserId());
    }

    private JsonNode normalize(TerminalCommandDtos.ResultRequest request) {
        validateSafe(request.providerReference()); validateSafe(request.providerRequestId());
        validateSafe(request.authorizationCode()); validateSafe(request.failureCode()); validateSafe(request.failureMessage());
        if (request.status() == ProviderPaymentStatus.APPROVED && (request.providerReference() == null || request.providerReference().isBlank()))
            throw invalid("Referencia do provider e obrigatoria para aprovacao.");
        if (request.metadata() == null || !request.metadata().isObject()) throw invalid("Metadata deve ser objeto JSON.");
        Iterator<Map.Entry<String, JsonNode>> fields = request.metadata().properties().iterator();
        int count = 0;
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next(); count++;
            if (count > 12 || !METADATA_WHITELIST.contains(field.getKey())
                    || field.getValue().isObject() || field.getValue().isArray())
                throw invalid("Metadata contem campo nao permitido.");
            validateSafe(field.getValue().asString());
        }
        ObjectNode out = mapper.createObjectNode();
        out.put("status", request.status().name());
        put(out, "providerReference", request.providerReference());
        put(out, "providerRequestId", request.providerRequestId());
        put(out, "authorizationCode", request.authorizationCode());
        put(out, "failureCode", request.failureCode());
        put(out, "failureMessage", safe(request.failureMessage(), 255));
        out.set("metadata", request.metadata().deepCopy());
        return out;
    }

    private ProviderPaymentResult toProviderResult(JsonNode result) {
        return new ProviderPaymentResult(ProviderPaymentStatus.valueOf(result.path("status").asString()),
                text(result, "providerReference"), text(result, "providerRequestId"), text(result, "failureCode"),
                text(result, "failureMessage"), metadataMap(result.path("metadata")), now());
    }

    private Map<String, Object> metadataMap(JsonNode node) {
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
        if (node != null && node.isObject()) node.properties().forEach(e -> {
            if (METADATA_WHITELIST.contains(e.getKey())) {
                JsonNode value = e.getValue();
                if (value.isBoolean()) result.put(e.getKey(), value.asBoolean());
                else if (value.isNumber()) result.put(e.getKey(), value.asLong());
                else result.put(e.getKey(), safe(value.asString(), 120));
            }
        });
        return Map.copyOf(result);
    }

    private TerminalCommandRepository.CommandSnapshot requireOwned(UUID commandId, UUID deviceId) {
        TerminalCommandRepository.CommandSnapshot command = repository.find(commandId);
        if (command == null || !command.deviceId().equals(deviceId))
            throw new NotFoundException("PAYMENT_TERMINAL_COMMAND_NOT_FOUND", "Comando nao encontrado.");
        return command;
    }
    private void validateSafe(String value) { if (value != null && CARD_NUMBER.matcher(value).find()) throw invalid("Resultado contem dado sensivel."); }
    private String safe(String value, int limit) { if (value == null || value.isBlank()) return null; String v=value.trim().replaceAll("[\\r\\n\\t]+", " "); return v.length()<=limit?v:v.substring(0,limit); }
    private void put(ObjectNode node, String key, String value) { String safe=safe(value, key.equals("failureMessage")?255:120); if(safe==null) node.putNull(key); else node.put(key,safe); }
    private String text(JsonNode node, String key) { JsonNode v=node.get(key); return v==null||v.isNull()?null:v.asString(); }
    private BusinessException invalid(String message) { return new BusinessException("PAYMENT_TERMINAL_RESULT_INVALID", message); }
    private static UUID reconciliationCommandId(
            UUID transactionId
    ) {
        return UUID.nameUUIDFromBytes(
                (
                        "QUERY_PAYMENT:"
                                + transactionId
                ).getBytes(StandardCharsets.UTF_8)
        );
    }

    static String paymentUserReference(
            UUID transactionId
    ) {
        return transactionId
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase(Locale.ROOT);
    }

    private OffsetDateTime now() { return OffsetDateTime.now(clock); }
}
