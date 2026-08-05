package br.com.vitrine7.checkout.dto;

import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CheckoutSessionResponse(
        UUID id,
        UUID idempotencyKey,
        String businessArea,
        String operationType,
        Long sourceId,
        String status,
        Long subtotalCents,
        Long discountCents,
        Long totalCents,
        String documentType,
        String cpfDigits,
        OffsetDateTime expiresAt,
        OffsetDateTime paidAt,
        OffsetDateTime finalizedAt,
        OffsetDateTime cancelledAt,
        Long cancelledByUserId,
        String cancelReason,
        Long createdByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static CheckoutSessionResponse from(
            CheckoutSessionEntity checkout
    ) {
        return new CheckoutSessionResponse(
                checkout.getId(),
                checkout.getIdempotencyKey(),
                checkout.getBusinessArea().name(),
                checkout.getOperationType().name(),
                checkout.getSourceId(),
                checkout.getStatus().name(),
                checkout.getSubtotalCents(),
                checkout.getDiscountCents(),
                checkout.getTotalCents(),
                checkout.getDocumentType() == null
                        ? null
                        : checkout.getDocumentType().name(),
                checkout.getCpfDigits(),
                checkout.getExpiresAt(),
                checkout.getPaidAt(),
                checkout.getFinalizedAt(),
                checkout.getCancelledAt(),
                checkout.getCancelledByUserId(),
                checkout.getCancelReason(),
                checkout.getCreatedByUserId(),
                checkout.getCreatedAt(),
                checkout.getUpdatedAt()
        );
    }
}
