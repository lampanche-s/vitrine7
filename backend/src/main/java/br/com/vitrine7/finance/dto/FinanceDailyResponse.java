package br.com.vitrine7.finance.dto;

import java.time.LocalDate;
import java.util.List;

public record FinanceDailyResponse(
        LocalDate date,
        long totalRevenueCents,
        long paymentCount,
        long averageTicketCents,
        List<FinanceBreakdownResponse> byPaymentMethod,
        List<FinanceBreakdownResponse> byModule
) {
}
