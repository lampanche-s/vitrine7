package br.com.vitrine7.client.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ClientConsumptionHistoryResponse(
        long operationId,
        OffsetDateTime completedAt,
        long totalCents,
        String paymentStatus,
        List<Line> lines
) {
    public record Line(
            String entryType,
            String itemName,
            int quantity,
            long unitPriceCents,
            long lineTotalCents
    ) {
    }
}
