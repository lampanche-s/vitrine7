package br.com.vitrine7.payment.terminal.adapter.pagbank;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.payment.core.entity.PaymentMethod;
import br.com.vitrine7.payment.terminal.adapter.ProviderConfiguration;
import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentCommand;
import br.com.vitrine7.payment.terminal.adapter.ProviderPaymentResult;
import br.com.vitrine7.payment.terminal.bridge.TerminalBridgeService;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderEnvironment;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderImplementationStatus;
import br.com.vitrine7.payment.terminal.provider.ProviderCapabilities;
import br.com.vitrine7.payment.terminal.provider.ProviderPaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PagBankPaymentTerminalAdapterTest {

    private final TerminalBridgeService terminalBridgeService =
            mock(TerminalBridgeService.class);

    private final PagBankPaymentTerminalAdapter adapter =
            new PagBankPaymentTerminalAdapter(terminalBridgeService);

    @Test
    void exposesLocalPhysicalCapabilitiesAndAcceptsOnlyPagbank() {
        assertEquals(
                PaymentProviderCode.PAGBANK,
                adapter.providerCode()
        );

        ProviderCapabilities capabilities =
                adapter.capabilities();

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

        adapter.validateConfiguration(
                configuration(PaymentProviderCode.PAGBANK)
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adapter.validateConfiguration(
                        configuration(
                                PaymentProviderCode.SIMULATOR
                        )
                )
        );

        assertEquals(
                "PAYMENT_PROVIDER_CONFIGURATION_INVALID",
                exception.getCode()
        );
    }

    @Test
    void delegatesCommandToTerminalBridge() {
        ProviderPaymentCommand command = command();
        ProviderPaymentResult expected = result(
                ProviderPaymentStatus.APPROVED,
                "PAG-1",
                null,
                null
        );

        when(terminalBridgeService.initiatePayment(command))
                .thenReturn(expected);

        ProviderPaymentResult actual =
                adapter.initiatePayment(command);

        assertSame(expected, actual);
        verify(terminalBridgeService)
                .initiatePayment(command);
    }

    @Test
    void propagatesApprovedResultFromBridge() {
        ProviderPaymentCommand command = command();
        ProviderPaymentResult expected = result(
                ProviderPaymentStatus.APPROVED,
                "PAG-APPROVED",
                null,
                null
        );

        when(terminalBridgeService.initiatePayment(command))
                .thenReturn(expected);

        ProviderPaymentResult actual =
                adapter.initiatePayment(command);

        assertEquals(
                ProviderPaymentStatus.APPROVED,
                actual.status()
        );
        assertEquals(
                "PAG-APPROVED",
                actual.providerReference()
        );
    }

    @Test
    void propagatesDeclinedResultFromBridge() {
        ProviderPaymentCommand command = command();
        ProviderPaymentResult expected = result(
                ProviderPaymentStatus.DECLINED,
                "PAG-DECLINED",
                "51",
                "Transacao recusada."
        );

        when(terminalBridgeService.initiatePayment(command))
                .thenReturn(expected);

        ProviderPaymentResult actual =
                adapter.initiatePayment(command);

        assertEquals(
                ProviderPaymentStatus.DECLINED,
                actual.status()
        );
        assertEquals("51", actual.failureCode());
        assertEquals(
                "Transacao recusada.",
                actual.failureMessage()
        );
    }

    @Test
    void propagatesErrorResultFromBridge() {
        ProviderPaymentCommand command = command();
        ProviderPaymentResult expected = result(
                ProviderPaymentStatus.ERROR,
                null,
                "PAYMENT_TERMINAL_RESULT_ERROR",
                "Falha de comunicacao."
        );

        when(terminalBridgeService.initiatePayment(command))
                .thenReturn(expected);

        ProviderPaymentResult actual =
                adapter.initiatePayment(command);

        assertEquals(
                ProviderPaymentStatus.ERROR,
                actual.status()
        );
        assertEquals(
                "PAYMENT_TERMINAL_RESULT_ERROR",
                actual.failureCode()
        );
        assertEquals(
                "Falha de comunicacao.",
                actual.failureMessage()
        );
    }

    @Test
    void propagatesTimeoutFromBridge() {
        ProviderPaymentCommand command = command();

        when(terminalBridgeService.initiatePayment(command))
                .thenThrow(new BusinessException(
                        "PAYMENT_TERMINAL_COMMAND_EXPIRED",
                        "Tempo limite do comando excedido."
                ));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adapter.initiatePayment(command)
        );

        assertEquals(
                "PAYMENT_TERMINAL_COMMAND_EXPIRED",
                exception.getCode()
        );
    }

    private ProviderPaymentCommand command() {
        return new ProviderPaymentCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentMethod.CREDIT_CARD,
                1290L,
                configuration(PaymentProviderCode.PAGBANK)
        );
    }

    private ProviderConfiguration configuration(
            PaymentProviderCode providerCode
    ) {
        return new ProviderConfiguration(
                UUID.randomUUID(),
                providerCode,
                PaymentProviderEnvironment.LOCAL,
                1L,
                null,
                false
        );
    }

    private ProviderPaymentResult result(
            ProviderPaymentStatus status,
            String providerReference,
            String failureCode,
            String failureMessage
    ) {
        return new ProviderPaymentResult(
                status,
                providerReference,
                providerReference,
                failureCode,
                failureMessage,
                Map.of(),
                OffsetDateTime.now()
        );
    }
}
