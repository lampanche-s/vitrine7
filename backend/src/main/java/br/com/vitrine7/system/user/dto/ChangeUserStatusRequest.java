package br.com.vitrine7.system.user.dto;

import br.com.vitrine7.system.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(
        @NotNull(message = "Informe o novo status.")
        UserStatus status
) {
}
