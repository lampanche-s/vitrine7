package br.com.vitrine7.payment.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CashPaymentRequest(

        @Min(value = 1, message = "O valor deve ser maior que zero.")
        @Max(value = 999_999_999, message = "O valor excede o limite permitido.")
        Long amountCents,

        @NotNull(
                message = "Informe o valor recebido."
        )
        @Min(
                value = 1,
                message = "O valor recebido deve ser maior que zero."
        )
        @Max(
                value = 999_999_999,
                message = "O valor recebido excede o limite permitido."
        )
        Long cashReceivedCents
) {
    public CashPaymentRequest(Long cashReceivedCents) {
        this(null, cashReceivedCents);
    }
}
