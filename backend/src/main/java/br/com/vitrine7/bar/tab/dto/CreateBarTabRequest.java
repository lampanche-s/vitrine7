package br.com.vitrine7.bar.tab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateBarTabRequest(

        @NotBlank(message = "Informe o nome da comanda.")
        @Size(
                max = 80,
                message = "O nome deve possuir no maximo 80 caracteres."
        )
        String name,

        @Positive(message = "O cliente informado é inválido.")
        Long clientId,

        @Positive(message = "O funcionário informado é inválido.")
        Long employeeId
) {
    public CreateBarTabRequest(String name, Long clientId) {
        this(name, clientId, null);
    }
}
