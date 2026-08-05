package br.com.vitrine7.lava.servicecatalog.dto;

import br.com.vitrine7.lava.servicecatalog.entity.LavaServiceEntity;

import java.time.OffsetDateTime;

public record LavaServiceResponse(
        Long id,
        String name,
        String category,
        Long smallVehiclePriceCents,
        Long mediumVehiclePriceCents,
        Integer durationMinutes,
        String durationLabel,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static LavaServiceResponse from(
            LavaServiceEntity service
    ) {
        return new LavaServiceResponse(
                service.getId(),
                service.getName(),
                service.getCategory(),
                service.getSmallVehiclePriceCents(),
                service.getMediumVehiclePriceCents(),
                service.getDurationMinutes(),
                formatDuration(service.getDurationMinutes()),
                service.isActive(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }

    private static String formatDuration(
            int durationMinutes
    ) {
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;

        if (hours == 0) {
            return minutes + " min";
        }

        if (minutes == 0) {
            return hours + " h";
        }

        return hours + " h " + minutes + " min";
    }
}
