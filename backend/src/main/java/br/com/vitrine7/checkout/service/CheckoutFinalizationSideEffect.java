package br.com.vitrine7.checkout.service;

import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;

import java.time.OffsetDateTime;

public interface CheckoutFinalizationSideEffect {

    void afterCheckoutFinalized(
            CheckoutSessionEntity checkout,
            Long actorUserId,
            OffsetDateTime finalizedAt
    );
}
