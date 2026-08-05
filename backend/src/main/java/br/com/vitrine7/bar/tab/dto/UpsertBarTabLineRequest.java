package br.com.vitrine7.bar.tab.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpsertBarTabLineRequest(

        @NotNull(message = "Informe a quantidade.")
        @Min(value = 1, message = "A quantidade minima e 1.")
        @Max(
                value = 10_000,
                message = "A quantidade excede o limite permitido."
        )
        Integer quantity,

        @Min(
                value = 1,
                message = "O valor unitario deve ser maior que zero."
        )
        @Max(
                value = 99_999_999,
                message = "O valor unitario excede o limite permitido."
        )
        Long unitPriceCents
) {
}
