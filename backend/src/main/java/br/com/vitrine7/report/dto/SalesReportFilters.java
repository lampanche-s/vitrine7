package br.com.vitrine7.report.dto;

import java.time.Instant;
import java.time.LocalDate;

public record SalesReportFilters(
        LocalDate from,
        LocalDate to,
        Instant fromInstant,
        Instant toExclusiveInstant,
        SalesReportScope scope,
        String timeZone
) {
}
