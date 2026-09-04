package br.com.vitrine7.payment.terminal.bridge;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.payment.terminal.bridge.dto.TerminalDeviceDtos;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalTransactionEntity;
import br.com.vitrine7.payment.terminal.repository.PaymentTerminalTransactionRepository;
import br.com.vitrine7.payment.terminal.service.TerminalPaymentCompletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TerminalDeviceService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "secret", "token", "credential", "authorization", "cookie", "csrf",
            "jwt", "pin", "cvv", "card", "pan", "track", "certificate", "privatekey", "apikey");

    private final TerminalDeviceRepository repository;
    private final JdbcTemplate jdbc;
    private final TerminalBridgeProperties properties;
    private final Clock clock;
    private final PaymentTerminalTransactionRepository transactionRepository;
    private final TerminalPaymentCompletionService completionService;

    @Transactional
    public TerminalDeviceDtos.Response create(TerminalDeviceDtos.CreateRequest request, Long actorId) {
        validateProfile(request.providerProfileId(), request.providerCode());
        UUID id = repository.create(request, actorId);
        return require(id);
    }

    @Transactional(readOnly = true)
    public List<TerminalDeviceDtos.Response> list() {
        return repository.findAll(onlineCutoff());
    }

    @Transactional(readOnly = true)
    public TerminalDeviceDtos.Response require(UUID id) {
        TerminalDeviceDtos.Response response = repository.find(id, onlineCutoff());
        if (response == null) throw notFound();
        return response;
    }

    @Transactional
    public TerminalDeviceDtos.PairingCodeResponse createPairingCode(UUID deviceId, Long actorId) {
        require(deviceId);
        OffsetDateTime now = now();
        repository.invalidatePairingCodes(deviceId, now);
        String code = randomText(12);
        OffsetDateTime expiresAt = now.plus(properties.pairingCodeTtl());
        repository.insertPairingCode(deviceId, sha256(code), expiresAt, actorId);
        return new TerminalDeviceDtos.PairingCodeResponse(deviceId, code, expiresAt);
    }

    @Transactional
    public TerminalDeviceDtos.PairResponse pair(TerminalDeviceDtos.PairRequest request) {
        validateCapabilities(request.capabilities());
        byte[] supplied = sha256(request.pairingCode());
        TerminalDeviceRepository.PairingCandidate match = repository.findPairingCandidates().stream()
                .filter(candidate -> MessageDigest.isEqual(candidate.codeHash(), supplied)).findFirst()
                .orElseThrow(() -> new BusinessException("PAYMENT_TERMINAL_PAIRING_CODE_INVALID", "Codigo de pareamento invalido."));
        TerminalDeviceRepository.DevicePairingData data = repository.lockPairing(match.id());
        if (data == null || !MessageDigest.isEqual(data.codeHash(), supplied))
            throw new BusinessException("PAYMENT_TERMINAL_PAIRING_CODE_INVALID", "Codigo de pareamento invalido.");
        if (data.usedAt() != null)
            throw new BusinessException("PAYMENT_TERMINAL_PAIRING_CODE_USED", "Codigo de pareamento ja utilizado.");
        OffsetDateTime now = now();
        if (!data.expiresAt().isAfter(now))
            throw new BusinessException("PAYMENT_TERMINAL_PAIRING_CODE_EXPIRED", "Codigo de pareamento expirado.");
        if (data.providerCode() != request.providerCode())
            throw new BusinessException("PAYMENT_TERMINAL_PAIRING_CODE_INVALID", "Codigo de pareamento invalido.");

        String token = randomText(32);
        repository.pair(data, sha256(token), request, now);
        return new TerminalDeviceDtos.PairResponse(data.deviceId(), token, now);
    }

    @Transactional
    public TerminalDeviceDtos.HeartbeatResponse heartbeat(TerminalDevicePrincipal principal,
                                                           TerminalDeviceDtos.HeartbeatRequest request) {
        if (principal.providerCode() != request.providerCode())
            throw new BusinessException("PAYMENT_TERMINAL_RESULT_INVALID", "Provider do dispositivo invalido.");
        validateCapabilities(request.capabilities());
        OffsetDateTime now = now();
        repository.heartbeat(principal.deviceId(), request, now);
        return new TerminalDeviceDtos.HeartbeatResponse(principal.deviceId(), "ACTIVE", now);
    }

    public void revoke(UUID id, Long actorId) {
        require(id);
        List<UUID> pendingTransactions = jdbc.queryForList("""
                SELECT terminal_transaction_id FROM payment_terminal_commands
                WHERE device_id=? AND status IN ('QUEUED','DELIVERED','ACKNOWLEDGED')
                """, UUID.class, id);
        if (repository.revoke(id, actorId, now())) {
            jdbc.update("""
                    UPDATE payment_terminal_commands
                    SET status='CANCELLED', failure_code='PAYMENT_TERMINAL_DEVICE_REVOKED',
                        failure_message='Dispositivo revogado.', updated_at=CURRENT_TIMESTAMP, version=version+1
                    WHERE device_id=? AND status IN ('QUEUED','DELIVERED','ACKNOWLEDGED')
                    """, id);
            for (UUID transactionId : pendingTransactions) {
                PaymentTerminalTransactionEntity transaction = transactionRepository.findById(transactionId).orElse(null);
                if (transaction != null) {
                    completionService.failCommunication(transaction.getCheckoutSessionId(), transaction.getPaymentId(),
                            "PAYMENT_TERMINAL_DEVICE_REVOKED", "Dispositivo da maquininha revogado.");
                }
            }
            jdbc.update("""
                    UPDATE payment_terminal_transactions SET bridge_delivery_status='CANCELLED', updated_at=CURRENT_TIMESTAMP
                    WHERE terminal_device_id=? AND bridge_delivery_status IN ('QUEUED','DELIVERED','ACKNOWLEDGED')
                    """, id);
        }
    }

    public TerminalDevicePrincipal authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 256)
            throw new BusinessException("PAYMENT_TERMINAL_DEVICE_TOKEN_INVALID", "Token do dispositivo invalido.");
        TerminalDeviceRepository.DeviceAuth auth = repository.findAuthByTokenHash(sha256(rawToken));
        if (auth == null)
            throw new BusinessException("PAYMENT_TERMINAL_DEVICE_TOKEN_INVALID", "Token do dispositivo invalido.");
        if (auth.status() == TerminalDeviceStatus.REVOKED)
            throw new BusinessException("PAYMENT_TERMINAL_DEVICE_REVOKED", "Dispositivo revogado.");
        if (auth.status() == TerminalDeviceStatus.PENDING_PAIRING)
            throw new BusinessException("PAYMENT_TERMINAL_DEVICE_TOKEN_INVALID", "Token do dispositivo invalido.");
        return new TerminalDevicePrincipal(auth.id(), auth.providerCode());
    }

    private void validateProfile(UUID profileId, PaymentProviderCode provider) {
        if (profileId == null) return;
        Integer count = jdbc.queryForObject("SELECT count(*) FROM payment_provider_profiles WHERE id=? AND provider_code=?",
                Integer.class, profileId, provider.name());
        if (count == null || count == 0)
            throw new BusinessException("PAYMENT_PROVIDER_PROFILE_INVALID", "Perfil nao pertence ao provider informado.");
    }

    public void validateCapabilities(JsonNode node) {
        if (node == null || !node.isObject())
            throw new BusinessException("PAYMENT_TERMINAL_RESULT_INVALID", "Capabilities deve ser um objeto JSON.");
        try {
            if (node.toString().getBytes(StandardCharsets.UTF_8).length > 8192) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("PAYMENT_TERMINAL_RESULT_INVALID", "Capabilities excede o limite permitido.");
        }
        inspectKeys(node);
    }

    private void inspectKeys(JsonNode node) {
        if (node.isObject()) node.properties().forEach(entry -> {
            String normalized = entry.getKey().replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
            if (SENSITIVE_KEYS.stream().anyMatch(normalized::contains))
                throw new BusinessException("PAYMENT_TERMINAL_RESULT_INVALID", "Capabilities contem chave nao permitida.");
            inspectKeys(entry.getValue());
        });
        else if (node.isArray()) node.forEach(this::inspectKeys);
    }

    private OffsetDateTime onlineCutoff() { return now().minus(properties.offlineAfter()); }
    private OffsetDateTime now() { return OffsetDateTime.now(clock); }
    private String randomText(int bytes) { byte[] data = new byte[bytes]; RANDOM.nextBytes(data); return Base64.getUrlEncoder().withoutPadding().encodeToString(data); }
    private byte[] sha256(String value) { try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); } catch (Exception e) { throw new IllegalStateException(e); } }
    private NotFoundException notFound() { return new NotFoundException("PAYMENT_TERMINAL_DEVICE_NOT_FOUND", "Dispositivo nao encontrado."); }
}
