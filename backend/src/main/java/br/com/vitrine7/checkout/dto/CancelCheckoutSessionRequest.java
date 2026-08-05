package br.com.vitrine7.checkout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelCheckoutSessionRequest(

        @NotBlank(
                message = "Informe o motivo do cancelamento."
        )
        @Size(
                min = 3,
                max = 255,
                message = "O motivo deve possuir entre 3 e 255 caracteres."
        )
        String reason
) {
}
