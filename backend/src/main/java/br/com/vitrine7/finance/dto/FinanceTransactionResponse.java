package br.com.vitrine7.finance.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FinanceTransactionResponse(
        UUID paymentId,
        UUID checkoutId,
        String operationType,
        FinanceModule module,
        FinancePaymentMethod method,
        String processingMode,
        String status,
        long amountCents,
        OffsetDateTime approvedAt,
        Long responsibleUserId,
        String responsibleUserName,
        Long operationId,
        String displayName,
        String description,
        String detailPath
) {
}
