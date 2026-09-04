package br.com.vitrine7.payment.core.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.payment.core.dto.PaymentResponse;
import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.entity.PaymentStatus;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PaymentReversalService {

    private final PaymentRepository paymentRepository;
    private final Clock clock;

    public PaymentReversalService(
            PaymentRepository paymentRepository,
            Clock clock
    ) {
        this.paymentRepository = paymentRepository;
        this.clock = clock;
    }

    @Transactional
    public PaymentResponse markReversed(
            UUID paymentId,
            String reason,
            Long actorUserId
    ) {
        PaymentEntity payment =
                paymentRepository
                        .findByIdForUpdate(paymentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "PAYMENT_NOT_FOUND",
                                        "Pagamento nao encontrado."
                                )
                        );

        if (payment.getStatus() == PaymentStatus.REVERSED) {
            throw new BusinessException(
                    "PAYMENT_ALREADY_REVERSED",
                    "Este pagamento ja foi marcado como estornado."
            );
        }

        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new BusinessException(
                    "PAYMENT_CANNOT_BE_REVERSED",
                    "Somente um pagamento aprovado pode ser marcado como estornado."
            );
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        var approvedPayments = paymentRepository
                .findApprovedByCheckoutForUpdate(
                        payment.getCheckoutSessionId()
                );

        if (approvedPayments.isEmpty()) {
            throw new BusinessException(
                    "PAYMENT_CANNOT_BE_REVERSED",
                    "Não há pagamentos aprovados para estornar."
            );
        }

        for (PaymentEntity approvedPayment : approvedPayments) {
            approvedPayment.markReversed(
                    actorUserId,
                    reason,
                    now
            );
        }

        paymentRepository.flush();

        return PaymentResponse.from(
                approvedPayments.stream()
                        .filter(item -> item.getId().equals(paymentId))
                        .findFirst()
                        .orElse(approvedPayments.get(0))
        );
    }
}
