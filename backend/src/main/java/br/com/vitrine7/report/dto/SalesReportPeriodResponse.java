package br.com.vitrine7.report.dto;

import java.time.LocalDate;

public record SalesReportPeriodResponse(
        LocalDate from,
        LocalDate to,
        String timeZone
) {
}
