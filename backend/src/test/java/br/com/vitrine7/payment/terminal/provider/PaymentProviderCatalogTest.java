package br.com.vitrine7.payment.terminal.provider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentProviderCatalogTest {

    private final PaymentProviderCatalog catalog =
            new PaymentProviderCatalog();

    @Test
    void listReturnsOnlyCurrentAdministrativeProviders() {
        List<String> codes = catalog.list()
                .stream()
                .map(descriptor -> descriptor.code().name())
                .toList();

        assertEquals(
                List.of("SIMULATOR", "PAGBANK"),
                codes
        );
    }

    @Test
    void pagbankIsAvailableForLocalPhysicalIntegration() {
        PaymentProviderCatalog.ProviderDescriptor descriptor =
                catalog.require(PaymentProviderCode.PAGBANK);

        ProviderCapabilities capabilities =
                descriptor.capabilities();

        assertEquals(
                PaymentProviderImplementationStatus.AVAILABLE,
                descriptor.implementationStatus()
        );
        assertEquals(
                PaymentProviderImplementationStatus.AVAILABLE,
                capabilities.implementationStatus()
        );

        assertTrue(capabilities.credit());
        assertTrue(capabilities.debit());
        assertFalse(capabilities.pix());
        assertFalse(capabilities.cancellation());
        assertFalse(capabilities.query());
        assertTrue(capabilities.synchronous());
        assertFalse(capabilities.asynchronous());
        assertTrue(capabilities.requiresPhysicalTerminal());
        assertFalse(capabilities.requiresWebhook());
    }
}
