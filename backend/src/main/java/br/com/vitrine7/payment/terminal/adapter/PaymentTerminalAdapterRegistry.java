package br.com.vitrine7.payment.terminal.adapter;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PaymentTerminalAdapterRegistry {

    private final Map<PaymentProviderCode, PaymentProviderAdapter> adapters;

    public PaymentTerminalAdapterRegistry(
            List<PaymentProviderAdapter> adapters
    ) {
        Map<PaymentProviderCode, PaymentProviderAdapter> indexed =
                new LinkedHashMap<>();

        for (PaymentProviderAdapter adapter : adapters) {
            PaymentProviderAdapter previous =
                    indexed.putIfAbsent(
                            adapter.providerCode(),
                            adapter
                    );
            if (previous != null) {
                throw new IllegalStateException(
                        "Adapter duplicado para provider "
                                + adapter.providerCode()
                );
            }
        }

        if (!indexed.containsKey(PaymentProviderCode.SIMULATOR)) {
            throw new IllegalStateException(
                    "Adapter do simulador nao registrado."
            );
        }

        this.adapters = Map.copyOf(indexed);
    }

    public PaymentProviderAdapter getAdapter(
            PaymentProviderCode providerCode
    ) {
        return findAdapter(providerCode)
                .orElseThrow(() -> new BusinessException(
                        "PAYMENT_PROVIDER_ADAPTER_NOT_AVAILABLE",
                        "Nao existe adapter disponivel para o provider selecionado."
                ));
    }

    public Optional<PaymentProviderAdapter> findAdapter(
            PaymentProviderCode providerCode
    ) {
        return Optional.ofNullable(
                adapters.get(providerCode)
        );
    }

    public boolean isAdapterAvailable(
            PaymentProviderCode providerCode
    ) {
        return adapters.containsKey(providerCode);
    }
}
