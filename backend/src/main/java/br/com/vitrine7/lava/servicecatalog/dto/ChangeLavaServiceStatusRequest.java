package br.com.vitrine7.lava.servicecatalog.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeLavaServiceStatusRequest(

        @NotNull(message = "Informe o status do serviço.")
        Boolean active
) {
}
