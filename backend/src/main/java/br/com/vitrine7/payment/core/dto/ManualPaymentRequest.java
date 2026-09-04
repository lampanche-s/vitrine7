package br.com.vitrine7.payment.core.dto;

import br.com.vitrine7.payment.core.entity.PaymentMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ManualPaymentRequest(

        @NotNull(
                message = "Informe o metodo de pagamento."
        )
        PaymentMethod method,

        @NotBlank(
                message = "Informe o motivo da confirmacao manual."
        )
        @Size(
                min = 3,
                max = 255,
                message = "O motivo deve possuir entre 3 e 255 caracteres."
        )
        String reason,

        @Min(value = 1, message = "O valor deve ser maior que zero.")
        @Max(value = 999_999_999, message = "O valor excede o limite permitido.")
        Long amountCents
) {
    public ManualPaymentRequest(PaymentMethod method, String reason) {
        this(method, reason, null);
    }
}
