package br.com.vitrine7.history.bar.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BarHistoryResponse(
        Long operationId,
        UUID checkoutId,
        String displayName,
        String operationalStatus,
        String checkoutStatus,
        Long subtotalCents,
        Long discountCents,
        Long totalCents,
        String documentType,
        HistoryPaymentMethod paymentMethod,
        String paymentStatus,
        UUID paymentId,
        OffsetDateTime paymentReversedAt,
        String paymentReversalReason,
        Long cashReceivedCents,
        Long cashChangeCents,
        Integer lineCount,
        Integer totalUnits,
        OffsetDateTime createdAt,
        OffsetDateTime finishedAt,
        OffsetDateTime reopenUntil,
        Long createdByUserId,
        String detailPath
) {
}
