package br.com.vitrine7.supplier.dto;

import br.com.vitrine7.supplier.entity.SupplierEntity;
import java.time.OffsetDateTime;

public record SupplierResponse(Long id, String name, String cnpj, String phone, String cep, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static SupplierResponse from(SupplierEntity supplier) {
        return new SupplierResponse(supplier.getId(), supplier.getName(), supplier.getCnpjDigits(), supplier.getPhoneDigits(), supplier.getCepDigits(), supplier.getCreatedAt(), supplier.getUpdatedAt());
    }
}
