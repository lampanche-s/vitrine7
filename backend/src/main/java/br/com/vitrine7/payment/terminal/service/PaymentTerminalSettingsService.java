package br.com.vitrine7.payment.terminal.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.payment.terminal.dto.PaymentTerminalSettingsResponse;
import br.com.vitrine7.payment.terminal.dto.UpdatePaymentTerminalSettingsRequest;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalMode;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalProvider;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalSettingsEntity;
import br.com.vitrine7.payment.terminal.entity.TerminalSimulationOutcome;
import br.com.vitrine7.payment.terminal.dto.PaymentProviderProfileRequest;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderEnvironment;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderProfile;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderProfileRepository;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderProfileService;
import br.com.vitrine7.payment.terminal.repository.PaymentTerminalSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentTerminalSettingsService {

    private final PaymentTerminalSettingsRepository settingsRepository;
    private final PaymentProviderProfileRepository profileRepository;
    private final PaymentProviderProfileService profileService;

    @Transactional(readOnly = true)
    public PaymentTerminalSettingsResponse getCurrent() {
        return PaymentTerminalSettingsResponse.from(
                getSettings()
        );
    }

    @Transactional
    public PaymentTerminalSettingsResponse update(
            UpdatePaymentTerminalSettingsRequest request
    ) {
        PaymentTerminalSettingsEntity settings =
                getSettings();

        settings.updateSimulatedConfiguration(
                PaymentTerminalProvider.SIMULATOR,
                request.active(),
                request.simulatedOutcome()
        );

        PaymentProviderProfile simulator =
                profileRepository
                        .findByProviderCode(PaymentProviderCode.SIMULATOR)
                        .orElseThrow(() -> new NotFoundException(
                                "PAYMENT_PROVIDER_PROFILE_NOT_FOUND",
                                "Perfil do simulador nao encontrado."
                        ));

        profileService.update(
                simulator.id(),
                new PaymentProviderProfileRequest(
                        PaymentProviderCode.SIMULATOR,
                        simulator.displayName(),
                        PaymentProviderEnvironment.LOCAL,
                        request.active(),
                        null,
                        simulator.merchantReference(),
                        simulator.terminalReference(),
                        java.util.Map.of(
                                "simulatedOutcome",
                                request.simulatedOutcome().name()
                        ),
                        null,
                        simulator.version()
                ),
                null
        );

        if (Boolean.TRUE.equals(request.active())
                && !simulator.active()) {
            profileService.activate(
                    simulator.id(),
                    null
            );
        }


        return PaymentTerminalSettingsResponse.from(
                settings
        );
    }

    @Transactional(readOnly = true)
    public ActiveTerminalConfiguration
    requireActiveConfiguration() {
        PaymentTerminalSettingsEntity settings =
                getSettings();

        if (!settings.isActive()) {
            throw new BusinessException(
                    "PAYMENT_TERMINAL_INTEGRATION_DISABLED",
                    "A integracao com a maquininha esta desativada."
            );
        }

        if (settings.getProvider() == null) {
            throw new BusinessException(
                    "PAYMENT_TERMINAL_PROVIDER_REQUIRED",
                    "Nenhum provedor de maquininha foi selecionado."
            );
        }

        if (settings.getMode()
                == PaymentTerminalMode.REAL) {

            throw new BusinessException(
                    "REAL_PAYMENT_TERMINAL_NOT_AVAILABLE",
                    "A integracao real com a maquininha ainda nao esta disponivel."
            );
        }

        return new ActiveTerminalConfiguration(
                settings.getProvider(),
                settings.getMode(),
                settings.getSimulatedOutcome()
        );
    }

    private PaymentTerminalSettingsEntity getSettings() {
        return settingsRepository
                .findById(
                        PaymentTerminalSettingsEntity.SINGLETON_ID
                )
                .orElseThrow(() -> new NotFoundException(
                        "PAYMENT_TERMINAL_SETTINGS_NOT_FOUND",
                        "As configuracoes da maquininha nao foram encontradas."
                ));
    }

    public record ActiveTerminalConfiguration(
            PaymentTerminalProvider provider,
            PaymentTerminalMode mode,
            TerminalSimulationOutcome simulatedOutcome
    ) {
    }
}
