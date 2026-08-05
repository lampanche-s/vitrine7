package br.com.vitrine7.system.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSystemSettingsRequest(

        @NotBlank(message = "Informe o nome do estabelecimento.")
        @Size(max = 120, message = "O nome deve possuir no máximo 120 caracteres.")
        String companyName,

        @Size(max = 18, message = "O CNPJ deve possuir no máximo 18 caracteres.")
        String cnpj,

        @Size(max = 20, message = "O telefone deve possuir no máximo 20 caracteres.")
        String phone,

        @Size(max = 255, message = "O endereço deve possuir no máximo 255 caracteres.")
        String address
) {
}
