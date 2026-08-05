package br.com.vitrine7.finance.dto;

import java.time.LocalDate;

public record FinancePeriodResponse(
        LocalDate from,
        LocalDate to,
        String timeZone
) {
}
