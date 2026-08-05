package br.com.vitrine7.bar.tab.dto;

import br.com.vitrine7.bar.tab.entity.BarTabLineEntity;

public record BarTabLineResponse(
        Long id,
        Long catalogEntryId,
        String entryType,
        String itemName,
        Long unitPriceCents,
        Integer quantity,
        Long lineTotalCents
) {

    public static BarTabLineResponse from(
            BarTabLineEntity line
    ) {
        return new BarTabLineResponse(
                line.getId(),
                line.getCatalogEntryId(),
                line.getEntryTypeSnapshot().name(),
                line.getItemNameSnapshot(),
                line.getUnitPriceCents(),
                line.getQuantity(),
                line.getLineTotalCents()
        );
    }
}
