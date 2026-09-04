package br.com.vitrine7.report.dto;

import java.time.LocalDate;

public record SalesReportDailyResponse(
        LocalDate date,
        Long operationCount,
        Long receivedCents,
        Long averageTicketCents
) {
}
