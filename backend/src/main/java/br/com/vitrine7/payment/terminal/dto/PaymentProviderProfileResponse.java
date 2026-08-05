package br.com.vitrine7.payment.terminal.dto;

import br.com.vitrine7.payment.terminal.provider.PaymentProviderProfile;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PaymentProviderProfileResponse(
        UUID id,
        String providerCode,
        String displayName,
        String environment,
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
    public static PaymentProviderProfileResponse from(
            PaymentProviderProfile profile
    ) {
        return new PaymentProviderProfileResponse(
                profile.id(),
                profile.providerCode().name(),
                profile.displayName(),
                profile.environment().name(),
                profile.enabled(),
                profile.active(),
                profile.merchantReference(),
                profile.terminalReference(),
                profile.publicConfiguration(),
                profile.credentialsConfigured(),
                profile.credentialKeys(),
                profile.configurationVersion(),
                profile.createdAt(),
                profile.updatedAt(),
                profile.createdByUserId(),
                profile.updatedByUserId(),
                profile.version()
        );
    }
}
