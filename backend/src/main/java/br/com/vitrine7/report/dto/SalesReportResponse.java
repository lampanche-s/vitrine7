package br.com.vitrine7.report.dto;

import java.util.List;

public record SalesReportResponse(
        SalesReportScope scope,
        SalesReportPeriodResponse period,
        Long totalReceivedCents,
        Long operationCount,
        Long averageTicketCents,
        Long totalUnits,
        List<SalesReportPaymentBreakdownResponse> byPaymentMethod,
        List<SalesReportCatalogEntryResponse> topEntries,
        List<SalesReportOperationResponse> latestOperations,
        List<SalesReportOperationResponse> operations,
        List<SalesReportLineResponse> lines
) {
}
