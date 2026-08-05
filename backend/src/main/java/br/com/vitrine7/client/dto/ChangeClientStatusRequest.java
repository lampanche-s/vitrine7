package br.com.vitrine7.client.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeClientStatusRequest(
        @NotNull(message = "Informe o status do cliente.")
        Boolean active
) {
}
