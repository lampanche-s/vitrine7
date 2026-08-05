package br.com.vitrine7.finance.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record FinanceSummaryResponse(
        FinancePeriodResponse period,
        long totalRevenueCents,
        long paymentCount,
        long averageTicketCents,
        OffsetDateTime firstApprovedAt,
        OffsetDateTime lastApprovedAt,
        List<FinanceBreakdownResponse> byModule,
        List<FinanceBreakdownResponse> byOperationType,
        List<FinanceBreakdownResponse> byPaymentMethod
) {
}
