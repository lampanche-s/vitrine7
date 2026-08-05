package br.com.vitrine7.payment.terminal.adapter.simulated;

import br.com.vitrine7.payment.core.entity.PaymentMethod;
import br.com.vitrine7.payment.terminal.adapter.ProviderConfiguration;
import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentCommand;
import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentResult;
import br.com.vitrine7.payment.terminal.bridge.TerminalBridgeService;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderEnvironment;
import br.com.vitrine7.payment.terminal.provider.ProviderPaymentStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SimulatedPaymentTerminalAdapterTest {

    @Test
    void simulatorStillApprovesInProcessPaymentsByDefault() {
        SimulatedPaymentTerminalAdapter adapter =
                new SimulatedPaymentTerminalAdapter(
                        mock(TerminalBridgeService.class)
                );
        ProviderPaymentCommand command = new ProviderPaymentCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentMethod.CREDIT_CARD,
                1290L,
                new ProviderConfiguration(
                        UUID.randomUUID(),
                        PaymentProviderCode.SIMULATOR,
                        PaymentProviderEnvironment.LOCAL,
                        1L,
                        null,
                        false
                )
        );

        ProviderPaymentResult result = adapter.initiatePayment(command);

        assertEquals(ProviderPaymentStatus.APPROVED, result.status());
        assertTrue((Boolean) result.metadata().get("simulated"));
    }
}
