package br.com.vitrine7.payment.terminal.service;

import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.checkout.repository.CheckoutSessionRepository;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.entity.PaymentStatus;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import br.com.vitrine7.payment.core.service.CheckoutPaymentAllocationService;
import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentResult;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalTransactionEntity;
import br.com.vitrine7.payment.terminal.repository.PaymentTerminalTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TerminalPaymentCompletionService {

    private final CheckoutSessionRepository checkoutRepository;
    private final PaymentRepository paymentRepository;
    private final CheckoutPaymentAllocationService allocationService;
    private final PaymentTerminalTransactionRepository
            terminalTransactionRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletionResult complete(
            UUID checkoutId,
            UUID paymentId,
            ProviderPaymentResult adapterResult,
            Long actorUserId
    ) {
        CheckoutSessionEntity checkout =
                checkoutRepository
                        .findByIdForUpdate(checkoutId)
                        .orElseThrow(() -> new NotFoundException(
                                "CHECKOUT_NOT_FOUND",
                                "Checkout nao encontrado."
                        ));

        PaymentEntity payment =
                paymentRepository
                        .findByIdForUpdate(paymentId)
                        .orElseThrow(() -> new NotFoundException(
                                "PAYMENT_NOT_FOUND",
                                "Pagamento nao encontrado."
                        ));

        PaymentTerminalTransactionEntity transaction =
                terminalTransactionRepository
                        .findByPaymentIdForUpdate(paymentId)
                        .orElseThrow(() -> new NotFoundException(
                                "PAYMENT_TERMINAL_TRANSACTION_NOT_FOUND",
                                "Transacao da maquininha nao foi encontrada."
                        ));

        if (payment.getStatus() == PaymentStatus.APPROVED
                || payment.getStatus() == PaymentStatus.DECLINED) {

            return new CompletionResult(
                    payment,
                    transaction,
                    checkout
            );
        }

        OffsetDateTime now =
                OffsetDateTime.now(clock);

        if (adapterResult.approved()) {
            payment.markApproved(
                    actorUserId,
                    now
            );

            transaction.markApproved(
                    adapterResult.providerReference(),
                    "00",
                    "Transacao aprovada.",
                    now,
                    adapterResult.providerRequestId(),
                    adapterResult.metadata()
            );

            paymentRepository.flush();
            allocationService.applyApprovedPayment(
                    checkout,
                    now
            );

        } else {
            payment.markDeclined(now);

            transaction.markDeclined(
                    adapterResult.providerReference(),
                    adapterResult.failureCode(),
                    adapterResult.failureMessage(),
                    now,
                    adapterResult.providerRequestId(),
                    adapterResult.failureCode(),
                    adapterResult.failureMessage(),
                    adapterResult.metadata()
            );

            checkout.markPaymentFailed();

        }

        return new CompletionResult(
                payment,
                transaction,
                checkout
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletionResult failCommunication(
            UUID checkoutId,
            UUID paymentId,
            String errorCode,
            String errorMessage
    ) {
        CheckoutSessionEntity checkout =
                checkoutRepository
                        .findByIdForUpdate(checkoutId)
                        .orElseThrow(() -> new NotFoundException(
                                "CHECKOUT_NOT_FOUND",
                                "Checkout nao encontrado."
                        ));

        PaymentEntity payment =
                paymentRepository
                        .findByIdForUpdate(paymentId)
                        .orElseThrow(() -> new NotFoundException(
                                "PAYMENT_NOT_FOUND",
                                "Pagamento nao encontrado."
                        ));

        PaymentTerminalTransactionEntity transaction =
                terminalTransactionRepository
                        .findByPaymentIdForUpdate(paymentId)
                        .orElseThrow(() -> new NotFoundException(
                                "PAYMENT_TERMINAL_TRANSACTION_NOT_FOUND",
                                "Transacao da maquininha nao foi encontrada."
                        ));

        if (payment.getStatus() == PaymentStatus.PROCESSING) {
            OffsetDateTime now =
                    OffsetDateTime.now(clock);

            payment.markDeclined(now);

            transaction.markError(
                    errorCode,
                    normalizeErrorMessage(errorMessage),
                    now
            );

            checkout.markPaymentFailed();

        }

        return new CompletionResult(
                payment,
                transaction,
                checkout
        );
    }

    private String normalizeErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Falha de comunicacao com a maquininha.";
        }

        String normalized = message
                .trim()
                .replaceAll("\\s+", " ");

        return normalized.length() <= 255
                ? normalized
                : normalized.substring(0, 255);
    }

    public record CompletionResult(
            PaymentEntity payment,
            PaymentTerminalTransactionEntity transaction,
            CheckoutSessionEntity checkout
    ) {
    }
}
