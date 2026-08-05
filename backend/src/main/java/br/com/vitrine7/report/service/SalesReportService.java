package br.com.vitrine7.report.service;

import br.com.vitrine7.report.dto.SalesReportFilters;
import br.com.vitrine7.report.dto.SalesReportPeriodResponse;
import br.com.vitrine7.report.dto.SalesReportResponse;
import br.com.vitrine7.report.dto.SalesReportScope;
import br.com.vitrine7.report.repository.SalesReportReadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class SalesReportService {

    private static final int SUMMARY_TOP_LIMIT = 20;
    private static final int SUMMARY_RECENT_LIMIT = 10;

    private final SalesReportFilterService filterService;
    private final SalesReportReadRepository repository;

    public SalesReportService(
            SalesReportFilterService filterService,
            SalesReportReadRepository repository
    ) {
        this.filterService = filterService;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SalesReportResponse summary(
            LocalDate from,
            LocalDate to,
            SalesReportScope scope
    ) {
        SalesReportFilters filters = filterService.build(
                from,
                to,
                scope,
                false
        );

        return buildResponse(filters, false);
    }

    @Transactional(readOnly = true)
    public SalesReportResponse export(
            LocalDate from,
            LocalDate to,
            SalesReportScope scope
    ) {
        SalesReportFilters filters = filterService.build(
                from,
                to,
                scope,
                true
        );

        return buildResponse(filters, true);
    }

    private SalesReportResponse buildResponse(
            SalesReportFilters filters,
            boolean includeDetails
    ) {
        SalesReportReadRepository.Totals totals = repository.totals(filters);
        long averageTicket = totals.operationCount() == 0
                ? 0L
                : totals.totalReceivedCents() / totals.operationCount();

        return new SalesReportResponse(
                filters.scope(),
                new SalesReportPeriodResponse(
                        filters.from(),
                        filters.to(),
                        filters.timeZone()
                ),
                totals.totalReceivedCents(),
                totals.operationCount(),
                averageTicket,
                totals.totalUnits(),
                repository.paymentBreakdown(filters),
                repository.topEntries(filters, SUMMARY_TOP_LIMIT),
                repository.operations(filters, SUMMARY_RECENT_LIMIT),
                includeDetails
                        ? repository.operations(filters, null)
                        : List.of(),
                includeDetails
                        ? repository.lines(filters)
                        : List.of()
        );
    }
}
