package br.com.vitrine7.payment.terminal.adapter;

import br.com.vitrine7.payment.terminal.adapter.pagbank.PagBankPaymentTerminalAdapter;
import br.com.vitrine7.payment.terminal.adapter.simulated.SimulatedPaymentTerminalAdapter;
import br.com.vitrine7.payment.terminal.bridge.TerminalBridgeService;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PaymentTerminalAdapterRegistryTest {

    @Test
    void findsPagbankAdapterWithoutBreakingSimulator() {
        TerminalBridgeService bridge = mock(TerminalBridgeService.class);
        SimulatedPaymentTerminalAdapter simulatorAdapter =
                new SimulatedPaymentTerminalAdapter(bridge);
        PagBankPaymentTerminalAdapter pagBankAdapter =
                new PagBankPaymentTerminalAdapter(bridge);
        PaymentTerminalAdapterRegistry registry =
                new PaymentTerminalAdapterRegistry(
                        List.of(simulatorAdapter, pagBankAdapter)
                );

        assertSame(
                pagBankAdapter,
                registry.getAdapter(PaymentProviderCode.PAGBANK)
        );
        assertSame(
                simulatorAdapter,
                registry.getAdapter(PaymentProviderCode.SIMULATOR)
        );
        assertTrue(registry.isAdapterAvailable(PaymentProviderCode.PAGBANK));
        assertTrue(registry.isAdapterAvailable(PaymentProviderCode.SIMULATOR));
    }
}
