package br.com.vitrine7.print.controller;

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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAuthority('bar:access')")
public class PrintJobController {

    private final PrintJobService service;

    public PrintJobController(PrintJobService service) {
        this.service = service;
    }

    @PostMapping("/checkouts/{checkoutId}/print-jobs")
    public PrintJobDtos.Created create(
            @PathVariable UUID checkoutId,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return service.create(checkoutId, principal);
    }

    @PostMapping("/bar/tabs/{tabId}/prepayment-print-jobs")
    public PrintJobDtos.Created createPrePaymentNote(
            @PathVariable Long tabId,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return service.createPrePaymentNote(tabId, principal);
    }

    @GetMapping("/print-jobs/{id}")
    public PrintJobDtos.Status status(@PathVariable UUID id) {
        return service.status(id);
    }
}
