package br.com.vitrine7.checkout.service;

import br.com.vitrine7.checkout.entity.CheckoutOperationType;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface CheckoutResourceReleaseHandler {

    boolean supports(CheckoutOperationType operationType);

    void release(
            UUID checkoutId,
            String reason,
            OffsetDateTime releasedAt
    );
}
