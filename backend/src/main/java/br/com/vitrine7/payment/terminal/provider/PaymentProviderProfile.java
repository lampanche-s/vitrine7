package br.com.vitrine7.payment.terminal.provider;

import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PaymentProviderProfile(
        UUID id,
        PaymentProviderCode providerCode,
        String displayName,
        PaymentProviderEnvironment environment,
        boolean enabled,
        boolean active,
        String merchantReference,
        String terminalReference,
        JsonNode publicConfiguration,
        boolean credentialsConfigured,
        List<String> credentialKeys,
        long configurationVersion,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long createdByUserId,
        Long updatedByUserId,
        long version
) {
}
