package br.com.vitrine7.payment.terminal.dto;

import br.com.vitrine7.payment.terminal.entity.PaymentTerminalProvider;
import br.com.vitrine7.payment.terminal.entity.TerminalSimulationOutcome;
import jakarta.validation.constraints.NotNull;

public record UpdatePaymentTerminalSettingsRequest(

        @NotNull(message = "Informe o provedor.")
        PaymentTerminalProvider provider,

        @NotNull(message = "Informe se a integracao esta ativa.")
        Boolean active,

        @NotNull(message = "Informe o resultado da simulacao.")
        TerminalSimulationOutcome simulatedOutcome
) {
}
