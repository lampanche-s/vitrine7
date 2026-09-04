package br.com.vitrine7.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
        @NotBlank(message = "Informe o nome do funcionário.")
        @Size(max = 80, message = "O nome deve possuir no máximo 80 caracteres.")
        String name
) {}
