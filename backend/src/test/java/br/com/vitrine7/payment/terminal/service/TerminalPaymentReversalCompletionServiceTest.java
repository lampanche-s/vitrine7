package br.com.vitrine7.payment.terminal.service;

import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.entity.PaymentMethod;
import br.com.vitrine7.payment.core.entity.PaymentProcessingMode;
import br.com.vitrine7.payment.core.entity.PaymentStatus;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalMode;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalProvider;
import br.com.vitrine7.payment.terminal.entity.PaymentTerminalTransactionEntity;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderCode;
import br.com.vitrine7.payment.terminal.provider.PaymentProviderEnvironment;
import br.com.vitrine7.payment.terminal.provider.ProviderPaymentStatus;
import br.com.vitrine7.payment.terminal.repository.PaymentTerminalTransactionRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TerminalPaymentReversalCompletionServiceTest {

    private final PaymentRepository paymentRepository =
            mock(PaymentRepository.class);

    private final PaymentTerminalTransactionRepository
            transactionRepository =
            mock(
                    PaymentTerminalTransactionRepository.class
            );

    private final TerminalPaymentReversalCompletionService
            service =
            new TerminalPaymentReversalCompletionService(
                    paymentRepository,
                    transactionRepository
            );

    @Test
    void approvedReversalMarksPaymentReversed() {
        Fixture fixture = fixture();

        fixture.payment().markReversalPending();

        arrange(fixture);

        service.complete(
                fixture.transactionId(),
                ProviderPaymentStatus.APPROVED,
                20L,
                "Cliente solicitou cancelamento",
                OffsetDateTime.parse(
                        "2026-08-01T13:00:00Z"
                )
        );

        assertEquals(
                PaymentStatus.REVERSED,
                fixture.payment().getStatus()
        );

        assertEquals(
                20L,
                fixture.payment()
                        .getReversedByUserId()
        );

        assertEquals(
                "Cliente solicitou cancelamento",
                fixture.payment()
                        .getReversalReason()
        );

    }

    @Test
    void failedReversalRestoresApprovedPayment() {
        Fixture fixture = fixture();

        fixture.payment().markReversalPending();

        arrange(fixture);

        service.complete(
                fixture.transactionId(),
                ProviderPaymentStatus.CANCELLED,
                20L,
                "Cliente desistiu",
                OffsetDateTime.parse(
                        "2026-08-01T13:00:00Z"
                )
        );

        assertEquals(
                PaymentStatus.APPROVED,
                fixture.payment().getStatus()
        );

    }

    @Test
    void unknownReversalKeepsPaymentPending() {
        Fixture fixture = fixture();

        fixture.payment().markReversalPending();

        arrange(fixture);

        service.complete(
                fixture.transactionId(),
                ProviderPaymentStatus.UNKNOWN,
                20L,
                "Resultado nao confirmado",
                OffsetDateTime.parse(
                        "2026-08-01T13:00:00Z"
                )
        );

        assertEquals(
                PaymentStatus.REVERSAL_PENDING,
                fixture.payment().getStatus()
        );

    }

    private void arrange(Fixture fixture) {
        when(transactionRepository.findById(
                fixture.transactionId()
        )).thenReturn(
                Optional.of(fixture.transaction())
        );

        when(paymentRepository.findByIdForUpdate(
                fixture.paymentId()
        )).thenReturn(
                Optional.of(fixture.payment())
        );

        when(transactionRepository.findByIdForUpdate(
                fixture.transactionId()
        )).thenReturn(
                Optional.of(fixture.transaction())
        );
    }

    private Fixture fixture() {
        UUID checkoutId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        OffsetDateTime approvedAt =
                OffsetDateTime.parse(
                        "2026-08-01T12:00:00Z"
                );

        PaymentEntity payment =
                PaymentEntity.processingTerminal(
                        checkoutId,
                        UUID.randomUUID(),
                        "a".repeat(64),
                        PaymentMethod.CREDIT_CARD,
                        PaymentProcessingMode.TERMINAL_REAL,
                        1_290L,
                        10L
                );

        payment.markApproved(
                10L,
                approvedAt
        );

        PaymentTerminalTransactionEntity transaction =
                PaymentTerminalTransactionEntity.sent(
                        paymentId,
                        checkoutId,
                        UUID.randomUUID(),
                        "b".repeat(64),
                        PaymentTerminalProvider.PAGBANK,
                        PaymentTerminalMode.REAL,
                        PaymentMethod.CREDIT_CARD,
                        1_290L,
                        10L,
                        approvedAt.minusSeconds(10),
                        UUID.randomUUID(),
                        PaymentProviderCode.PAGBANK,
                        PaymentProviderEnvironment.LOCAL,
                        1L
                );

        transaction.markApproved(
                "TX-ORIGINAL",
                "00",
                "Transacao aprovada.",
                approvedAt,
                "REQUEST-ORIGINAL",
                Map.of(
                        "userReference",
                        "ABC1234567"
                )
        );

        org.springframework.test.util
                .ReflectionTestUtils
                .setField(
                        payment,
                        "id",
                        paymentId
                );

        org.springframework.test.util
                .ReflectionTestUtils
                .setField(
                        transaction,
                        "id",
                        transactionId
                );

        return new Fixture(
                paymentId,
                transactionId,
                payment,
                transaction
        );
    }

    private record Fixture(
            UUID paymentId,
            UUID transactionId,
            PaymentEntity payment,
            PaymentTerminalTransactionEntity transaction
    ) {
    }
}
