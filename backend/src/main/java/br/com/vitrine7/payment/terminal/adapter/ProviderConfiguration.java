package br.com.vitrine7.payment.terminal.adapter;

import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderEnvironment;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

public record ProviderConfiguration(
        UUID profileId,
        PaymentProviderCode providerCode,
        PaymentProviderEnvironment environment,
        long configurationVersion,
        JsonNode publicConfiguration,
        boolean credentialsConfigured
) {
}
