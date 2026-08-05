package br.com.vitrine7.payment.terminal.bridge;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TerminalCommandExpirationServiceTest {

    private final TerminalCommandRepository repository =
            mock(TerminalCommandRepository.class);

    private final TerminalCommandQueueService queueService =
            mock(TerminalCommandQueueService.class);

    private final Clock clock =
            Clock.fixed(
                    Instant.parse(
                            "2026-08-01T12:00:00Z"
                    ),
                    ZoneOffset.UTC
            );

    @Test
    void expirationQueuesReconciliationOnlyForPayment() {
        UUID paymentCommandId =
                UUID.randomUUID();

        UUID queryCommandId =
                UUID.randomUUID();

        UUID deviceId =
                UUID.randomUUID();

        UUID transactionId =
                UUID.randomUUID();

        TerminalCommandRepository.CommandSnapshot payment =
                new TerminalCommandRepository.CommandSnapshot(
                        paymentCommandId,
                        deviceId,
                        transactionId,
                        "INITIATE_PAYMENT",
                        "EXPIRED",
                        null,
                        OffsetDateTime.now(clock)
                                .minusSeconds(1),
                        "PAYMENT_TERMINAL_COMMAND_EXPIRED",
                        "Comando expirado."
                );

        TerminalCommandRepository.CommandSnapshot query =
                new TerminalCommandRepository.CommandSnapshot(
                        queryCommandId,
                        deviceId,
                        transactionId,
                        "QUERY_PAYMENT",
                        "EXPIRED",
                        null,
                        OffsetDateTime.now(clock)
                                .minusSeconds(1),
                        "PAYMENT_TERMINAL_COMMAND_EXPIRED",
                        "Comando expirado."
                );

        when(repository.expireDue(
                OffsetDateTime.now(clock)
        )).thenReturn(
                List.of(payment, query)
        );

        TerminalCommandExpirationService service =
                new TerminalCommandExpirationService(
                        repository,
                        queueService,
                        clock
                );

        assertEquals(
                2,
                service.expireBatch()
        );

        verify(repository).mirrorDelivery(
                paymentCommandId,
                "EXPIRED"
        );

        verify(repository).mirrorDelivery(
                queryCommandId,
                "EXPIRED"
        );

        verify(queueService)
                .createReconciliationQuery(
                        transactionId,
                        deviceId
                );
    }
}
