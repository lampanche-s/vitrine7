package br.com.vitrine7.report.dto;

public record SalesReportTypeSummaryResponse(
        String entryType,
        Long revenueCents,
        Long quantity,
        Double participationPercentage
) {
}
