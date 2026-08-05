package br.com.vitrine7.payment.terminal.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.payment.core.dto.PaymentResponse;
import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.entity.PaymentStatus;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentResult;
import br.com.vitrine7.payment.terminal.bridge.TerminalCommandQueueService;
import br.com.vitrine7.payment.terminal.dto.PaymentTerminalTransactionResponse;
import br.com.vitrine7.payment.terminal.dto.TerminalPaymentReversalResponse;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalTransactionEntity;
import br.com.vitrine7.payment.terminal.provider.ProviderPaymentStatus;
import br.com.vitrine7.payment.terminal.repository.PaymentTerminalTransactionRepository;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TerminalPaymentReversalService {

    private final TerminalPaymentReversalStartService
            startService;

    private final TerminalCommandQueueService
            queueService;

    private final PaymentRepository paymentRepository;

    private final PaymentTerminalTransactionRepository
            transactionRepository;

    public TerminalPaymentReversalResponse reverse(
            UUID paymentId,
            String reason,
            VitrineUserPrincipal principal
    ) {
        TerminalPaymentReversalStartService.StartResult
                started =
                startService.start(
                        paymentId,
                        reason,
                        principal.getId()
                );

        ProviderPaymentResult providerResult;

        try {
            providerResult =
                    queueService.waitForResult(
                            started.commandId()
                    );

        } catch (BusinessException exception) {
            if (exception.getCode().equals(
                    "PAYMENT_TERMINAL_COMMAND_EXPIRED"
            )) {
                throw pendingConfirmation();
            }

            throw exception;
        }

        if (providerResult.status()
                == ProviderPaymentStatus.APPROVED) {

            return buildResponse(started);
        }

        if (providerResult.status()
                == ProviderPaymentStatus.CANCELLED
                || providerResult.status()
                == ProviderPaymentStatus.DECLINED) {

            throw new BusinessException(
                    "PAYMENT_REVERSAL_NOT_COMPLETED",
                    "O estorno foi cancelado ou nao aprovado na maquininha."
            );
        }

        throw pendingConfirmation();
    }

    private TerminalPaymentReversalResponse buildResponse(
            TerminalPaymentReversalStartService.StartResult
                    started
    ) {
        PaymentEntity payment =
                paymentRepository
                        .findById(started.paymentId())
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "PAYMENT_NOT_FOUND",
                                        "Pagamento nao encontrado apos o estorno."
                                )
                        );

        if (payment.getStatus()
                != PaymentStatus.REVERSED) {

            throw new BusinessException(
                    "PAYMENT_REVERSAL_INCONSISTENT",
                    "A maquininha aprovou o estorno, mas o pagamento nao foi finalizado corretamente."
            );
        }

        PaymentTerminalTransactionEntity transaction =
                transactionRepository
                        .findById(
                                started.transactionId()
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "PAYMENT_TERMINAL_TRANSACTION_NOT_FOUND",
                                        "Transacao original nao encontrada."
                                )
                        );

        return new TerminalPaymentReversalResponse(
                started.commandId(),
                PaymentResponse.from(payment),
                PaymentTerminalTransactionResponse.from(
                        transaction
                )
        );
    }

    private BusinessException pendingConfirmation() {
        return new BusinessException(
                "PAYMENT_REVERSAL_PENDING_CONFIRMATION",
                "Nao foi possivel confirmar o resultado do estorno. Verifique a maquininha e a conta PagBank antes de qualquer nova tentativa."
        );
    }
}
