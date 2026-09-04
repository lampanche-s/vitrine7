package br.com.vitrine7.payment.core.service;

import br.com.vitrine7.checkout.entity.CheckoutOperationType;
import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.checkout.entity.CheckoutStatus;
import br.com.vitrine7.checkout.repository.CheckoutSessionRepository;
import br.com.vitrine7.checkout.service.CheckoutFinalizationService;
import br.com.vitrine7.common.idempotency.IdempotencyFingerprintService;
import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.entity.PaymentMethod;
import br.com.vitrine7.payment.core.entity.PaymentProcessingMode;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private final PaymentRepository paymentRepository =
            mock(PaymentRepository.class);
    private final CheckoutSessionRepository checkoutRepository =
            mock(CheckoutSessionRepository.class);
    private final PaymentExecutionService executionService =
            mock(PaymentExecutionService.class);
    private final PaymentIdempotencyValidator idempotencyValidator =
            mock(PaymentIdempotencyValidator.class);
    private final CheckoutFinalizationService finalizationService =
            mock(CheckoutFinalizationService.class);
    private final IdempotencyFingerprintService fingerprintService =
            mock(IdempotencyFingerprintService.class);
    private final PaymentRequestNormalizer requestNormalizer =
            mock(PaymentRequestNormalizer.class);

    private final PaymentService service =
            new PaymentService(
                    paymentRepository,
                    checkoutRepository,
                    executionService,
                    idempotencyValidator,
                    finalizationService,
                    fingerprintService,
                    requestNormalizer
            );

    @Test
    void pixConfirmationBuildsSimpleManualPixCommand() {
        UUID checkoutId =
                UUID.fromString("11111111-1111-4111-8111-111111111111");
        UUID idempotencyKey =
                UUID.fromString("22222222-2222-4222-8222-222222222222");
        OffsetDateTime now =
                OffsetDateTime.parse("2026-07-24T12:00:00Z");
        VitrineUserPrincipal principal =
                mock(VitrineUserPrincipal.class);

        when(principal.getId()).thenReturn(7L);
        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(fingerprintService.sha256(any()))
                .thenReturn("abc123");

        PaymentEntity payment =
                PaymentEntity.approvedManualFallback(
                        checkoutId,
                        idempotencyKey,
                        "abc123",
                        PaymentMethod.PIX,
                        4500L,
                        "Pix confirmado.",
                        7L,
                        now
                );
        CheckoutSessionEntity checkout =
                CheckoutSessionEntity.openDraft(
                        UUID.fromString("33333333-3333-4333-8333-333333333333"),
                        "fingerprint",
                        CheckoutOperationType.BAR_COMMAND,
                        7L,
                        now.plusMinutes(30)
                );
        ReflectionTestUtils.setField(checkout, "id", checkoutId);
        ReflectionTestUtils.setField(checkout, "sourceId", 21L);
        ReflectionTestUtils.setField(checkout, "status", CheckoutStatus.FINALIZED);
        ReflectionTestUtils.setField(checkout, "totalCents", 4500L);

        when(executionService.execute(any()))
                .thenReturn(
                        new PaymentExecutionService.ExecutionResult(
                                payment,
                                checkout,
                                false
                        )
                );
        when(checkoutRepository.findById(checkoutId))
                .thenReturn(Optional.of(checkout));

        service.confirmPix(
                checkoutId,
                idempotencyKey,
                principal
        );

        ArgumentCaptor<PaymentExecutionCommand> captor =
                ArgumentCaptor.forClass(
                        PaymentExecutionCommand.class
                );
        verify(executionService).execute(captor.capture());

        PaymentExecutionCommand command =
                captor.getValue();

        assertEquals(PaymentMethod.PIX, command.method());
        assertEquals(
                PaymentProcessingMode.MANUAL_FALLBACK,
                command.processingMode()
        );
        assertNull(command.cashReceivedCents());
        assertEquals(
                "Pix confirmado.",
                command.manualReason()
        );
        verify(finalizationService)
                .finalizeCheckoutIfSupported(checkoutId, 7L);
    }
}
