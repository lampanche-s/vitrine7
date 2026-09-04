package br.com.vitrine7.payment.terminal.service;

import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.checkout.entity.CheckoutStatus;
import br.com.vitrine7.checkout.repository.CheckoutSessionRepository;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.entity.PaymentProcessingMode;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import br.com.vitrine7.payment.core.service.CheckoutPaymentAllocationService;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalMode;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalProvider;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalTransactionEntity;
import br.com.vitrine7.payment.terminal.provider.ActivePaymentProviderService;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import br.com.vitrine7.payment.terminal.repository.PaymentTerminalTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class TerminalPaymentStartService {

    private final CheckoutSessionRepository checkoutRepository;
    private final PaymentRepository paymentRepository;
    private final CheckoutPaymentAllocationService allocationService;
    private final PaymentTerminalTransactionRepository
            terminalTransactionRepository;
    private final ActivePaymentProviderService activePaymentProviderService;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StartResult start(
            TerminalPaymentExecutionCommand command
    ) {
        CheckoutSessionEntity checkout =
                checkoutRepository
                        .findByIdForUpdate(command.checkoutId())
                        .orElseThrow(() -> new NotFoundException(
                                "CHECKOUT_NOT_FOUND",
                                "Checkout nao encontrado."
                        ));

        OffsetDateTime now =
                OffsetDateTime.now(clock);

        checkout.expireIfNecessary(now);
        validateCheckout(checkout);

        CheckoutPaymentAllocationService.Allocation allocation =
                allocationService.resolve(
                        checkout,
                        command.amountCents()
                );

        ActivePaymentProviderService.ActiveProvider activeProvider =
                activePaymentProviderService.requireActiveProvider();

        PaymentProcessingMode processingMode =
                activeProvider.profile().providerCode()
                        == PaymentProviderCode.SIMULATOR
                        ? PaymentProcessingMode.TERMINAL_SIMULATED
                        : PaymentProcessingMode.TERMINAL_REAL;

        checkout.markPaymentProcessing();

        PaymentEntity payment =
                PaymentEntity.processingTerminal(
                        checkout.getId(),
                        command.idempotencyKey(),
                        command.requestFingerprint(),
                        command.method(),
                        processingMode,
                        allocation.requestedCents(),
                        command.actorUserId()
                );

        PaymentEntity savedPayment =
                paymentRepository.saveAndFlush(payment);

        PaymentTerminalTransactionEntity transaction =
                PaymentTerminalTransactionEntity.sent(
                        savedPayment.getId(),
                        checkout.getId(),
                        command.idempotencyKey(),
                        command.requestFingerprint(),
                        PaymentTerminalProvider.valueOf(
                                activeProvider.profile()
                                        .providerCode()
                                        .name()
                        ),
                        activeProvider.profile().providerCode()
                                == PaymentProviderCode.SIMULATOR
                                ? PaymentTerminalMode.SIMULATED
                                : PaymentTerminalMode.REAL,
                        command.method(),
                        allocation.requestedCents(),
                        command.actorUserId(),
                        now,
                        activeProvider.profile().id(),
                        activeProvider.profile().providerCode(),
                        activeProvider.profile().environment(),
                        activeProvider.profile().configurationVersion()
                );

        PaymentTerminalTransactionEntity savedTransaction =
                terminalTransactionRepository
                        .saveAndFlush(transaction);

        return new StartResult(
                savedPayment,
                savedTransaction,
                checkout,
                activeProvider
        );
    }

    private void validateCheckout(
            CheckoutSessionEntity checkout
    ) {
        if (checkout.getStatus() == CheckoutStatus.EXPIRED) {
            throw new BusinessException(
                    "CHECKOUT_EXPIRED",
                    "O checkout expirou e nao pode ser pago."
            );
        }

        if (checkout.getStatus() == CheckoutStatus.CANCELLED) {
            throw new BusinessException(
                    "CHECKOUT_CANCELLED",
                    "O checkout foi cancelado e nao pode ser pago."
            );
        }

        if (checkout.getStatus() == CheckoutStatus.PAYMENT_PROCESSING) {
            throw new BusinessException(
                    "PAYMENT_ALREADY_PROCESSING",
                    "Ja existe um pagamento em processamento para este checkout."
            );
        }

        if (checkout.getStatus() == CheckoutStatus.PAID
                || checkout.getStatus() == CheckoutStatus.FINALIZED) {

            throw new BusinessException(
                    "CHECKOUT_ALREADY_PAID",
                    "Este checkout ja esta pago."
            );
        }

        if (checkout.getStatus() != CheckoutStatus.READY_FOR_PAYMENT
                && checkout.getStatus() != CheckoutStatus.PAYMENT_FAILED) {

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

    public record StartResult(
            PaymentEntity payment,
            PaymentTerminalTransactionEntity transaction,
            CheckoutSessionEntity checkout,
            ActivePaymentProviderService.ActiveProvider activeProvider
    ) {
    }
}
