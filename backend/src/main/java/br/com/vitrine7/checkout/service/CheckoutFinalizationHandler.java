package br.com.vitrine7.checkout.service;

import br.com.vitrine7.checkout.entity.CheckoutOperationType;
import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;

import java.time.OffsetDateTime;

public interface CheckoutFinalizationHandler {

    boolean supports(
            CheckoutOperationType operationType
    );

    int finalizeOperation(
            CheckoutSessionEntity checkout,
            Long actorUserId,
            OffsetDateTime finalizedAt
    );
}
