package br.com.vitrine7.payment.core.service;

import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.checkout.entity.CheckoutStatus;
import br.com.vitrine7.checkout.repository.CheckoutSessionRepository;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.payment.core.config.PaymentProperties;
import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.entity.PaymentProcessingMode;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PaymentExecutionService {

    private final CheckoutSessionRepository checkoutRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentIdempotencyValidator idempotencyValidator;
    private final CheckoutPaymentAllocationService allocationService;
    private final PaymentProperties paymentProperties;
    private final Clock clock;

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public ExecutionResult execute(
            PaymentExecutionCommand command
    ) {
        CheckoutSessionEntity checkout =
                checkoutRepository
                        .findByIdForUpdate(command.checkoutId())
                        .orElseThrow(() -> new NotFoundException(
                                "CHECKOUT_NOT_FOUND",
                                "Checkout nao encontrado."
                        ));

        PaymentEntity replay =
                paymentRepository
                        .findByIdempotencyKey(
                                command.idempotencyKey()
                        )
                        .orElse(null);

        if (replay != null) {
            idempotencyValidator.validateReplay(
                    replay,
                    command
            );

            return new ExecutionResult(
                    replay,
                    checkout,
                    true
            );
        }

        OffsetDateTime now =
                OffsetDateTime.now(clock);

        checkout.expireIfNecessary(now);

        validateCheckoutCanReceivePayment(
                checkout
        );

        if (command.processingMode()
                == PaymentProcessingMode.MANUAL_FALLBACK
                && command.method()
                != br.com.vitrine7.payment.core.entity.PaymentMethod.PIX
                && !paymentProperties
                .isManualFallbackEnabled()) {

            throw new BusinessException(
                    "MANUAL_PAYMENT_FALLBACK_DISABLED",
                    "A confirmacao manual de pagamento esta desabilitada."
            );
        }

        CheckoutPaymentAllocationService.Allocation allocation =
                allocationService.resolve(
                        checkout,
                        command.amountCents()
                );

        long amountCents = allocation.requestedCents();

        checkout.markPaymentProcessing();

        PaymentEntity payment = createPayment(
                command,
                amountCents,
                now
        );

        PaymentEntity saved =
                paymentRepository.saveAndFlush(payment);

        allocationService.applyApprovedPayment(
                checkout,
                now
        );

        return new ExecutionResult(
                saved,
                checkout,
                false
        );
    }

    private void validateCheckoutCanReceivePayment(
            CheckoutSessionEntity checkout
    ) {
        if (checkout.getStatus()
                == CheckoutStatus.EXPIRED) {

            throw new BusinessException(
                    "CHECKOUT_EXPIRED",
                    "O checkout expirou e nao pode ser pago."
            );
        }

        if (checkout.getStatus()
                == CheckoutStatus.CANCELLED) {

            throw new BusinessException(
                    "CHECKOUT_CANCELLED",
                    "O checkout foi cancelado e nao pode ser pago."
            );
        }

        if (checkout.getStatus()
                == CheckoutStatus.PAID
                || checkout.getStatus()
                == CheckoutStatus.FINALIZED) {

            throw new BusinessException(
                    "CHECKOUT_ALREADY_PAID",
                    "Este checkout ja esta pago."
            );
        }

        if (checkout.getStatus()
                != CheckoutStatus.READY_FOR_PAYMENT
                && checkout.getStatus()
                != CheckoutStatus.PAYMENT_FAILED) {

            throw new BusinessException(
                    "CHECKOUT_NOT_READY_FOR_PAYMENT",
                    "O checkout nao esta pronto para receber pagamento."
            );
        }

        if (checkout.getSourceId() == null) {
            throw new BusinessException(
                    "CHECKOUT_SOURCE_REQUIRED",
                    "O checkout ainda nao esta vinculado a uma operacao."
            );
        }

        if (checkout.getTotalCents() == null
                || checkout.getTotalCents() <= 0) {

            throw new BusinessException(
                    "INVALID_CHECKOUT_TOTAL",
                    "O total do checkout deve ser maior que zero."
            );
        }

    }

    private PaymentEntity createPayment(
            PaymentExecutionCommand command,
            long amountCents,
            OffsetDateTime now
    ) {
        return switch (command.processingMode()) {
            case CASH -> createCashPayment(
                    command,
                    amountCents,
                    now
            );

            case MANUAL_FALLBACK ->
                    PaymentEntity.approvedManualFallback(
                            command.checkoutId(),
                            command.idempotencyKey(),
                            command.requestFingerprint(),
                            command.method(),
                            amountCents,
                            command.manualReason(),
                            command.actorUserId(),
                            now
                    );

            case TERMINAL_SIMULATED, TERMINAL_REAL ->
                    throw new BusinessException(
                            "PAYMENT_MODE_NOT_AVAILABLE",
                            "O modo de pagamento informado ainda nao esta disponivel."
                    );
        };
    }

    private PaymentEntity createCashPayment(
            PaymentExecutionCommand command,
            long amountCents,
            OffsetDateTime now
    ) {
        Long received =
                command.cashReceivedCents();

        if (received == null
                || received < amountCents) {

            throw new BusinessException(
                    "INSUFFICIENT_CASH_RECEIVED",
                    "O valor recebido e menor que o total do checkout."
            );
        }

        return PaymentEntity.approvedCash(
                command.checkoutId(),
                command.idempotencyKey(),
                command.requestFingerprint(),
                amountCents,
                received,
                command.actorUserId(),
                now
        );
    }

    public record ExecutionResult(
            PaymentEntity payment,
            CheckoutSessionEntity checkout,
            boolean replayed
    ) {
    }
}
