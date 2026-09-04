package br.com.vitrine7.catalog.dto;

import br.com.vitrine7.catalog.entity.CatalogEntryEntity;

import java.time.OffsetDateTime;

public record CatalogEntryResponse(
        Long id,
        String name,
        String type,
        Long priceCents,
        boolean stockEnabled,
        Integer stockQuantity,
        Integer minimumStockQuantity,
        Long supplierId,
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
                entry.isStockEnabled(),
                entry.getStockQuantity(),
                entry.getMinimumStockQuantity(),
                entry.getSupplier() == null ? null : entry.getSupplier().getId(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
