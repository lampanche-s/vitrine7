package br.com.vitrine7.payment.terminal.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.common.config.BusinessProperties;
import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.entity.PaymentMethod;
import br.com.vitrine7.payment.core.entity.PaymentProcessingMode;
import br.com.vitrine7.payment.core.entity.PaymentStatus;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import br.com.vitrine7.payment.terminal.bridge.TerminalBridgeProperties;
import br.com.vitrine7.payment.terminal.bridge.TerminalCommandQueueService;
import br.com.vitrine7.payment.terminal.bridge.TerminalDeviceRepository;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalTransactionEntity;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalTransactionStatus;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import br.com.vitrine7.payment.terminal.repository.PaymentTerminalTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TerminalPaymentReversalStartService {

    private final PaymentRepository paymentRepository;

    private final PaymentTerminalTransactionRepository
            transactionRepository;

    private final TerminalDeviceRepository deviceRepository;

    private final TerminalCommandQueueService queueService;

    private final TerminalBridgeProperties bridgeProperties;

    private final BusinessProperties businessProperties;

    private final Clock clock;

    @Transactional
    public StartResult start(
            UUID paymentId,
            String reason,
            Long actorUserId
    ) {
        String normalizedReason =
                PaymentEntity.normalizeReversalReason(
                        reason
                );

        PaymentEntity payment =
                paymentRepository
                        .findByIdForUpdate(paymentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "PAYMENT_NOT_FOUND",
                                        "Pagamento nao encontrado."
                                )
                        );

        validatePayment(payment);

        PaymentTerminalTransactionEntity transaction =
                transactionRepository
                        .findByPaymentIdForUpdate(paymentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "PAYMENT_TERMINAL_TRANSACTION_NOT_FOUND",
                                        "Transacao da maquininha nao encontrada."
                                )
                        );

        validateTransaction(transaction);

        UUID deviceId =
                transaction.getTerminalDeviceId();

        if (deviceId == null) {
            throw new BusinessException(
                    "PAYMENT_REVERSAL_DEVICE_NOT_FOUND",
                    "O dispositivo usado no pagamento nao foi encontrado."
            );
        }

        OffsetDateTime onlineCutoff =
                OffsetDateTime.now(clock)
                        .minus(
                                bridgeProperties.offlineAfter()
                        );

        if (!deviceRepository.isAvailable(
                deviceId,
                onlineCutoff
        )) {
            throw new BusinessException(
                    "PAYMENT_TERMINAL_DEVICE_NOT_AVAILABLE",
                    "O agente PagBank usado no pagamento esta offline."
            );
        }

        payment.markReversalPending();

        UUID commandId =
                queueService.createReversal(
                        transaction.getId(),
                        deviceId,
                        actorUserId,
                        normalizedReason,
                        transaction.getProviderReference()
                );

        return new StartResult(
                commandId,
                payment.getId(),
                transaction.getId()
        );
    }

    private void validatePayment(
            PaymentEntity payment
    ) {
        if (payment.getStatus()
                == PaymentStatus.REVERSED) {

            throw new BusinessException(
                    "PAYMENT_ALREADY_REVERSED",
                    "Este pagamento ja foi estornado."
            );
        }

        if (payment.getStatus()
                == PaymentStatus.REVERSAL_PENDING) {

            throw new BusinessException(
                    "PAYMENT_REVERSAL_ALREADY_PENDING",
                    "Este pagamento ja possui um estorno em processamento."
            );
        }

        if (payment.getStatus()
                != PaymentStatus.APPROVED) {

            throw new BusinessException(
                    "PAYMENT_CANNOT_BE_REVERSED",
                    "Somente um pagamento aprovado pode ser estornado."
            );
        }

        boolean cardPayment =
                payment.getMethod()
                        == PaymentMethod.CREDIT_CARD
                        || payment.getMethod()
                        == PaymentMethod.DEBIT_CARD;

        if (!cardPayment
                || payment.getProcessingMode()
                != PaymentProcessingMode.TERMINAL_REAL) {

            throw new BusinessException(
                    "PAYMENT_REVERSAL_UNSUPPORTED",
                    "Somente pagamentos reais de credito ou debito podem ser estornados pela maquininha."
            );
        }
    }

    private void validateTransaction(
            PaymentTerminalTransactionEntity transaction
    ) {
        if (transaction.getStatus()
                != PaymentTerminalTransactionStatus.APPROVED
                || transaction.getProviderCodeSnapshot()
                != PaymentProviderCode.PAGBANK) {

            throw new BusinessException(
                    "PAYMENT_REVERSAL_UNSUPPORTED",
                    "A transacao nao e um pagamento PagBank aprovado."
            );
        }

        if (transaction.getApprovedAt() == null) {
            throw new BusinessException(
                    "PAYMENT_REVERSAL_APPROVAL_DATE_NOT_FOUND",
                    "A data de aprovacao da transacao nao foi encontrada."
            );
        }

        ZoneId zone = businessTimeZone();

        LocalDate approvedDate =
                transaction.getApprovedAt()
                        .atZoneSameInstant(zone)
                        .toLocalDate();

        LocalDate currentDate =
                LocalDate.now(
                        clock.withZone(zone)
                );

        if (!approvedDate.equals(currentDate)) {
            throw new BusinessException(
                    "PAYMENT_REVERSAL_OUTSIDE_ALLOWED_DATE",
                    "O estorno presencial pela maquininha somente pode ser realizado no mesmo dia do pagamento."
            );
        }
    }

    private ZoneId businessTimeZone() {
        String configured =
                businessProperties.businessTimeZone();

        if (configured == null
                || configured.isBlank()) {

            return ZoneId.of("America/Bahia");
        }

        return ZoneId.of(configured);
    }

    public record StartResult(
            UUID commandId,
            UUID paymentId,
            UUID transactionId
    ) {
    }
}
