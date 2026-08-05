package br.com.vitrine7.lava.workorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelLavaWorkOrderRequest(
        @NotBlank
        @Size(min = 3, max = 255)
        String reason
) {
}
