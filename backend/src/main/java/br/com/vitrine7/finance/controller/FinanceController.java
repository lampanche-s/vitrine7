package br.com.vitrine7.finance.controller;

import br.com.vitrine7.checkout.entity.CheckoutOperationType;
import br.com.vitrine7.common.pagination.PageResponse;
import br.com.vitrine7.finance.dto.FinanceDailyResponse;
import br.com.vitrine7.finance.dto.FinanceModule;
import br.com.vitrine7.finance.dto.FinancePaymentMethod;
import br.com.vitrine7.finance.dto.FinanceSummaryResponse;
import br.com.vitrine7.finance.dto.FinanceTransactionResponse;
import br.com.vitrine7.finance.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
@Validated
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('reports:access')")
    public FinanceSummaryResponse summary(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestParam(required = false)
            FinanceModule module,

            @RequestParam(required = false)
            CheckoutOperationType operationType,

            @RequestParam(required = false)
            FinancePaymentMethod paymentMethod
    ) {
        return financeService.summary(
                from,
                to,
                module,
                operationType,
                paymentMethod
        );
    }

    @GetMapping("/daily")
    @PreAuthorize("hasAuthority('reports:access')")
    public List<FinanceDailyResponse> daily(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestParam(required = false)
            FinanceModule module,

            @RequestParam(required = false)
            CheckoutOperationType operationType,

            @RequestParam(required = false)
            FinancePaymentMethod paymentMethod
    ) {
        return financeService.daily(
                from,
                to,
                module,
                operationType,
                paymentMethod
        );
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('reports:access')")
    public PageResponse<FinanceTransactionResponse> transactions(
            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestParam(required = false)
            FinanceModule module,

            @RequestParam(required = false)
            CheckoutOperationType operationType,

            @RequestParam(required = false)
            FinancePaymentMethod paymentMethod,

            @RequestParam(required = false)
            String search
    ) {
        return financeService.transactions(
                page,
                size,
                from,
                to,
                module,
                operationType,
                paymentMethod,
                search
        );
    }
}
