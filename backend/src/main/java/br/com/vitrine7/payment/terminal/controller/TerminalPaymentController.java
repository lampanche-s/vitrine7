package br.com.vitrine7.payment.terminal.controller;

import br.com.vitrine7.payment.terminal.dto.PaymentTerminalTransactionResponse;
import br.com.vitrine7.payment.terminal.dto.TerminalPaymentConfirmationResponse;
import br.com.vitrine7.payment.terminal.dto.TerminalPaymentRequest;
import br.com.vitrine7.payment.terminal.service.TerminalPaymentService;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize(
        "hasAuthority('bar:access')"
)
public class TerminalPaymentController {

    private final TerminalPaymentService terminalPaymentService;

    @PostMapping(
            "/checkouts/{checkoutId}/payments/terminal"
    )
    public ResponseEntity<TerminalPaymentConfirmationResponse>
    process(
            @PathVariable UUID checkoutId,

            @RequestHeader("Idempotency-Key")
            UUID idempotencyKey,

            @Valid
            @RequestBody
            TerminalPaymentRequest request,

            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        TerminalPaymentService.TerminalConfirmationResult
                result =
                terminalPaymentService.process(
                        checkoutId,
                        idempotencyKey,
                        request,
                        principal
                );

        HttpStatus status = result.replayed()
                ? HttpStatus.OK
                : HttpStatus.CREATED;

        return ResponseEntity
                .status(status)
                .header(
                        "Idempotent-Replayed",
                        Boolean.toString(
                                result.replayed()
                        )
                )
                .body(result.response());
    }

    @GetMapping(
            "/payment-terminal/transactions/{transactionId}"
    )
    public PaymentTerminalTransactionResponse findById(
            @PathVariable UUID transactionId
    ) {
        return terminalPaymentService.findById(
                transactionId
        );
    }

    @GetMapping(
            "/checkouts/{checkoutId}/payment-terminal-transactions"
    )
    public List<PaymentTerminalTransactionResponse>
    listByCheckout(
            @PathVariable UUID checkoutId
    ) {
        return terminalPaymentService.listByCheckout(
                checkoutId
        );
    }

}
