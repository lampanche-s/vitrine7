package br.com.vitrine7.system.user.dto;

import br.com.vitrine7.common.security.PasswordPolicy;
import br.com.vitrine7.system.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Informe o nome.")
        @Size(max = 120, message = "O nome deve possuir no máximo 120 caracteres.")
        String name,

        @NotBlank(message = "Informe um nome de usuário.")
        @Size(max = 255, message = "O nome de usuário deve possuir no máximo 255 caracteres.")
        String username,

        @NotNull(message = PasswordPolicy.MESSAGE)
        @Size(min = PasswordPolicy.MIN_LENGTH, message = PasswordPolicy.MESSAGE)
        String password,

        @NotNull(message = "Informe o perfil do usuário.")
        UserRole role
) {
}
