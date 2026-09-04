package br.com.vitrine7.report.repository;

import br.com.vitrine7.report.dto.SalesReportFilters;
import br.com.vitrine7.report.dto.SalesReportScope;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesReportReadRepositoryTest {

    @Test
    void convertsInstantFiltersToJdbcSupportedTimestamps()
            throws Exception {
        SalesReportReadRepository repository =
                new SalesReportReadRepository(null);
        Method method = SalesReportReadRepository.class
                .getDeclaredMethod(
                        "params",
                        SalesReportFilters.class
                );
        method.setAccessible(true);

        MapSqlParameterSource params =
                (MapSqlParameterSource) method.invoke(
                        repository,
                        new SalesReportFilters(
                                null,
                                null,
                                Instant.parse("2026-08-05T03:00:00Z"),
                                Instant.parse("2026-08-06T03:00:00Z"),
                                SalesReportScope.ALL,
                                "America/Bahia"
                        )
                );

        assertTrue(params.getValue("fromInstant") instanceof OffsetDateTime);
        assertTrue(params.getValue("toInstant") instanceof OffsetDateTime);
    }

    @Test
    void reportsIgnoreReversedPayments()
            throws Exception {
        Field field =
                SalesReportReadRepository.class
                        .getDeclaredField(
                                "OPERATION_CTE"
                        );

        field.setAccessible(true);

        String sql =
                (String) field.get(null);

        assertFalse(
                sql.contains(
                        "'REVERSED'"
                )
        );

        assertTrue(
                sql.contains(
                        "WHERE payment.status = 'APPROVED'"
                )
        );

        assertFalse(
                sql.contains(
                        "'REVERSAL_" + "PENDING'"
                )
        );
    }

    @Test
    void reportsIncludeOnlyClosedFinalizedApprovedOperations()
            throws Exception {
        Field field = SalesReportReadRepository.class
                .getDeclaredField("OPERATION_CTE");

        field.setAccessible(true);

        String sql = (String) field.get(null);

        assertTrue(sql.contains("WHERE tab.status = 'CLOSED'"));
        assertTrue(sql.contains("checkout.status = 'FINALIZED'"));
        assertTrue(sql.contains("WHERE payment.status = 'APPROVED'"));
    }
}
