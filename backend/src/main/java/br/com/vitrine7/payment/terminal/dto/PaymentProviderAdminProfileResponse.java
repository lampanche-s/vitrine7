package br.com.vitrine7.payment.terminal.dto;

import br.com.vitrine7.payment.terminal.provider.PaymentProviderCatalog;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderProfile;
import br.com.vitrine7.payment.terminal.provider.ProviderCapabilities;

public record PaymentProviderAdminProfileResponse(
        String code,
        String displayName,
        String implementationStatus,
        boolean adapterAvailable,
        boolean canBeActivated,
        boolean configured,
        PaymentProviderPublicProfileResponse profile,
        ProviderCapabilities capabilities
) {
    public static PaymentProviderAdminProfileResponse from(
            PaymentProviderCatalog.ProviderDescriptor descriptor,
            boolean adapterAvailable,
            boolean canBeActivated,
            PaymentProviderProfile profile
    ) {
        return new PaymentProviderAdminProfileResponse(
                descriptor.code().name(),
                descriptor.displayName(),
                descriptor.implementationStatus().name(),
                adapterAvailable,
                canBeActivated,
                profile != null,
                profile == null
                        ? null
                        : PaymentProviderPublicProfileResponse.from(profile),
                descriptor.capabilities()
        );
    }
}
