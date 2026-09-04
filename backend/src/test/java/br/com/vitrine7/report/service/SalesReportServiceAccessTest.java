package br.com.vitrine7.report.service;

import br.com.vitrine7.report.dto.SalesReportFilters;
import br.com.vitrine7.report.dto.SalesReportScope;
import br.com.vitrine7.report.repository.SalesReportReadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SalesReportServiceAccessTest {

    @Test
    void deniesProtectedRangeBeforeConsultingRepository() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        SalesReportFilters filters = new SalesReportFilters(
                from,
                to,
                Instant.parse("2026-08-01T03:00:00Z"),
                Instant.parse("2026-09-01T03:00:00Z"),
                SalesReportScope.ALL,
                "America/Bahia"
        );
        SalesReportFilterService filterService = mock(SalesReportFilterService.class);
        ReportPeriodAccessService accessService = mock(ReportPeriodAccessService.class);
        SalesReportReadRepository repository = mock(SalesReportReadRepository.class);
        when(filterService.build(from, to, SalesReportScope.ALL, false))
                .thenReturn(filters);
        doThrow(new AccessDeniedException("denied"))
                .when(accessService)
                .requireAccess(from, to, null);
        SalesReportService service = new SalesReportService(
                filterService,
                accessService,
                repository
        );

        assertThrows(
                AccessDeniedException.class,
                () -> service.summary(from, to, SalesReportScope.ALL, null)
        );

        verify(accessService).requireAccess(from, to, null);
        verifyNoInteractions(repository);
    }
}
