package br.com.vitrine7.payment.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentReversalRequest(

        @NotBlank(
                message = "O motivo do estorno e obrigatorio."
        )
        @Size(
                min = 3,
                max = 255,
                message = "O motivo deve possuir entre 3 e 255 caracteres."
        )
        String reason
) {
}
