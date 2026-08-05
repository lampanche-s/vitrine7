package br.com.vitrine7.catalog.dto;

import br.com.vitrine7.catalog.entity.CatalogEntryEntity;

import java.time.OffsetDateTime;

public record CatalogEntryResponse(
        Long id,
        String name,
        String type,
        Long priceCents,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static CatalogEntryResponse from(
            CatalogEntryEntity entry
    ) {
        return new CatalogEntryResponse(
                entry.getId(),
                entry.getName(),
                entry.getEntryType().name(),
                entry.getPriceCents(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
