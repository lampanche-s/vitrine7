package br.com.vitrine7.payment.core.service;

import br.com.vitrine7.common.exception.BusinessException;
import br.com.vitrine7.payment.core.dto.PaymentResponse;
import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.entity.PaymentMethod;
import br.com.vitrine7.payment.core.entity.PaymentProcessingMode;
import br.com.vitrine7.payment.core.entity.PaymentStatus;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentReversalServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-05T18:00:00Z");

    private final PaymentRepository repository =
            mock(PaymentRepository.class);

    private final PaymentReversalService service =
            new PaymentReversalService(
                    repository,
                    Clock.fixed(NOW, ZoneOffset.UTC)
            );

    @Test
    void marksApprovedPaymentAsReversed() {
        UUID paymentId = UUID.randomUUID();
        PaymentEntity payment = approvedPayment();
        ReflectionTestUtils.setField(
                payment,
                "id",
                paymentId
        );

        when(repository.findByIdForUpdate(paymentId))
                .thenReturn(Optional.of(payment));
        when(repository.findApprovedByCheckoutForUpdate(payment.getCheckoutSessionId()))
                .thenReturn(List.of(payment));

        PaymentResponse response =
                service.markReversed(
                        paymentId,
                        "Cliente solicitou cancelamento",
                        20L
                );

        assertEquals("REVERSED", response.status());
        assertEquals(
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC),
                response.reversedAt()
        );
        assertEquals(20L, response.reversedByUserId());
        assertEquals(
                "Cliente solicitou cancelamento",
                response.reversalReason()
        );

        verify(repository).flush();
    }

    @Test
    void rejectsSecondReversal() {
        UUID paymentId = UUID.randomUUID();
        PaymentEntity payment = approvedPayment();

        payment.markReversed(
                20L,
                "Cliente solicitou cancelamento",
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)
        );

        when(repository.findByIdForUpdate(paymentId))
                .thenReturn(Optional.of(payment));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.markReversed(
                                paymentId,
                                "Nova tentativa",
                                20L
                        )
                );

        assertEquals(
                "PAYMENT_ALREADY_REVERSED",
                exception.getCode()
        );
    }

    private PaymentEntity approvedPayment() {
        PaymentEntity payment =
                PaymentEntity.processingTerminal(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "a".repeat(64),
                        PaymentMethod.CREDIT_CARD,
                        PaymentProcessingMode.TERMINAL_SIMULATED,
                        2_500L,
                        10L
                );

        payment.markApproved(
                10L,
                OffsetDateTime.parse(
                        "2026-08-05T17:00:00Z"
                )
        );

        return payment;
    }
}
