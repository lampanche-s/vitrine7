package br.com.vitrine7.report.dto;

public record SalesReportPaymentBreakdownResponse(
        String method,
        Long amountCents,
        Long paymentCount,
        Double participationPercentage
) {
}
