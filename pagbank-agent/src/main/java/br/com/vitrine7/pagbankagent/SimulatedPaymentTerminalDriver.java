package br.com.vitrine7.pagbankagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class SimulatedPaymentTerminalDriver implements PaymentTerminalDriver {

    private final PaymentOutcomeMode outcomeMode;
    private final ObjectMapper mapper;

    public SimulatedPaymentTerminalDriver(
            PaymentOutcomeMode outcomeMode,
            ObjectMapper mapper
    ) {
        this.outcomeMode = outcomeMode;
        this.mapper = mapper;
    }

    @Override
    public Optional<BridgeDtos.ResultRequest> processPayment(
            BridgeDtos.CommandDelivery command
    ) {
        if (outcomeMode == PaymentOutcomeMode.TIMEOUT) {
            return Optional.empty();
        }

        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("terminalReference", "PAGBANK-LOCAL-DEV");
        metadata.put("entryMode", "SIMULATED");

        String userReference =
                command.payload()
                        .path("userReference")
                        .asText(
                                command.transactionId()
                                        .toString()
                                        .replace("-", "")
                                        .substring(0, 10)
                                        .toUpperCase(
                                                Locale.ROOT
                                        )
                        );

        metadata.put(
                "userReference",
                userReference
        );

        String providerReference =
                "PAGBANK-DEV-" + UUID.randomUUID();
        String providerRequestId =
                command.commandId().toString();

        return Optional.of(switch (outcomeMode) {
            case APPROVED -> new BridgeDtos.ResultRequest(
                    ProviderPaymentStatus.APPROVED,
                    providerReference,
                    providerRequestId,
                    "123456",
                    null,
                    null,
                    metadata
            );
            case DECLINED -> new BridgeDtos.ResultRequest(
                    ProviderPaymentStatus.DECLINED,
                    providerReference,
                    providerRequestId,
                    null,
                    "51",
                    "Transacao recusada pelo terminal PagBank simulado.",
                    metadata
            );
            case ERROR -> new BridgeDtos.ResultRequest(
                    ProviderPaymentStatus.ERROR,
                    null,
                    providerRequestId,
                    null,
                    "PAGBANK_AGENT_ERROR",
                    "Falha simulada no agente local PagBank.",
                    metadata
            );
            case TIMEOUT -> throw new IllegalStateException(
                    "Timeout ja tratado antes do resultado."
            );
        });
    }

    @Override
    public Optional<BridgeDtos.ResultRequest>
    queryLastApprovedTransaction(
            BridgeDtos.CommandDelivery command
    ) {
        if (outcomeMode == PaymentOutcomeMode.TIMEOUT) {
            return Optional.empty();
        }

        ObjectNode metadata =
                mapper.createObjectNode();

        metadata.put(
                "terminalReference",
                "PAGBANK-LOCAL-DEV"
        );

        metadata.put(
                "entryMode",
                "SIMULATED"
        );

        metadata.put(
                "queryType",
                "LAST_APPROVED_TRANSACTION"
        );

        String expectedUserReference =
                command.payload()
                        .path("expectedUserReference")
                        .asText("SIMULATED");

        metadata.put(
                "userReference",
                expectedUserReference
        );

        metadata.put(
                "transactionDate",
                "2026-08-01"
        );

        metadata.put(
                "transactionTime",
                "12:00:00"
        );

        metadata.put(
                "terminalSerialNumber",
                "SIMULATED"
        );

        if (outcomeMode == PaymentOutcomeMode.ERROR) {
            return Optional.of(
                    new BridgeDtos.ResultRequest(
                            ProviderPaymentStatus.ERROR,
                            null,
                            command.commandId()
                                    .toString(),
                            null,
                            "PAGBANK_AGENT_QUERY_ERROR",
                            "Falha simulada ao consultar "
                                    + "a ultima transacao.",
                            metadata
                    )
            );
        }

        if (outcomeMode == PaymentOutcomeMode.DECLINED) {
            return Optional.of(
                    new BridgeDtos.ResultRequest(
                            ProviderPaymentStatus.UNKNOWN,
                            null,
                            command.commandId()
                                    .toString(),
                            null,
                            "PAGBANK_LAST_APPROVED_NOT_FOUND",
                            "Nenhuma transacao aprovada "
                                    + "foi encontrada.",
                            metadata
                    )
            );
        }

        return Optional.of(
                new BridgeDtos.ResultRequest(
                        ProviderPaymentStatus.APPROVED,
                        "PAGBANK-DEV-LAST-APPROVED",
                        command.commandId().toString(),
                        "123456",
                        null,
                        null,
                        metadata
                )
        );
    }


}
