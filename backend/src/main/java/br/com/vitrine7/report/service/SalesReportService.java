package br.com.vitrine7.report.service;

import br.com.vitrine7.report.dto.SalesReportFilters;
import br.com.vitrine7.report.dto.SalesReportPeriodResponse;
import br.com.vitrine7.report.dto.SalesReportResponse;
import br.com.vitrine7.report.dto.SalesReportScope;
import br.com.vitrine7.report.dto.SalesReportTypeSummaryResponse;
import br.com.vitrine7.report.repository.SalesReportReadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

@Service
public class SalesReportService {

    private static final int SUMMARY_TOP_LIMIT = 20;
    private static final int SUMMARY_RECENT_LIMIT = 10;

    private final SalesReportFilterService filterService;
    private final ReportPeriodAccessService accessService;
    private final SalesReportReadRepository repository;

    public SalesReportService(
            SalesReportFilterService filterService,
            ReportPeriodAccessService accessService,
            SalesReportReadRepository repository
    ) {
        this.filterService = filterService;
        this.accessService = accessService;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SalesReportResponse summary(
            LocalDate from,
            LocalDate to,
            SalesReportScope scope,
            String reportPassword
    ) {
        SalesReportFilters filters = filterService.build(
                from,
                to,
                scope,
                false
        );

        accessService.requireAccess(filters.from(), filters.to(), reportPassword);

        return buildResponse(filters, false);
    }

    @Transactional(readOnly = true)
    public SalesReportResponse export(
            LocalDate from,
            LocalDate to,
            SalesReportScope scope,
            String reportPassword
    ) {
        SalesReportFilters filters = filterService.build(
                from,
                to,
                scope,
                true
        );

        accessService.requireAccess(filters.from(), filters.to(), reportPassword);

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
        long itemRevenue = filters.scope() == SalesReportScope.SERVICE
                ? 0L
                : totals.itemRevenueCents();
        long serviceRevenue = filters.scope() == SalesReportScope.ITEM
                ? 0L
                : totals.serviceRevenueCents();
        long itemUnits = filters.scope() == SalesReportScope.SERVICE
                ? 0L
                : totals.itemUnits();
        long serviceUnits = filters.scope() == SalesReportScope.ITEM
                ? 0L
                : totals.serviceUnits();
        List<SalesReportTypeSummaryResponse> distribution = new ArrayList<>();

        if (filters.scope() != SalesReportScope.SERVICE) {
            distribution.add(typeSummary(
                    "ITEM",
                    itemRevenue,
                    itemUnits,
                    totals.totalReceivedCents()
            ));
        }
        if (filters.scope() != SalesReportScope.ITEM) {
            distribution.add(typeSummary(
                    "SERVICE",
                    serviceRevenue,
                    serviceUnits,
                    totals.totalReceivedCents()
            ));
        }

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
                itemRevenue,
                serviceRevenue,
                itemUnits,
                serviceUnits,
                distribution,
                repository.paymentBreakdown(filters),
                repository.dailyEvolution(filters),
                filters.scope() == SalesReportScope.ITEM
                        ? List.of()
                        : repository.performance(filters, "SERVICE"),
                filters.scope() == SalesReportScope.SERVICE
                        ? List.of()
                        : repository.performance(filters, "ITEM"),
                repository.topEntries(filters, SUMMARY_TOP_LIMIT),
                repository.operations(filters, SUMMARY_RECENT_LIMIT),
                repository.operations(filters, null),
                includeDetails
                        ? repository.lines(filters)
                        : List.of()
        );
    }

    private SalesReportTypeSummaryResponse typeSummary(
            String entryType,
            long revenueCents,
            long quantity,
            long totalReceivedCents
    ) {
        double percentage = totalReceivedCents == 0
                ? 0D
                : Math.round(revenueCents * 10_000D / totalReceivedCents) / 100D;
        return new SalesReportTypeSummaryResponse(
                entryType,
                revenueCents,
                quantity,
                percentage
        );
    }
}
