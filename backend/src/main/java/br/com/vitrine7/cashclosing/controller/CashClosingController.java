package br.com.vitrine7.cashclosing.controller;

import br.com.vitrine7.cashclosing.dto.CashClosingDay;
import br.com.vitrine7.cashclosing.dto.CashClosingResponse;
import br.com.vitrine7.cashclosing.service.CashClosingService;
import br.com.vitrine7.print.dto.PrintJobDtos;
import br.com.vitrine7.print.service.PrintJobService;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cash-closing")
@PreAuthorize("hasAuthority('reports:access')")
public class CashClosingController {

    private final CashClosingService service;
    private final PrintJobService printJobService;

    public CashClosingController(
            CashClosingService service,
            PrintJobService printJobService
    ) {
        this.service = service;
        this.printJobService = printJobService;
    }

    @GetMapping("/{day}")
    public CashClosingResponse get(
            @PathVariable CashClosingDay day,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return service.get(day, principal);
    }

    @PostMapping("/{day}/print-jobs")
    public PrintJobDtos.Created print(
            @PathVariable CashClosingDay day,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return printJobService.createCashClosing(day, principal);
    }

    @PostMapping("/{day}/close")
    public CashClosingResponse close(
            @PathVariable CashClosingDay day,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return service.close(day, principal);
    }
}
