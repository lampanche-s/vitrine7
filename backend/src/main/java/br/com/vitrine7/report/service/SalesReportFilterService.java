package br.com.vitrine7.report.service;

import br.com.vitrine7.common.exception.InvalidRequestException;
import br.com.vitrine7.common.config.BusinessProperties;
import br.com.vitrine7.report.dto.SalesReportFilters;
import br.com.vitrine7.report.dto.SalesReportScope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class SalesReportFilterService {

    private static final long MAX_DAYS = 366;

    private final BusinessProperties properties;

    public SalesReportFilterService(BusinessProperties properties) {
        this.properties = properties;
    }

    public SalesReportFilters build(
            LocalDate from,
            LocalDate to,
            SalesReportScope scope,
            boolean requirePeriod
    ) {
        ZoneId zone = zoneId();

        if (requirePeriod && (from == null || to == null)) {
            throw new InvalidRequestException(
                    "REPORT_PERIOD_REQUIRED",
                    "Informe as datas inicial e final do relatório."
            );
        }

        LocalDate normalizedFrom = from;
        LocalDate normalizedTo = to;

        if (normalizedFrom != null || normalizedTo != null) {
            if (normalizedFrom == null || normalizedTo == null) {
                throw new InvalidRequestException(
                        "INVALID_REPORT_DATE_RANGE",
                        "Informe as duas datas do período."
                );
            }
            validateRange(normalizedFrom, normalizedTo);
        }

        return new SalesReportFilters(
                normalizedFrom,
                normalizedTo,
                normalizedFrom == null
                        ? null
                        : normalizedFrom.atStartOfDay(zone).toInstant(),
                normalizedTo == null
                        ? null
                        : normalizedTo.plusDays(1).atStartOfDay(zone).toInstant(),
                scope == null ? SalesReportScope.ALL : scope,
                zone.getId()
        );
    }

    public ZoneId zoneId() {
        String configured = properties.businessTimeZone();
        if (configured == null || configured.isBlank()) {
            return ZoneId.of("America/Bahia");
        }
        return ZoneId.of(configured);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new InvalidRequestException(
                    "INVALID_REPORT_DATE_RANGE",
                    "A data inicial deve ser menor ou igual à data final."
            );
        }

        if (from.plusDays(MAX_DAYS - 1).isBefore(to)) {
            throw new InvalidRequestException(
                    "REPORT_DATE_RANGE_TOO_LARGE",
                    "O período do relatório deve ter no máximo 366 dias."
            );
        }
    }
}
