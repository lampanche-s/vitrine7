package br.com.vitrine7.payment.terminal.adapter.simulated;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.payment.terminal.adapter.PaymentProviderAdapter;
import br.com.vitrine7.payment.terminal.adapter.ProviderConfiguration;
import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentCommand;
import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentResult;
import br.com.vitrine7.payment.terminal.adapter.ProviderQueryCommand;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import br.com.vitrine7.payment.terminal.provider.ProviderCapabilities;
import br.com.vitrine7.payment.terminal.provider.ProviderPaymentStatus;
import br.com.vitrine7.payment.terminal.entity.TerminalSimulationOutcome;
import br.com.vitrine7.payment.terminal.bridge.TerminalBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SimulatedPaymentTerminalAdapter
        implements PaymentProviderAdapter {

    private final TerminalBridgeService terminalBridgeService;

    @Override
    public PaymentProviderCode providerCode() {
        return PaymentProviderCode.SIMULATOR;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return ProviderCapabilities.simulator();
    }

    @Override
    public void validateConfiguration(
            ProviderConfiguration configuration
    ) {
        simulatedOutcome(configuration);
        transport(configuration);
    }

    @Override
    public ProviderPaymentResult initiatePayment(
            ProviderPaymentCommand command
    ) {
        if (transport(command.configuration()).equals("DEVICE_BRIDGE")) {
            return terminalBridgeService.initiatePayment(command);
        }
        String providerTransactionId =
                "SIM-"
                        + UUID.randomUUID();

        TerminalSimulationOutcome outcome =
                simulatedOutcome(command.configuration());

        if (outcome == TerminalSimulationOutcome.APPROVED) {

            return new ProviderPaymentResult(
                    ProviderPaymentStatus.APPROVED,
                    providerTransactionId,
                    providerTransactionId,
                    null,
                    null,
                    Map.of(
                            "simulated", true,
                            "outcome", outcome.name()
                    ),
                    OffsetDateTime.now()
            );
        }

        return new ProviderPaymentResult(
                ProviderPaymentStatus.DECLINED,
                providerTransactionId,
                providerTransactionId,
                "51",
                "Transacao recusada pelo terminal simulado.",
                Map.of(
                        "simulated", true,
                        "outcome", outcome.name()
                ),
                OffsetDateTime.now()
        );
    }

    @Override
    public ProviderPaymentResult queryPayment(
            ProviderQueryCommand command
    ) {
        return new ProviderPaymentResult(
                ProviderPaymentStatus.UNKNOWN,
                command.providerReference(),
                command.providerReference(),
                null,
                null,
                Map.of("simulated", true),
                OffsetDateTime.now()
        );
    }

    private TerminalSimulationOutcome simulatedOutcome(
            ProviderConfiguration configuration
    ) {
        String outcome = "APPROVED";

        if (configuration.publicConfiguration() != null
                && configuration.publicConfiguration()
                .has("simulatedOutcome")) {
            outcome = configuration.publicConfiguration()
                    .get("simulatedOutcome")
                    .asText();
        }

        try {
            return TerminalSimulationOutcome.valueOf(outcome);
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_CONFIGURATION_INVALID",
                    "Resultado configurado para o simulador e invalido."
            );
        }
    }

    private String transport(ProviderConfiguration configuration) {
        String transport = "IN_PROCESS";
        if (configuration.publicConfiguration() != null
                && configuration.publicConfiguration().has("transport")) {
            transport = configuration.publicConfiguration().get("transport").asText();
        }
        if (!transport.equals("IN_PROCESS") && !transport.equals("DEVICE_BRIDGE")) {
            throw new BusinessException("PAYMENT_PROVIDER_CONFIGURATION_INVALID",
                    "Transporte configurado para o simulador e invalido.");
        }
        return transport;
    }
}
