package br.com.vitrine7.receipt.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ReceiptResponse(
        String receiptType,
        String title,
        String nonFiscalNotice,
        ReceiptEstablishmentResponse establishment,
        ReceiptOperationResponse operation,
        List<ReceiptLineResponse> lines,
        long subtotalCents,
        long discountCents,
        long totalCents,
        ReceiptPaymentResponse payment,
        OffsetDateTime issuedAt
) {
}
