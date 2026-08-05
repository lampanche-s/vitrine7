package br.com.vitrine7.payment.terminal.dto;

import br.com.vitrine7.payment.terminal.entity.PaymentTerminalProvider;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalSettingsEntity;

import java.time.OffsetDateTime;
import java.util.List;

public record PaymentTerminalSettingsResponse(
        String provider,
        String mode,
        boolean active,
        String simulatedOutcome,
        List<String> supportedProviders,
        OffsetDateTime updatedAt
) {
    private static final List<String> SUPPORTED_PROVIDERS =
            List.of("SIMULATOR", "PAGBANK");

    public static PaymentTerminalSettingsResponse from(
            PaymentTerminalSettingsEntity settings
    ) {
        return new PaymentTerminalSettingsResponse(
                settings.getProvider() == null
                        ? null
                        : settings.getProvider().name(),
                settings.getMode().name(),
                settings.isActive(),
                settings.getSimulatedOutcome().name(),
                SUPPORTED_PROVIDERS,
                settings.getUpdatedAt()
        );
    }
}
