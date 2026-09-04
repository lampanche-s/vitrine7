package br.com.vitrine7.report.dto;

public record SalesReportPerformanceResponse(
        String name,
        String entryType,
        Long quantity,
        Long revenueCents,
        Long averagePriceCents
) {
}
