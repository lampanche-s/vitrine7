package br.com.vitrine7.payment.terminal.adapter;

import java.util.UUID;

public record ProviderQueryCommand(
        UUID transactionId,
        String providerReference,
        ProviderConfiguration configuration
) {
}
