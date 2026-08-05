package br.com.vitrine7.payment.terminal.dto;

import java.util.UUID;

public record PaymentProviderStatusResponse(
        UUID activeProfileId,
        String providerCode,
        String displayName,
        String implementationStatus,
        String environment,
        boolean enabled,
        long configurationVersion,
        boolean adapterAvailable,
        PaymentProviderPublicProfileResponse profile
) {
}
