package br.com.vitrine7.lava.client.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeLavaClientStatusRequest(

        @NotNull(message = "Informe o status do cliente.")
        Boolean active
) {
}
