package br.com.vitrine7.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(
        @NotBlank(message = "Informe o nome do cliente.")
        @Size(max = 120, message = "O nome deve possuir no máximo 120 caracteres.")
        String name,

        @Size(max = 20, message = "O telefone deve possuir no máximo 20 caracteres.")
        String phone,

        @NotBlank(message = "Informe o veículo.")
        @Size(max = 120, message = "O veículo deve possuir no máximo 120 caracteres.")
        String vehicleName,

        @NotBlank(message = "Informe a placa.")
        @Size(max = 10, message = "A placa informada é muito longa.")
        String plate
) {
}
