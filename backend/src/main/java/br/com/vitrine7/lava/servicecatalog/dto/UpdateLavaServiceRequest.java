package br.com.vitrine7.lava.servicecatalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateLavaServiceRequest(

        @NotBlank(message = "Informe o nome do serviço.")
        @Size(
                max = 120,
                message = "O nome deve possuir no máximo 120 caracteres."
        )
        String name,

        @NotBlank(message = "Informe a categoria do serviço.")
        @Size(
                max = 80,
                message = "A categoria deve possuir no máximo 80 caracteres."
        )
        String category,

        @NotNull(
                message = "Informe o preço para veículo pequeno."
        )
        @Min(
                value = 1,
                message = "O preço para veículo pequeno deve ser maior que zero."
        )
        @Max(
                value = 99_999_999,
                message = "O preço para veículo pequeno excede o limite permitido."
        )
        Long smallVehiclePriceCents,

        @NotNull(
                message = "Informe o preço para veículo médio."
        )
        @Min(
                value = 1,
                message = "O preço para veículo médio deve ser maior que zero."
        )
        @Max(
                value = 99_999_999,
                message = "O preço para veículo médio excede o limite permitido."
        )
        Long mediumVehiclePriceCents,

        @NotNull(message = "Informe a duração do serviço.")
        @Min(
                value = 1,
                message = "A duração deve ser de pelo menos 1 minuto."
        )
        @Max(
                value = 1440,
                message = "A duração não pode exceder 1440 minutos."
        )
        Integer durationMinutes
) {
}
