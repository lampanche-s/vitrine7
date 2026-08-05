package br.com.vitrine7.client.dto;

import br.com.vitrine7.client.entity.ClientEntity;

import java.time.OffsetDateTime;

public record ClientResponse(
        Long id,
        String name,
        String phone,
        String vehicleName,
        String plate,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ClientResponse from(ClientEntity client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getPhoneDigits(),
                client.getVehicleName(),
                client.getPlate(),
                client.isActive(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }
}
