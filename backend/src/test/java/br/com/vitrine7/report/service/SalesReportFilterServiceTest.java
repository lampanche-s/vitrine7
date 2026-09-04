package br.com.vitrine7.report.service;

import br.com.vitrine7.common.exception.InvalidRequestException;
import br.com.vitrine7.common.config.BusinessProperties;
import br.com.vitrine7.report.dto.SalesReportFilters;
import br.com.vitrine7.report.dto.SalesReportScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SalesReportFilterServiceTest {

    private final SalesReportFilterService service =
            new SalesReportFilterService(
                    new BusinessProperties(
                            "America/Bahia",
                            new BusinessProperties.Establishment(
                                    "Vitrine 7",
                                    null,
                                    null,
                                    "Rua Senhor do Bonfim, Monte Gordo, Camaçari/BA"
                            )
                    )
            );

    @Test
    void summaryAllowsOpenPeriod() {
        SalesReportFilters filters = service.build(
                null,
                null,
                SalesReportScope.ALL,
                false
        );

        assertNull(filters.from());
        assertNull(filters.to());
        assertNull(filters.fromInstant());
        assertNull(filters.toExclusiveInstant());
        assertEquals("America/Bahia", filters.timeZone());
    }

    @Test
    void exportConvertsInclusiveDatesToExclusiveInstantRange() {
        SalesReportFilters filters = service.build(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 5),
                SalesReportScope.SERVICE,
                true
        );

        assertEquals(
                Instant.parse("2026-08-01T03:00:00Z"),
                filters.fromInstant()
        );
        assertEquals(
                Instant.parse("2026-08-06T03:00:00Z"),
                filters.toExclusiveInstant()
        );
        assertEquals(SalesReportScope.SERVICE, filters.scope());
    }

    @Test
    void rejectsMissingInvalidOrExcessivePeriod() {
        InvalidRequestException missing = assertThrows(
                InvalidRequestException.class,
                () -> service.build(
                        null,
                        null,
                        SalesReportScope.ITEM,
                        true
                )
        );
        assertEquals("REPORT_PERIOD_REQUIRED", missing.getCode());

        InvalidRequestException inverted = assertThrows(
                InvalidRequestException.class,
                () -> service.build(
                        LocalDate.of(2026, 8, 5),
                        LocalDate.of(2026, 8, 1),
                        SalesReportScope.ITEM,
                        true
                )
        );
        assertEquals("INVALID_REPORT_DATE_RANGE", inverted.getCode());

        InvalidRequestException tooLarge = assertThrows(
                InvalidRequestException.class,
                () -> service.build(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2026, 1, 2),
                        SalesReportScope.ITEM,
                        true
                )
        );
        assertEquals("REPORT_DATE_RANGE_TOO_LARGE", tooLarge.getCode());
    }
}
