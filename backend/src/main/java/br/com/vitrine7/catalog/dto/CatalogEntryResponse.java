package br.com.vitrine7.catalog.dto;

import br.com.vitrine7.catalog.entity.CatalogEntryEntity;

import java.time.OffsetDateTime;

public record CatalogEntryResponse(
        Long id,
        String name,
        String type,
        Long priceCents,
        Integer stockQuantity,
        Integer minimumStockQuantity,
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
                entry.getStockQuantity(),
                entry.getMinimumStockQuantity(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
