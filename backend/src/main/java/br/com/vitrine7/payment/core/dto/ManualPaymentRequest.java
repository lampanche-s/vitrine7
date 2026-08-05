package br.com.vitrine7.payment.core.dto;

import br.com.vitrine7.payment.core.entity.PaymentMethod;
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
        String reason
) {
}
