package br.com.vitrine7.payment.terminal.adapter;

import br.com.vitrine7.payment.core.entity.PaymentMethod;

import java.util.UUID;

public record ProviderPaymentCommand(
        UUID checkoutId,
        UUID paymentId,
        PaymentMethod method,
        long amountCents,
        ProviderConfiguration configuration
) {
}
