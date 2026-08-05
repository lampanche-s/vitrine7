package br.com.vitrine7.finance.dto;

public record FinanceBreakdownResponse(
        String key,
        long revenueCents,
        long paymentCount,
        long revenueBasisPoints
) {
}
