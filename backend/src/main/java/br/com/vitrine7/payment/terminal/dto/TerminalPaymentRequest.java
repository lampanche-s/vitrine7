package br.com.vitrine7.payment.terminal.dto;

import br.com.vitrine7.payment.core.entity.PaymentMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TerminalPaymentRequest(

        @NotNull(message = "Informe o metodo de pagamento.")
        PaymentMethod method,

        @Min(value = 1, message = "O valor deve ser maior que zero.")
        @Max(value = 999_999_999, message = "O valor excede o limite permitido.")
        Long amountCents
) {
    public TerminalPaymentRequest(PaymentMethod method) {
        this(method, null);
    }
}
