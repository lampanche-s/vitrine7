package br.com.vitrine7.report.controller;

import br.com.vitrine7.report.dto.SalesReportResponse;
import br.com.vitrine7.report.dto.SalesReportScope;
import br.com.vitrine7.report.service.SalesReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports/sales")
@PreAuthorize("hasAuthority('reports:access')")
public class SalesReportController {

    private final SalesReportService reportService;

    public SalesReportController(SalesReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public SalesReportResponse summary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestParam(defaultValue = "ALL")
            SalesReportScope scope,

            @RequestHeader(value = "X-Report-Password", required = false)
            String reportPassword
    ) {
        return reportService.summary(from, to, scope, reportPassword);
    }

    @GetMapping("/export")
    public SalesReportResponse export(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestParam
            SalesReportScope scope,

            @RequestHeader(value = "X-Report-Password", required = false)
            String reportPassword
    ) {
        return reportService.export(from, to, scope, reportPassword);
    }
}
