package br.com.vitrine7.system.user.dto;

import br.com.vitrine7.common.security.PasswordPolicy;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResetUserPasswordRequest(
        @NotNull(message = PasswordPolicy.MESSAGE)
        @Size(min = PasswordPolicy.MIN_LENGTH, message = PasswordPolicy.MESSAGE)
        String newPassword
) {
}
