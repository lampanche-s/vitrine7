package br.com.vitrine7.lava.workorder.dto;

import br.com.vitrine7.lava.workorder.entity.LavaVehicleSize;
import jakarta.validation.constraints.NotNull;

public record UpdateLavaWorkOrderVehicleSizeRequest(
        @NotNull
        LavaVehicleSize vehicleSize
) {
}
