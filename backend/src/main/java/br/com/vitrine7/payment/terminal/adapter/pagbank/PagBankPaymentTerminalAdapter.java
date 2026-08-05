package br.com.vitrine7.payment.terminal.adapter.pagbank;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.payment.terminal.adapter.PaymentProviderAdapter;
import br.com.vitrine7.payment.terminal.adapter.ProviderConfiguration;
import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentCommand;
import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentResult;
import br.com.vitrine7.payment.terminal.adapter.ProviderQueryCommand;
import br.com.vitrine7.payment.terminal.bridge.TerminalBridgeService;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import br.com.vitrine7.payment.terminal.provider.ProviderCapabilities;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PagBankPaymentTerminalAdapter
        implements PaymentProviderAdapter {

    private final TerminalBridgeService terminalBridgeService;

    @Override
    public PaymentProviderCode providerCode() {
        return PaymentProviderCode.PAGBANK;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return ProviderCapabilities.pagBankLocal();
    }

    @Override
    public void validateConfiguration(
            ProviderConfiguration configuration
    ) {
        if (configuration == null
                || configuration.providerCode() != PaymentProviderCode.PAGBANK) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_CONFIGURATION_INVALID",
                    "Configuracao nao pertence ao provider PagBank."
            );
        }
    }

    @Override
    public ProviderPaymentResult initiatePayment(
            ProviderPaymentCommand command
    ) {
        validateConfiguration(command.configuration());
        return terminalBridgeService.initiatePayment(command);
    }

    @Override
    public ProviderPaymentResult queryPayment(
            ProviderQueryCommand command
    ) {
        throw new BusinessException(
                "PAYMENT_PROVIDER_OPERATION_NOT_AVAILABLE",
                "Consulta PagBank pelo adapter ainda nao esta disponivel."
        );
    }

}
