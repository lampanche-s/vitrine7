package br.com.vitrine7.employee.voucher.controller;

import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.employee.voucher.dto.*;
import br.com.vitrine7.employee.voucher.service.EmployeeVoucherQueryService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/employees/vouchers")
@PreAuthorize("hasAuthority('reports:access')")
@RequiredArgsConstructor
@Validated
public class EmployeeVoucherController {
    private final EmployeeVoucherQueryService service;

    @GetMapping
    public PageResponse<EmployeeVoucherResponse> list(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(employeeId, from, to, page, size);
    }

    @GetMapping("/report")
    public EmployeeVoucherReportResponse report(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return service.report(employeeId, from, to);
    }
}
