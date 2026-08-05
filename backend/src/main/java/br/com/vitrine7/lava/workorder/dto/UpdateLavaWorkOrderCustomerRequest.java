package br.com.vitrine7.lava.workorder.dto;

import jakarta.validation.constraints.Size;

public record UpdateLavaWorkOrderCustomerRequest(
        Long clientId,

        @Size(max = 120)
        String customerName,

        @Size(max = 30)
        String phone,

        @Size(max = 120)
        String vehicleName,

        @Size(max = 12)
        String plate
) {
}
