package br.com.vitrine7.payment.core.dto;

import br.com.vitrine7.payment.core.entity.PaymentEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID checkoutSessionId,
        UUID idempotencyKey,
        String method,
        String processingMode,
        String status,
        Long amountCents,
        Long cashReceivedCents,
        Long cashChangeCents,
        Long cashConfirmedByUserId,
        String manualReason,
        OffsetDateTime approvedAt,
        Long approvedByUserId,
        OffsetDateTime declinedAt,
        OffsetDateTime cancelledAt,
        Long cancelledByUserId,
        String cancelReason,
        OffsetDateTime reversedAt,
        Long reversedByUserId,
        String reversalReason,
        Long createdByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static PaymentResponse from(
            PaymentEntity payment
    ) {
        return new PaymentResponse(
                payment.getId(),
                payment.getCheckoutSessionId(),
                payment.getIdempotencyKey(),
                payment.getMethod().name(),
                payment.getProcessingMode().name(),
                payment.getStatus().name(),
                payment.getAmountCents(),
                payment.getCashReceivedCents(),
                payment.getCashChangeCents(),
                payment.getCashConfirmedByUserId(),
                payment.getManualReason(),
                payment.getApprovedAt(),
                payment.getApprovedByUserId(),
                payment.getDeclinedAt(),
                payment.getCancelledAt(),
                payment.getCancelledByUserId(),
                payment.getCancelReason(),
                payment.getReversedAt(),
                payment.getReversedByUserId(),
                payment.getReversalReason(),
                payment.getCreatedByUserId(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
