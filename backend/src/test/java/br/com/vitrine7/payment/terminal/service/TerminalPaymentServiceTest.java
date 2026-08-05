package br.com.vitrine7.payment.terminal.service;

import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.checkout.repository.CheckoutSessionRepository;
import br.com.vitrine7.checkout.service.CheckoutFinalizationService;
import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.common.idempotency.IdempotencyFingerprintService;
import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.entity.PaymentMethod;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import br.com.vitrine7.payment.core.service.PaymentIdempotencyValidator;
import br.com.vitrine7.payment.terminal.adapter.PaymentProviderAdapter;
import br.com.vitrine7.payment.terminal.adapter.ProviderConfiguration;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalTransactionEntity;
import br.com.vitrine7.payment.terminal.provider.ActivePaymentProviderService;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderProfile;
import br.com.vitrine7.payment.terminal.repository.PaymentTerminalTransactionRepository;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TerminalPaymentServiceTest {

    private final TerminalPaymentService service =
            new TerminalPaymentService(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

    @Test
    void terminalPaymentsAcceptOnlyCreditAndDebitForNewCharges() {
        assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(
                        service,
                        "validateMethod",
                        PaymentMethod.CREDIT_CARD
                )
        );
        assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(
                        service,
                        "validateMethod",
                        PaymentMethod.DEBIT_CARD
                )
        );

        BusinessException pixException = assertThrows(
                BusinessException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "validateMethod",
                        PaymentMethod.PIX
                )
        );
        BusinessException cashException = assertThrows(
                BusinessException.class,
                () -> ReflectionTestUtils.invokeMethod(
                        service,
                        "validateMethod",
                        PaymentMethod.CASH
                )
        );

        assertEquals("INVALID_TERMINAL_PAYMENT_METHOD", pixException.getCode());
        assertEquals("INVALID_TERMINAL_PAYMENT_METHOD", cashException.getCode());
    }

    @Test
    void timeoutKeepsPaymentPendingForLateResult() {
        PaymentRepository paymentRepository =
                mock(PaymentRepository.class);

        CheckoutSessionRepository checkoutRepository =
                mock(CheckoutSessionRepository.class);

        PaymentTerminalTransactionRepository
                transactionRepository =
                mock(
                        PaymentTerminalTransactionRepository.class
                );

        TerminalPaymentStartService startService =
                mock(TerminalPaymentStartService.class);

        TerminalPaymentCompletionService completionService =
                mock(TerminalPaymentCompletionService.class);

        PaymentIdempotencyValidator idempotencyValidator =
                mock(PaymentIdempotencyValidator.class);

        IdempotencyFingerprintService fingerprintService =
                mock(IdempotencyFingerprintService.class);

        CheckoutFinalizationService finalizationService =
                mock(CheckoutFinalizationService.class);

        TerminalPaymentService paymentService =
                new TerminalPaymentService(
                        paymentRepository,
                        checkoutRepository,
                        transactionRepository,
                        startService,
                        completionService,
                        idempotencyValidator,
                        fingerprintService,
                        finalizationService
                );

        UUID checkoutId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        PaymentEntity payment =
                mock(PaymentEntity.class);

        CheckoutSessionEntity checkout =
                mock(CheckoutSessionEntity.class);

        PaymentTerminalTransactionEntity transaction =
                mock(
                        PaymentTerminalTransactionEntity.class
                );

        PaymentProviderAdapter adapter =
                mock(PaymentProviderAdapter.class);

        PaymentProviderProfile profile =
                mock(PaymentProviderProfile.class);

        ProviderConfiguration configuration =
                mock(ProviderConfiguration.class);

        VitrineUserPrincipal principal =
                mock(VitrineUserPrincipal.class);

        when(principal.getId()).thenReturn(10L);

        when(fingerprintService.sha256(any()))
                .thenReturn("a".repeat(64));

        when(paymentRepository.findByIdempotencyKey(
                idempotencyKey
        )).thenReturn(Optional.empty());

        when(payment.getId()).thenReturn(paymentId);
        when(checkout.getTotalCents()).thenReturn(1_290L);

        ActivePaymentProviderService.ActiveProvider
                activeProvider =
                new ActivePaymentProviderService.ActiveProvider(
                        profile,
                        configuration,
                        adapter
                );

        when(startService.start(any()))
                .thenReturn(
                        new TerminalPaymentStartService.StartResult(
                                payment,
                                transaction,
                                checkout,
                                activeProvider
                        )
                );

        when(adapter.initiatePayment(any()))
                .thenThrow(
                        new BusinessException(
                                "PAYMENT_TERMINAL_COMMAND_EXPIRED",
                                "Tempo limite do comando excedido."
                        )
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> paymentService.process(
                                checkoutId,
                                idempotencyKey,
                                new br.com.vitrine7.payment.terminal.dto.TerminalPaymentRequest(
                                        PaymentMethod.CREDIT_CARD
                                ),
                                principal
                        )
                );

        assertEquals(
                "PAYMENT_TERMINAL_COMMAND_EXPIRED",
                exception.getCode()
        );

        verify(
                completionService,
                never()
        ).failCommunication(
                any(),
                any(),
                any(),
                any()
        );
    }
}
