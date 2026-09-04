package br.com.vitrine7.report.dto;

import java.time.OffsetDateTime;

public record SalesReportLineResponse(
        Long operationId,
        String displayName,
        OffsetDateTime completedAt,
        String itemName,
        String entryType,
        Integer quantity,
        Long unitPriceCents,
        Long totalCents
) {
}
