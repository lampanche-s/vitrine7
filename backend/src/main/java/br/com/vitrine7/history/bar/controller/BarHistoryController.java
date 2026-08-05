package br.com.vitrine7.history.bar.controller;

import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.history.bar.dto.BarHistoryResponse;
import br.com.vitrine7.history.bar.dto.HistoryPaymentMethod;
import br.com.vitrine7.history.bar.service.BarHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bar/history")
@RequiredArgsConstructor
@Validated
public class BarHistoryController {

    private final BarHistoryService historyService;

    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('bar:access')")
    public List<BarHistoryResponse> recent(
            @RequestParam(required = false)
            Integer limit
    ) {
        return historyService.recent(limit);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('reports:access')")
    public PageResponse<BarHistoryResponse> list(
            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,

            @RequestParam(required = false)
            HistoryPaymentMethod paymentMethod
    ) {
        return historyService.list(
                page,
                size,
                search,
                from,
                to,
                paymentMethod
        );
    }
}
