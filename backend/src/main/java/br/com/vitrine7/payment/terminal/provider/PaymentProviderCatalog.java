package br.com.vitrine7.payment.terminal.provider;

import br.com.vitrine7.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PaymentProviderCatalog {

    private static final List<PaymentProviderCode> ADMINISTRATIVE_PROVIDERS =
            List.of(
                    PaymentProviderCode.SIMULATOR,
                    PaymentProviderCode.PAGBANK
            );

    public List<ProviderDescriptor> list() {
        return ADMINISTRATIVE_PROVIDERS.stream()
                .map(this::descriptor)
                .toList();
    }

    public ProviderDescriptor require(PaymentProviderCode code) {
        return descriptor(code);
    }

    public Optional<ProviderDescriptor> find(String code) {
        try {
            PaymentProviderCode providerCode =
                    PaymentProviderCode.valueOf(code);

            if (!isAdministrativelySelectable(providerCode)) {
                return Optional.empty();
            }

            return Optional.of(descriptor(providerCode));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public boolean isAdministrativelySelectable(
            PaymentProviderCode code
    ) {
        return ADMINISTRATIVE_PROVIDERS.contains(code);
    }

    private ProviderDescriptor descriptor(
            PaymentProviderCode code
    ) {
        return switch (code) {
            case SIMULATOR -> new ProviderDescriptor(
                    code,
                    "Terminal Simulado",
                    PaymentProviderImplementationStatus.AVAILABLE,
                    ProviderCapabilities.simulator()
            );
            case PAGBANK -> new ProviderDescriptor(
                    code,
                    "PagBank",
                    PaymentProviderImplementationStatus.AVAILABLE,
                    ProviderCapabilities.pagBankLocal()
            );
            default -> throw new BusinessException(
                    "PAYMENT_PROVIDER_NOT_AVAILABLE",
                    "Provider de pagamento nao esta disponivel no catalogo atual."
            );
        };
    }

    public record ProviderDescriptor(
            PaymentProviderCode code,
            String displayName,
            PaymentProviderImplementationStatus implementationStatus,
            ProviderCapabilities capabilities
    ) {
    }
}
