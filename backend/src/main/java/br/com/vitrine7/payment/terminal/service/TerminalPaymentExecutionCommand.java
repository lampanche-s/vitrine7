package br.com.vitrine7.payment.terminal.service;

import br.com.vitrine7.payment.core.entity.PaymentMethod;

import java.util.UUID;

public record TerminalPaymentExecutionCommand(
        UUID checkoutId,
        UUID idempotencyKey,
        String requestFingerprint,
        PaymentMethod method,
        Long actorUserId
) {
}
