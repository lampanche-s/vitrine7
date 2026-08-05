package br.com.vitrine7.payment.terminal.service;

import br.com.vitrine7.checkout.dto.CheckoutSessionResponse;
import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.checkout.repository.CheckoutSessionRepository;
import br.com.vitrine7.checkout.service.CheckoutFinalizationService;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.common.idempotency.IdempotencyFingerprintService;
import br.com.vitrine7.payment.core.dto.PaymentResponse;
import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.entity.PaymentMethod;
import br.com.vitrine7.payment.core.entity.PaymentStatus;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import br.com.vitrine7.payment.core.service.PaymentIdempotencyValidator;
import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentCommand;
import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentResult;
import br.com.vitrine7.payment.terminal.dto.PaymentTerminalTransactionResponse;
import br.com.vitrine7.payment.terminal.dto.TerminalPaymentConfirmationResponse;
import br.com.vitrine7.payment.terminal.dto.TerminalPaymentRequest;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalTransactionEntity;
import br.com.vitrine7.payment.terminal.repository.PaymentTerminalTransactionRepository;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TerminalPaymentService {

    private static final String FINGERPRINT_VERSION =
            "payment-terminal-v1";

    private final PaymentRepository paymentRepository;
    private final CheckoutSessionRepository checkoutRepository;
    private final PaymentTerminalTransactionRepository
            terminalTransactionRepository;
    private final TerminalPaymentStartService startService;
    private final TerminalPaymentCompletionService completionService;
    private final PaymentIdempotencyValidator idempotencyValidator;
    private final IdempotencyFingerprintService fingerprintService;
    private final CheckoutFinalizationService finalizationService;

    public TerminalConfirmationResult process(
            UUID checkoutId,
            UUID idempotencyKey,
            TerminalPaymentRequest request,
            VitrineUserPrincipal principal
    ) {
        validateMethod(request.method());

        String fingerprint =
                createFingerprint(
                        checkoutId,
                        request.method()
                );

        PaymentEntity existing =
                paymentRepository
                        .findByIdempotencyKey(idempotencyKey)
                        .orElse(null);

        if (existing != null) {
            idempotencyValidator.validateReplay(
                    existing,
                    checkoutId,
                    fingerprint,
                    principal.getId()
            );

            finalizeApprovedPayment(existing);

            return buildResult(
                    existing,
                    true
            );
        }

        TerminalPaymentExecutionCommand command =
                new TerminalPaymentExecutionCommand(
                        checkoutId,
                        idempotencyKey,
                        fingerprint,
                        request.method(),
                        principal.getId()
                );

        TerminalPaymentStartService.StartResult started;

        try {
            started = startService.start(command);

        } catch (DataIntegrityViolationException exception) {
            PaymentEntity concurrentlyCreated =
                    paymentRepository
                            .findByIdempotencyKey(idempotencyKey)
                            .orElseThrow(() -> exception);

            idempotencyValidator.validateReplay(
                    concurrentlyCreated,
                    checkoutId,
                    fingerprint,
                    principal.getId()
            );

            finalizeApprovedPayment(
                    concurrentlyCreated
            );

            return buildResult(
                    concurrentlyCreated,
                    true
            );
        }

        ProviderPaymentResult adapterResult;

        try {
            adapterResult = started.activeProvider()
                    .adapter()
                    .initiatePayment(
                    new ProviderPaymentCommand(
                            checkoutId,
                            started.payment().getId(),
                            request.method(),
                            started.checkout().getTotalCents(),
                            started.activeProvider()
                                    .configuration()
                    )
            );

        } catch (RuntimeException exception) {
            if (exception instanceof BusinessException businessException
                    && businessException.getCode()
                            .equals("PAYMENT_TERMINAL_COMMAND_EXPIRED")) {

                /*
                 * O timeout HTTP não comprova que a transação falhou.
                 * A maquininha pode aprovar e o resultado chegar depois.
                 *
                 * Por isso, pagamento e checkout permanecem em processamento.
                 */
                throw businessException;
            }

            completionService.failCommunication(
                    checkoutId,
                    started.payment().getId(),
                    "TERMINAL_COMMUNICATION_ERROR",
                    exception.getMessage()
            );

            if (exception instanceof BusinessException businessException
                    && (businessException.getCode()
                            .equals("PAYMENT_TERMINAL_DEVICE_NOT_AVAILABLE")
                    || businessException.getCode()
                            .equals("PAYMENT_TERMINAL_COMMAND_CONFLICT"))) {

                throw businessException;
            }

            throw new BusinessException(
                    "PAYMENT_TERMINAL_COMMUNICATION_ERROR",
                    "Nao foi possivel concluir a comunicacao com a maquininha."
            );
        }

        TerminalPaymentCompletionService.CompletionResult
                completed =
                completionService.complete(
                        checkoutId,
                        started.payment().getId(),
                        adapterResult,
                        principal.getId()
                );

        finalizeApprovedPayment(
                completed.payment()
        );

        return buildResult(
                completed.payment(),
                false
        );
    }

    @Transactional(readOnly = true)
    public PaymentTerminalTransactionResponse findById(
            UUID transactionId
    ) {
        PaymentTerminalTransactionEntity transaction =
                terminalTransactionRepository
                        .findById(transactionId)
                        .orElseThrow(() -> new NotFoundException(
                                "PAYMENT_TERMINAL_TRANSACTION_NOT_FOUND",
                                "Transacao da maquininha nao foi encontrada."
                        ));

        return PaymentTerminalTransactionResponse.from(
                transaction
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentTerminalTransactionResponse>
    listByCheckout(UUID checkoutId) {
        if (!checkoutRepository.existsById(checkoutId)) {
            throw new NotFoundException(
                    "CHECKOUT_NOT_FOUND",
                    "Checkout nao encontrado."
            );
        }

        return terminalTransactionRepository
                .findAllByCheckoutSessionIdOrderByCreatedAtAsc(
                        checkoutId
                )
                .stream()
                .map(
                        PaymentTerminalTransactionResponse::from
                )
                .toList();
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

        finalizationService
                .finalizeCheckoutIfSupported(
                        payment.getCheckoutSessionId(),
                        actorUserId
                );
    }

    private TerminalConfirmationResult buildResult(
            PaymentEntity payment,
            boolean replayed
    ) {
        PaymentTerminalTransactionEntity transaction =
                terminalTransactionRepository
                        .findByPaymentId(payment.getId())
                        .orElseThrow(() -> new NotFoundException(
                                "PAYMENT_TERMINAL_TRANSACTION_NOT_FOUND",
                                "Transacao da maquininha nao foi encontrada."
                        ));

        CheckoutSessionEntity checkout =
                checkoutRepository
                        .findById(
                                payment.getCheckoutSessionId()
                        )
                        .orElseThrow(() -> new NotFoundException(
                                "CHECKOUT_NOT_FOUND",
                                "Checkout nao encontrado."
                        ));

        TerminalPaymentConfirmationResponse response =
                new TerminalPaymentConfirmationResponse(
                        PaymentResponse.from(payment),
                        PaymentTerminalTransactionResponse.from(
                                transaction
                        ),
                        CheckoutSessionResponse.from(
                                checkout
                        )
                );

        return new TerminalConfirmationResult(
                response,
                replayed
        );
    }

    private void validateMethod(PaymentMethod method) {
        if (method == null
                || method == PaymentMethod.CASH
                || method == PaymentMethod.PIX) {

            throw new BusinessException(
                    "INVALID_TERMINAL_PAYMENT_METHOD",
                    "A maquininha aceita apenas credito ou debito."
            );
        }
    }

    private String createFingerprint(
            UUID checkoutId,
            PaymentMethod method
    ) {
        String canonicalPayload =
                FINGERPRINT_VERSION
                        + "|checkoutId="
                        + checkoutId
                        + "|method="
                        + method.name();

        return fingerprintService.sha256(
                canonicalPayload
        );
    }

    public record TerminalConfirmationResult(
            TerminalPaymentConfirmationResponse response,
            boolean replayed
    ) {
    }
}
