package br.com.vitrine7.bar.tab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelBarTabRequest(

        @NotBlank(message = "Informe o motivo.")
        @Size(
                min = 3,
                max = 255,
                message = "O motivo deve possuir entre 3 e 255 caracteres."
        )
        String reason
) {
}
