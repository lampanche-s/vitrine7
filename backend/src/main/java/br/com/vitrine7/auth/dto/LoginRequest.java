package br.com.vitrine7.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Informe um nome de usuário.")
        @Size(max = 255, message = "O nome de usuário é muito longo.")
        String username,

        @NotBlank(message = "Informe a senha.")
        String password
) {
}
