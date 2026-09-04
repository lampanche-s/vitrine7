package br.com.vitrine7.payment.core.service;

import br.com.vitrine7.checkout.dto.CheckoutSessionResponse;
import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.checkout.repository.CheckoutSessionRepository;
import br.com.vitrine7.checkout.service.CheckoutFinalizationService;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.common.idempotency.IdempotencyFingerprintService;
import br.com.vitrine7.payment.core.dto.CashPaymentRequest;
import br.com.vitrine7.payment.core.dto.ManualPaymentRequest;
import br.com.vitrine7.payment.core.dto.PaymentConfirmationResponse;
import br.com.vitrine7.payment.core.dto.PixPaymentRequest;
import br.com.vitrine7.payment.core.dto.PaymentResponse;
import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.entity.PaymentMethod;
import br.com.vitrine7.payment.core.entity.PaymentProcessingMode;
import br.com.vitrine7.payment.core.entity.PaymentStatus;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String CASH_FINGERPRINT_VERSION =
            "payment-cash-v1";

    private static final String MANUAL_FINGERPRINT_VERSION =
            "payment-manual-v1";

    private static final String PIX_FINGERPRINT_VERSION =
            "payment-pix-v1";

    private final PaymentRepository paymentRepository;
    private final CheckoutSessionRepository checkoutRepository;
    private final PaymentExecutionService executionService;
    private final PaymentIdempotencyValidator idempotencyValidator;
    private final CheckoutFinalizationService finalizationService;
    private final IdempotencyFingerprintService fingerprintService;
    private final PaymentRequestNormalizer requestNormalizer;

    public ConfirmationResult confirmCash(
            UUID checkoutId,
            UUID idempotencyKey,
            CashPaymentRequest request,
            VitrineUserPrincipal principal
    ) {
        String canonicalPayload =
                CASH_FINGERPRINT_VERSION
                        + "|checkoutId="
                        + checkoutId
                        + "|method=CASH"
                        + "|amountCents="
                        + request.amountCents()
                        + "|cashReceivedCents="
                        + request.cashReceivedCents();

        String fingerprint =
                fingerprintService.sha256(
                        canonicalPayload
                );

        PaymentExecutionCommand command =
                new PaymentExecutionCommand(
                        checkoutId,
                        idempotencyKey,
                        fingerprint,
                        PaymentMethod.CASH,
                        PaymentProcessingMode.CASH,
                        request.amountCents(),
                        request.cashReceivedCents(),
                        null,
                        principal.getId()
                );

        return execute(command);
    }

    public ConfirmationResult confirmManual(
            UUID checkoutId,
            UUID idempotencyKey,
            ManualPaymentRequest request,
            VitrineUserPrincipal principal
    ) {
        requestNormalizer.validateManualMethod(
                request.method()
        );

        String normalizedReason =
                requestNormalizer
                        .normalizeManualReason(
                                request.reason()
                        );

        String canonicalPayload =
                MANUAL_FINGERPRINT_VERSION
                        + "|checkoutId="
                        + checkoutId
                        + "|method="
                        + request.method().name()
                        + "|reason="
                        + normalizedReason
                        + "|amountCents="
                        + request.amountCents();

        String fingerprint =
                fingerprintService.sha256(
                        canonicalPayload
                );

        PaymentExecutionCommand command =
                new PaymentExecutionCommand(
                        checkoutId,
                        idempotencyKey,
                        fingerprint,
                        request.method(),
                        PaymentProcessingMode.MANUAL_FALLBACK,
                        request.amountCents(),
                        null,
                        normalizedReason,
                        principal.getId()
                );

        return execute(command);
    }

    public ConfirmationResult confirmPix(
            UUID checkoutId,
            UUID idempotencyKey,
            VitrineUserPrincipal principal
    ) {
        return confirmPix(
                checkoutId,
                idempotencyKey,
                null,
                principal
        );
    }

    public ConfirmationResult confirmPix(
            UUID checkoutId,
            UUID idempotencyKey,
            PixPaymentRequest request,
            VitrineUserPrincipal principal
    ) {
        Long amountCents = request == null ? null : request.amountCents();
        String canonicalPayload =
                PIX_FINGERPRINT_VERSION
                        + "|checkoutId="
                        + checkoutId
                        + "|method=PIX"
                        + "|amountCents="
                        + amountCents;

        String fingerprint =
                fingerprintService.sha256(
                        canonicalPayload
                );

        PaymentExecutionCommand command =
                new PaymentExecutionCommand(
                        checkoutId,
                        idempotencyKey,
                        fingerprint,
                        PaymentMethod.PIX,
                        PaymentProcessingMode.MANUAL_FALLBACK,
                        amountCents,
                        null,
                        "Pix confirmado.",
                        principal.getId()
                );

        return execute(command);
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(UUID paymentId) {
        PaymentEntity payment =
                paymentRepository.findById(paymentId)
                        .filter(current -> current.getStatus() != PaymentStatus.SUPERSEDED)
                        .orElseThrow(() -> new NotFoundException(
                                "PAYMENT_NOT_FOUND",
                                "Pagamento nao encontrado."
                        ));

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listByCheckout(
            UUID checkoutId
    ) {
        if (!checkoutRepository.existsById(checkoutId)) {
            throw new NotFoundException(
                    "CHECKOUT_NOT_FOUND",
                    "Checkout nao encontrado."
            );
        }

        return paymentRepository
                .findAllByCheckoutSessionIdOrderByCreatedAtAsc(
                        checkoutId
                )
                .stream()
                .filter(payment -> payment.getStatus() != PaymentStatus.SUPERSEDED)
                .map(PaymentResponse::from)
                .toList();
    }

    private ConfirmationResult execute(
            PaymentExecutionCommand command
    ) {
        PaymentEntity existing =
                paymentRepository
                        .findByIdempotencyKey(
                                command.idempotencyKey()
                        )
                        .orElse(null);

        if (existing != null) {
            idempotencyValidator.validateReplay(
                    existing,
                    command
            );

            finalizeApprovedPayment(existing);

            return buildResult(
                    existing,
                    true
            );
        }

        try {
            PaymentExecutionService.ExecutionResult execution =
                    executionService.execute(command);

            finalizeApprovedPayment(
                    execution.payment()
            );

            return buildResult(
                    execution.payment(),
                    execution.replayed()
            );

        } catch (DataIntegrityViolationException exception) {
            PaymentEntity concurrentlyCreated =
                    paymentRepository
                            .findByIdempotencyKey(
                                    command.idempotencyKey()
                            )
                            .orElseThrow(() -> exception);

            idempotencyValidator.validateReplay(
                    concurrentlyCreated,
                    command
            );

            finalizeApprovedPayment(
                    concurrentlyCreated
            );

            return buildResult(
                    concurrentlyCreated,
                    true
            );
        }
    }

    private void finalizeApprovedPayment(
            PaymentEntity payment
    ) {
        if (payment.getStatus()
                != PaymentStatus.APPROVED) {
            return;
        }

        Long actorUserId =
                payment.getApprovedByUserId() != null
                        ? payment.getApprovedByUserId()
                        : payment.getCreatedByUserId();

        CheckoutSessionEntity checkout = checkoutRepository
                .findById(payment.getCheckoutSessionId())
                .orElse(null);

        if (checkout == null
                || (checkout.getStatus()
                        != br.com.vitrine7.checkout.entity.CheckoutStatus.PAID
                    && checkout.getStatus()
                        != br.com.vitrine7.checkout.entity.CheckoutStatus.FINALIZED)) {
            return;
        }

        finalizationService.finalizeCheckoutIfSupported(
                payment.getCheckoutSessionId(),
                actorUserId
        );
    }

    private ConfirmationResult buildResult(
            PaymentEntity payment,
            boolean replayed
    ) {
        CheckoutSessionEntity checkout =
                checkoutRepository
                        .findById(
                                payment.getCheckoutSessionId()
                        )
                        .orElseThrow(() -> new NotFoundException(
                                "CHECKOUT_NOT_FOUND",
                                "Checkout nao encontrado."
                        ));

        PaymentConfirmationResponse response =
                new PaymentConfirmationResponse(
                        PaymentResponse.from(payment),
                        CheckoutSessionResponse.from(
                                checkout
                        )
                );

        return new ConfirmationResult(
                response,
                replayed
        );
    }

    public record ConfirmationResult(
            PaymentConfirmationResponse response,
            boolean replayed
    ) {
    }
}
