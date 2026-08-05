package br.com.vitrine7.history.lava.dto;

import br.com.vitrine7.history.bar.dto.HistoryPaymentMethod;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LavaHistoryResponse(
        Long workOrderId,
        UUID checkoutId,
        String customerSnapshot,
        String phoneSnapshot,
        String vehicleSnapshot,
        String plateSnapshot,
        String vehicleSize,
        String status,
        Long subtotalCents,
        Long discountCents,
        Long totalCents,
        HistoryPaymentMethod paymentMethod,
        String paymentStatus,
        UUID paymentId,
        OffsetDateTime paymentReversedAt,
        String paymentReversalReason,
        Integer serviceCount,
        OffsetDateTime createdAt,
        OffsetDateTime paidAt,
        OffsetDateTime completedAt,
        OffsetDateTime cancelledAt,
        Long createdByUserId,
        String detailPath
) {
}
