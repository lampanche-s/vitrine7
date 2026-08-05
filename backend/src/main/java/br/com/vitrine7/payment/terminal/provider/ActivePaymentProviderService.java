package br.com.vitrine7.payment.terminal.provider;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.exception.NotFoundException;
import br.com.vitrine7.payment.terminal.adapter.PaymentProviderAdapter;
import br.com.vitrine7.payment.terminal.adapter.PaymentTerminalAdapterRegistry;
import br.com.vitrine7.payment.terminal.adapter.ProviderConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivePaymentProviderService {

    private final PaymentProviderProfileRepository profileRepository;
    private final PaymentTerminalAdapterRegistry adapterRegistry;

    @Transactional(readOnly = true)
    public ActiveProvider requireActiveProvider() {
        PaymentProviderProfile profile =
                profileRepository.findActive()
                        .orElseThrow(() -> new NotFoundException(
                                "PAYMENT_PROVIDER_PROFILE_NOT_FOUND",
                                "Nenhum perfil de provider ativo foi encontrado."
                        ));

        if (!profile.enabled()) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_PROFILE_DISABLED",
                    "O perfil ativo de provider esta desabilitado."
            );
        }

        PaymentProviderAdapter adapter =
                adapterRegistry.getAdapter(
                        profile.providerCode()
                );

        ProviderConfiguration configuration =
                new ProviderConfiguration(
                        profile.id(),
                        profile.providerCode(),
                        profile.environment(),
                        profile.configurationVersion(),
                        profile.publicConfiguration(),
                        profile.credentialsConfigured()
                );

        return new ActiveProvider(
                profile,
                configuration,
                adapter
        );
    }

    public record ActiveProvider(
            PaymentProviderProfile profile,
            ProviderConfiguration configuration,
            PaymentProviderAdapter adapter
    ) {
    }
}
