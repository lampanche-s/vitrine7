package br.com.vitrine7.receipt.controller;

import br.com.vitrine7.receipt.dto.ReceiptResponse;
import br.com.vitrine7.receipt.service.ReceiptService;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checkouts")
@PreAuthorize("isAuthenticated()")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping("/{checkoutId}/receipt")
    public ReceiptResponse receipt(
            @PathVariable UUID checkoutId,
            @AuthenticationPrincipal VitrineUserPrincipal principal
    ) {
        return receiptService.getReceipt(
                checkoutId,
                principal
        );
    }
}
