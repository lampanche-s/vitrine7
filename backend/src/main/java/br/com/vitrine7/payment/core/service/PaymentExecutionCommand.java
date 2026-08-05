package br.com.vitrine7.payment.core.service;

import br.com.vitrine7.payment.core.entity.PaymentMethod;
import br.com.vitrine7.payment.core.entity.PaymentProcessingMode;

import java.util.UUID;

public record PaymentExecutionCommand(
        UUID checkoutId,
        UUID idempotencyKey,
        String requestFingerprint,
        PaymentMethod method,
        PaymentProcessingMode processingMode,
        Long cashReceivedCents,
        String manualReason,
        Long actorUserId
) {
}
