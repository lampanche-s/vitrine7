package br.com.vitrine7.payment.terminal.adapter;

import java.util.UUID;

public record ProviderCancelCommand(
        UUID transactionId,
        String providerReference,
        ProviderConfiguration configuration
) {
}
