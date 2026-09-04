package br.com.vitrine7.report.dto;

import java.util.List;

public record SalesReportResponse(
        SalesReportScope scope,
        SalesReportPeriodResponse period,
        Long totalReceivedCents,
        Long operationCount,
        Long averageTicketCents,
        Long totalUnits,
        Long itemRevenueCents,
        Long serviceRevenueCents,
        Long itemUnits,
        Long serviceUnits,
        List<SalesReportTypeSummaryResponse> distribution,
        List<SalesReportPaymentBreakdownResponse> byPaymentMethod,
        List<SalesReportDailyResponse> dailyEvolution,
        List<SalesReportPerformanceResponse> servicePerformance,
        List<SalesReportPerformanceResponse> productPerformance,
        List<SalesReportCatalogEntryResponse> topEntries,
        List<SalesReportOperationResponse> latestOperations,
        List<SalesReportOperationResponse> operations,
        List<SalesReportLineResponse> lines
) {
}
