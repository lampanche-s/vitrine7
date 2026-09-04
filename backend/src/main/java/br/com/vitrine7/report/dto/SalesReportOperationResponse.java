package br.com.vitrine7.report.dto;

import java.time.OffsetDateTime;

public record SalesReportOperationResponse(
        Long operationId,
        String displayName,
        OffsetDateTime completedAt,
        String paymentMethod,
        String responsibleUserName,
        Long grossCents,
        Long discountCents,
        Long netCents,
        Integer lineCount,
        Integer totalUnits,
        Integer itemUnits,
        Integer serviceUnits
) {
}
