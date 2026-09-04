package br.com.vitrine7.report.dto;

public record SalesReportCatalogEntryResponse(
        String name,
        String entryType,
        Long quantity,
        Long grossCents
) {
}
