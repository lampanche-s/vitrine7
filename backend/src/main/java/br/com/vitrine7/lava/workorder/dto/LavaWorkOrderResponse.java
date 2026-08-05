package br.com.vitrine7.lava.workorder.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LavaWorkOrderResponse(
        Long id,
        Long registeredClientId,
        String customerNameSnapshot,
        String customerPhoneDigitsSnapshot,
        String vehicleNameSnapshot,
        String vehiclePlateSnapshot,
        String vehicleSize,
        String status,
        UUID checkoutSessionId,
        String checkoutStatus,
        Long checkoutSourceId,
        String checkoutOperationType,
        Long subtotalCents,
        Long discountCents,
        Long totalCents,
        String documentType,
        String cpfDigits,
        boolean prepared,
        OffsetDateTime preparedAt,
        OffsetDateTime paidAt,
        Long paidByUserId,
        OffsetDateTime completedAt,
        Long completedByUserId,
        OffsetDateTime cancelledAt,
        Long cancelledByUserId,
        String cancellationReason,
        List<LavaWorkOrderLineResponse> services,
        Long createdByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
