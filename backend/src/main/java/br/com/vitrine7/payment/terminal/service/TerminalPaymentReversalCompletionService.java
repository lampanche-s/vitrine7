package br.com.vitrine7.payment.terminal.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.entity.PaymentStatus;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalTransactionEntity;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalTransactionStatus;
import br.com.vitrine7.payment.terminal.provider.ProviderPaymentStatus;
import br.com.vitrine7.payment.terminal.repository.PaymentTerminalTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TerminalPaymentReversalCompletionService {

    private final PaymentRepository paymentRepository;

    private final PaymentTerminalTransactionRepository
            transactionRepository;

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public ReversalCompletionResult complete(
            UUID transactionId,
            ProviderPaymentStatus providerStatus,
            Long actorUserId,
            String reason,
            OffsetDateTime completedAt
    ) {
        PaymentTerminalTransactionEntity reference =
                transactionRepository
                        .findById(transactionId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "PAYMENT_TERMINAL_TRANSACTION_NOT_FOUND",
                                        "Transacao da maquininha nao encontrada."
                                )
                        );

        PaymentEntity payment =
                paymentRepository
                        .findByIdForUpdate(
                                reference.getPaymentId()
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "PAYMENT_NOT_FOUND",
                                        "Pagamento nao encontrado."
                                )
                        );

        PaymentTerminalTransactionEntity transaction =
                transactionRepository
                        .findByIdForUpdate(transactionId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "PAYMENT_TERMINAL_TRANSACTION_NOT_FOUND",
                                        "Transacao da maquininha nao encontrada."
                                )
                        );

        if (!transaction.getPaymentId()
                .equals(payment.getId())) {

            throw new BusinessException(
                    "PAYMENT_TERMINAL_TRANSACTION_MISMATCH",
                    "A transacao nao pertence ao pagamento informado."
            );
        }

        if (transaction.getStatus()
                != PaymentTerminalTransactionStatus.APPROVED) {

            throw new BusinessException(
                    "PAYMENT_TERMINAL_TRANSACTION_NOT_APPROVED",
                    "Somente uma transacao aprovada pode ser estornada."
            );
        }

        if (payment.getStatus()
                == PaymentStatus.REVERSED) {

            return new ReversalCompletionResult(
                    payment,
                    transaction,
                    true
            );
        }

        if (payment.getStatus()
                != PaymentStatus.REVERSAL_PENDING) {

            throw new BusinessException(
                    "PAYMENT_REVERSAL_NOT_PENDING",
                    "O pagamento nao possui estorno pendente."
            );
        }

        if (providerStatus
                == ProviderPaymentStatus.APPROVED) {

            payment.markReversed(
                    actorUserId,
                    reason,
                    completedAt
            );

        } else if (
                providerStatus
                        == ProviderPaymentStatus.CANCELLED
                        || providerStatus
                        == ProviderPaymentStatus.DECLINED
        ) {
            payment.restoreApprovedAfterReversalFailure();
        }

        /*
         * ERROR e UNKNOWN nao comprovam que o estorno falhou.
         * O pagamento permanece REVERSAL_PENDING para impedir
         * uma segunda tentativa potencialmente duplicada.
         */

        return new ReversalCompletionResult(
                payment,
                transaction,
                false
        );
    }

    public record ReversalCompletionResult(
            PaymentEntity payment,
            PaymentTerminalTransactionEntity transaction,
            boolean replayed
    ) {
    }
}
