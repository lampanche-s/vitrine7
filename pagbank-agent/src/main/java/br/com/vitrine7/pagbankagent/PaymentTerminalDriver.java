package br.com.vitrine7.pagbankagent;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.Optional;

public interface PaymentTerminalDriver {

    default Optional<BridgeDtos.ResultRequest> processCommand(
            BridgeDtos.CommandDelivery command
    ) throws Exception {
        if ("INITIATE_PAYMENT".equals(command.commandType())) {
            return processPayment(command);
        }

        if ("QUERY_PAYMENT".equals(command.commandType())) {
            return queryLastApprovedTransaction(command);
        }

        return unsupportedCommand(command);
    }

    Optional<BridgeDtos.ResultRequest> processPayment(
            BridgeDtos.CommandDelivery command
    ) throws Exception;

    default Optional<BridgeDtos.ResultRequest>
    queryLastApprovedTransaction(
            BridgeDtos.CommandDelivery command
    ) throws Exception {
        return unsupportedCommand(command);
    }

    private static Optional<BridgeDtos.ResultRequest>
    unsupportedCommand(
            BridgeDtos.CommandDelivery command
    ) {
        return Optional.of(
                new BridgeDtos.ResultRequest(
                        ProviderPaymentStatus.ERROR,
                        null,
                        command.commandId().toString(),
                        null,
                        "PAGBANK_AGENT_UNSUPPORTED_COMMAND",
                        "Tipo de comando nao suportado pelo agente: "
                                + command.commandType(),
                        JsonNodeFactory.instance.objectNode()
                )
        );
    }
}
