package br.com.vitrine7.lava.workorder.dto;

import br.com.vitrine7.lava.workorder.entity.LavaVehicleSize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLavaWorkOrderRequest(
        Long clientId,

        @Size(max = 120)
        String customerName,

        @Size(max = 30)
        String phone,

        @Size(max = 120)
        String vehicleName,

        @Size(max = 12)
        String plate,

        @NotNull(message = "Informe o servico.")
        Long serviceId,

        @NotNull(message = "Informe o porte do veiculo.")
        LavaVehicleSize vehicleSize
) {
}
