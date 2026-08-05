package br.com.vitrine7.payment.core.controller;

import br.com.vitrine7.payment.core.dto.CashPaymentRequest;
import br.com.vitrine7.payment.core.dto.ManualPaymentRequest;
import br.com.vitrine7.payment.core.dto.PaymentConfirmationResponse;
import br.com.vitrine7.payment.core.dto.PaymentResponse;
import br.com.vitrine7.payment.core.service.PaymentService;
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
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping(
            "/checkouts/{checkoutId}/payments/cash"
    )
    public ResponseEntity<PaymentConfirmationResponse>
    confirmCash(
            @PathVariable UUID checkoutId,

            @RequestHeader("Idempotency-Key")
            UUID idempotencyKey,

            @Valid
            @RequestBody
            CashPaymentRequest request,

            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        PaymentService.ConfirmationResult result =
                paymentService.confirmCash(
                        checkoutId,
                        idempotencyKey,
                        request,
                        principal
                );

        return response(result);
    }

    @PostMapping(
            "/checkouts/{checkoutId}/payments/manual"
    )
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRADOR')")
    public ResponseEntity<PaymentConfirmationResponse>
    confirmManual(
            @PathVariable UUID checkoutId,

            @RequestHeader("Idempotency-Key")
            UUID idempotencyKey,

            @Valid
            @RequestBody
            ManualPaymentRequest request,

            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        PaymentService.ConfirmationResult result =
                paymentService.confirmManual(
                        checkoutId,
                        idempotencyKey,
                        request,
                        principal
                );

        return response(result);
    }

    @PostMapping(
            "/checkouts/{checkoutId}/payments/pix"
    )
    public ResponseEntity<PaymentConfirmationResponse>
    confirmPix(
            @PathVariable UUID checkoutId,

            @RequestHeader("Idempotency-Key")
            UUID idempotencyKey,

            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        PaymentService.ConfirmationResult result =
                paymentService.confirmPix(
                        checkoutId,
                        idempotencyKey,
                        principal
                );

        return response(result);
    }

    @GetMapping("/payments/{paymentId}")
    public PaymentResponse findById(
            @PathVariable UUID paymentId
    ) {
        return paymentService.findById(paymentId);
    }

    @GetMapping(
            "/checkouts/{checkoutId}/payments"
    )
    public List<PaymentResponse> listByCheckout(
            @PathVariable UUID checkoutId
    ) {
        return paymentService.listByCheckout(
                checkoutId
        );
    }

    private ResponseEntity<PaymentConfirmationResponse>
    response(
            PaymentService.ConfirmationResult result
    ) {
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
}
