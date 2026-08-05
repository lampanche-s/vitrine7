package br.com.vitrine7.receipt.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReceiptPaymentResponse(
        UUID paymentId,
        String method,
        String processingMode,
        String status,
        long approvedAmountCents,
        OffsetDateTime approvedAt,
        Long cashReceivedCents,
        Long cashChangeCents,
        String terminalProvider,
        OffsetDateTime reversedAt,
        String reversalReason
) {
}
