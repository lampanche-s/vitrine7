package br.com.vitrine7.lava.client.dto;

import br.com.vitrine7.lava.client.entity.LavaClientEntity;

import java.time.OffsetDateTime;

public record LavaClientResponse(
        Long id,
        String name,
        String phone,
        String vehicleName,
        String plate,
        Integer visitsCount,
        String lastServiceLabel,
        OffsetDateTime lastServiceAt,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static LavaClientResponse from(
            LavaClientEntity client
    ) {
        return new LavaClientResponse(
                client.getId(),
                client.getName(),
                client.getPhoneDigits(),
                client.getVehicleName(),
                client.getPlate(),
                client.getVisitsCount(),
                client.getLastServiceLabel(),
                client.getLastServiceAt(),
                client.isActive(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }
}
