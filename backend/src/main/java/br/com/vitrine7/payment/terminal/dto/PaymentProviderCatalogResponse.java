package br.com.vitrine7.payment.terminal.dto;

import br.com.vitrine7.payment.terminal.provider.PaymentProviderCatalog;
import br.com.vitrine7.payment.terminal.provider.ProviderCapabilities;

public record PaymentProviderCatalogResponse(
        String code,
        String displayName,
        String implementationStatus,
        boolean adapterAvailable,
        boolean canBeActivated,
        boolean active,
        boolean configured,
        String environment,
        ProviderCapabilities capabilities
) {
    public static PaymentProviderCatalogResponse from(
            PaymentProviderCatalog.ProviderDescriptor descriptor,
            boolean adapterAvailable,
            boolean canBeActivated,
            boolean active,
            boolean configured,
            String environment
    ) {
        return new PaymentProviderCatalogResponse(
                descriptor.code().name(),
                descriptor.displayName(),
                descriptor.implementationStatus().name(),
                adapterAvailable,
                canBeActivated,
                active,
                configured,
                environment,
                descriptor.capabilities()
        );
    }
}
