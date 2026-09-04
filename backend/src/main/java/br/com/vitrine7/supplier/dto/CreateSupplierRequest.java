package br.com.vitrine7.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupplierRequest(
        @NotBlank(message = "Informe o nome do fornecedor.") @Size(max = 120, message = "O nome deve possuir no máximo 120 caracteres.") String name,
        @Size(max = 30, message = "O CNPJ informado é muito longo.") String cnpj,
        @Size(max = 20, message = "O telefone informado é muito longo.") String phone,
        @Size(max = 12, message = "O CEP informado é muito longo.") String cep
) { }
