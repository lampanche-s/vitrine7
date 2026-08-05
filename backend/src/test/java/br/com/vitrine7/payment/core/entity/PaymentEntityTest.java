package br.com.vitrine7.payment.core.entity;

import br.com.vitrine7.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentEntityTest {

    private static final OffsetDateTime APPROVED_AT =
            OffsetDateTime.parse(
                    "2026-08-01T12:00:00Z"
            );

    private static final OffsetDateTime REVERSED_AT =
            OffsetDateTime.parse(
                    "2026-08-01T12:30:00Z"
            );

    @Test
    void approvedPaymentCanBeMarkedReversed() {
        PaymentEntity payment =
                approvedTerminalPayment();

        payment.markReversed(
                20L,
                "  Cliente solicitou cancelamento  ",
                REVERSED_AT
        );

        assertEquals(
                PaymentStatus.REVERSED,
                payment.getStatus()
        );

        assertEquals(
                REVERSED_AT,
                payment.getReversedAt()
        );

        assertEquals(
                20L,
                payment.getReversedByUserId()
        );

        assertEquals(
                "Cliente solicitou cancelamento",
                payment.getReversalReason()
        );
    }

    @Test
    void reversalRequiresValidReason() {
        PaymentEntity payment =
                approvedTerminalPayment();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> payment.markReversed(
                                20L,
                                "x",
                                REVERSED_AT
                        )
                );

        assertEquals(
                "INVALID_PAYMENT_REVERSAL_REASON",
                exception.getCode()
        );

        assertEquals(
                PaymentStatus.APPROVED,
                payment.getStatus()
        );
    }

    @Test
    void nonApprovedPaymentCannotBeMarkedReversed() {
        PaymentEntity payment =
                PaymentEntity.processingTerminal(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "a".repeat(64),
                        PaymentMethod.CREDIT_CARD,
                        PaymentProcessingMode.TERMINAL_REAL,
                        1_290L,
                        10L
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> payment.markReversed(
                                20L,
                                "Cliente solicitou cancelamento",
                                REVERSED_AT
                        )
                );

        assertEquals(
                "PAYMENT_CANNOT_BE_REVERSED",
                exception.getCode()
        );
    }

    private PaymentEntity approvedTerminalPayment() {
        PaymentEntity payment =
                PaymentEntity.processingTerminal(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "a".repeat(64),
                        PaymentMethod.CREDIT_CARD,
                        PaymentProcessingMode.TERMINAL_REAL,
                        1_290L,
                        10L
                );

        payment.markApproved(
                10L,
                APPROVED_AT
        );

        return payment;
    }
}
