package br.com.vitrine7.payment.terminal.dto;

import br.com.vitrine7.payment.terminal.provider.PaymentProviderProfile;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentProviderPublicProfileResponse(
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
        long configurationVersion,
        OffsetDateTime updatedAt,
        long version
) {
    public static PaymentProviderPublicProfileResponse from(
            PaymentProviderProfile profile
    ) {
        return new PaymentProviderPublicProfileResponse(
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
                profile.configurationVersion(),
                profile.updatedAt(),
                profile.version()
        );
    }
}
