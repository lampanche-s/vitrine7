package br.com.vitrine7.cashclosing.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record CashClosingResponse(
        LocalDate businessDate,
        String userName,
        boolean closed,
        OffsetDateTime closedAt,
        long grossSalesCents,
        long totalReceivedCents,
        long itemSalesCents,
        long serviceSalesCents,
        long saleCount,
        long averageTicketCents,
        long reversedCents,
        long reversedCount,
        long cashReceivedCents,
        long cashChangeCents,
        long openCommandCount,
        long openCommandAmountCents,
        OffsetDateTime firstSaleAt,
        OffsetDateTime lastSaleAt,
        List<PaymentBreakdown> paymentBreakdown,
        List<Operation> operations
) {
    public record PaymentBreakdown(
            String method,
            long amountCents,
            long saleCount
    ) {
    }

    public record Operation(
            long operationId,
            String displayName,
            OffsetDateTime completedAt,
            String paymentMethod,
            String paymentStatus,
            long amountCents,
            long cashReceivedCents,
            long cashChangeCents,
            List<Line> lines
    ) {
    }

    public record Line(
            String entryType,
            String itemName,
            int quantity,
            long unitPriceCents,
            long lineTotalCents
    ) {
    }
}
