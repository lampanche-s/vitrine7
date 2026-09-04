package br.com.vitrine7.bar.tab.dto;

import jakarta.validation.constraints.Size;

public record VoucherBarTabRequest(
        @Size(max = 120, message = "O veículo deve possuir no máximo 120 caracteres.") String vehicleName,
        @Size(max = 8, message = "A placa é inválida.") String vehiclePlate
) {}
