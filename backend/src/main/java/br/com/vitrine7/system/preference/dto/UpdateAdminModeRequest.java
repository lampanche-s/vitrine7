package br.com.vitrine7.system.preference.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAdminModeRequest(

        @NotNull(message = "Informe se o modo administrativo está ativo.")
        Boolean enabled
) {
}
