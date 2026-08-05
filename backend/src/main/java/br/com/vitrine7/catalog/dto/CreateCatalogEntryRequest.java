package br.com.vitrine7.catalog.dto;

import br.com.vitrine7.catalog.entity.CatalogEntryType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCatalogEntryRequest(

        @NotBlank(message = "Informe o nome.")
        @Size(
                max = 120,
                message = "O nome deve possuir no máximo 120 caracteres."
        )
        String name,

        @NotNull(message = "Informe se o cadastro é Item ou Serviço.")
        CatalogEntryType type,

        @NotNull(message = "Informe o preço.")
        @Min(
                value = 1,
                message = "O preço deve ser maior que zero."
        )
        @Max(
                value = 99_999_999,
                message = "O preço informado excede o limite permitido."
        )
        Long priceCents
) {
}
