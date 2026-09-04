package br.com.vitrine7.supplier.dto;

import br.com.vitrine7.catalog.entity.CatalogEntryEntity;
import br.com.vitrine7.supplier.entity.SupplierEntity;
import java.time.OffsetDateTime;
import java.util.List;

public record SupplierDetailResponse(Long id, String name, String cnpj, String phone, String cep, OffsetDateTime createdAt, OffsetDateTime updatedAt, List<CatalogItem> catalogEntries) {
    public static SupplierDetailResponse from(SupplierEntity supplier, List<CatalogEntryEntity> entries) {
        return new SupplierDetailResponse(supplier.getId(), supplier.getName(), supplier.getCnpjDigits(), supplier.getPhoneDigits(), supplier.getCepDigits(), supplier.getCreatedAt(), supplier.getUpdatedAt(), entries.stream().map(entry -> new CatalogItem(entry.getId(), entry.getName(), entry.getPriceCents(), entry.isStockEnabled(), entry.getStockQuantity(), entry.getMinimumStockQuantity())).toList());
    }
    public record CatalogItem(Long id, String name, Long priceCents, boolean stockEnabled, Integer stockQuantity, Integer minimumStockQuantity) { }
}
