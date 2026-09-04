package br.com.vitrine7.report.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportPeriodAccessServiceTest {

    private static final ZoneId BAHIA = ZoneId.of("America/Bahia");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-01T15:00:00Z"),
            BAHIA
    );

    private ReportPeriodAccessService service(String configuredPassword) {
        SalesReportFilterService filterService = mock(SalesReportFilterService.class);
        when(filterService.zoneId()).thenReturn(BAHIA);
        return new ReportPeriodAccessService(configuredPassword, filterService, CLOCK);
    }

    @Test
    void allowsTodayAndYesterdayWithoutPassword() {
        ReportPeriodAccessService service = service("segredo");

        assertDoesNotThrow(() -> service.requireAccess(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1),
                null
        ));
        assertDoesNotThrow(() -> service.requireAccess(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 8, 31),
                null
        ));
    }

    @Test
    void protectsAnyOtherRealDateRange() {
        ReportPeriodAccessService service = service("segredo");

        assertTrue(service.requiresPassword(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 1)
        ));
        assertTrue(service.requiresPassword(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        ));
        assertTrue(service.requiresPassword(null, null));
        assertFalse(service.requiresPassword(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1)
        ));
    }

    @Test
    void rejectsMissingOrWrongPasswordAndAllowsCorrectPassword() {
        ReportPeriodAccessService service = service("segredo");
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        assertThrows(AccessDeniedException.class,
                () -> service.requireAccess(from, to, null));
        assertThrows(AccessDeniedException.class,
                () -> service.requireAccess(from, to, "incorreta"));
        assertDoesNotThrow(() -> service.requireAccess(from, to, "segredo"));
    }

    @Test
    void rejectsProtectedPeriodsWhenConfigurationIsAbsent() {
        ReportPeriodAccessService service = service("");

        assertThrows(AccessDeniedException.class, () -> service.requireAccess(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                "qualquer"
        ));
    }
}
