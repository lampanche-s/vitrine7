package br.com.vitrine7.lava.workorder.dto;

import br.com.vitrine7.lava.workorder.entity.LavaWorkOrderLineEntity;

import java.time.OffsetDateTime;

public record LavaWorkOrderLineResponse(
        Long id,
        Long serviceId,
        String serviceNameSnapshot,
        Long priceCents,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static LavaWorkOrderLineResponse from(
            LavaWorkOrderLineEntity line
    ) {
        return new LavaWorkOrderLineResponse(
                line.getId(),
                line.getServiceId(),
                line.getServiceNameSnapshot(),
                line.getPriceCents(),
                line.getCreatedAt(),
                line.getUpdatedAt()
        );
    }
}
