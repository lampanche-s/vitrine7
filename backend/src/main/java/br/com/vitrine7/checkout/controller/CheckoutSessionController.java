package br.com.vitrine7.checkout.controller;

import br.com.vitrine7.checkout.dto.CancelCheckoutSessionRequest;
import br.com.vitrine7.checkout.dto.CheckoutFinalizationResponse;
import br.com.vitrine7.checkout.dto.CheckoutSessionResponse;
import br.com.vitrine7.checkout.dto.CreateCheckoutSessionRequest;
import br.com.vitrine7.checkout.service.CheckoutFinalizationService;
import br.com.vitrine7.checkout.service.CheckoutSessionService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checkouts")
@RequiredArgsConstructor
@PreAuthorize(
        "hasAuthority('bar:access')"
)
public class CheckoutSessionController {

    private final CheckoutSessionService checkoutService;
    private final CheckoutFinalizationService finalizationService;

    @PostMapping
    public ResponseEntity<CheckoutSessionResponse> open(
            @RequestHeader("Idempotency-Key")
            UUID idempotencyKey,

            @Valid
            @RequestBody
            CreateCheckoutSessionRequest request,

            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        CheckoutSessionService.OpenCheckoutResult result =
                checkoutService.open(
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
                        Boolean.toString(result.replayed())
                )
                .body(result.checkout());
    }

    @GetMapping("/{id}")
    public CheckoutSessionResponse findById(
            @PathVariable UUID id
    ) {
        return checkoutService.findById(id);
    }

    @PostMapping("/{id}/cancel")
    public CheckoutSessionResponse cancel(
            @PathVariable UUID id,

            @Valid
            @RequestBody
            CancelCheckoutSessionRequest request,

            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        return checkoutService.cancel(
                id,
                request,
                principal
        );
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<CheckoutFinalizationResponse>
    finalizeCheckout(
            @PathVariable UUID id,

            @AuthenticationPrincipal
            VitrineUserPrincipal principal
    ) {
        CheckoutFinalizationService.FinalizationResult result =
                finalizationService
                        .finalizeCheckoutRequired(
                                id,
                                principal.getId()
                        );

        HttpStatus status = result.finalizedNow()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        CheckoutFinalizationResponse response =
                new CheckoutFinalizationResponse(
                        result.checkout(),
                        result.finalizedNow(),
                        result.processedItems()
                );

        return ResponseEntity
                .status(status)
                .header(
                        "Idempotent-Replayed",
                        Boolean.toString(
                                !result.finalizedNow()
                        )
                )
                .body(response);
    }
}
