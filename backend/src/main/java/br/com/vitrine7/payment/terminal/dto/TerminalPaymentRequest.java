package br.com.vitrine7.payment.terminal.dto;

import br.com.vitrine7.payment.core.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record TerminalPaymentRequest(

        @NotNull(message = "Informe o metodo de pagamento.")
        PaymentMethod method
) {
}
