package br.com.vitrine7.payment.core.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.payment.core.entity.PaymentEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentIdempotencyValidator {

    public void validateReplay(
            PaymentEntity existing,
            PaymentExecutionCommand command
    ) {
        validateReplay(
                existing,
                command.checkoutId(),
                command.requestFingerprint(),
                command.actorUserId()
        );
    }

    public void validateReplay(
            PaymentEntity existing,
            UUID checkoutId,
            String requestFingerprint,
            Long actorUserId
    ) {
        if (!existing.getCreatedByUserId()
                .equals(actorUserId)) {

            throw new BusinessException(
                    "PAYMENT_IDEMPOTENCY_KEY_ALREADY_USED",
                    "A chave de idempotencia do pagamento ja foi utilizada."
            );
        }

        if (!existing.getCheckoutSessionId()
                .equals(checkoutId)) {

            throw differentRequest();
        }

        if (!existing.getRequestFingerprint()
                .equals(requestFingerprint)) {

            throw differentRequest();
        }
    }

    private BusinessException differentRequest() {
        return new BusinessException(
                "PAYMENT_IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST",
                "A chave de idempotencia do pagamento foi reutilizada com uma requisicao diferente."
        );
    }
}
