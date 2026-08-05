package br.com.vitrine7.payment.terminal.adapter;

import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import br.com.vitrine7.payment.terminal.provider.ProviderCapabilities;

public interface PaymentProviderAdapter {

    PaymentProviderCode providerCode();

    ProviderCapabilities capabilities();

    void validateConfiguration(
            ProviderConfiguration configuration
    );

    ProviderPaymentResult initiatePayment(
            ProviderPaymentCommand command
    );

    ProviderPaymentResult queryPayment(
            ProviderQueryCommand command
    );
}
