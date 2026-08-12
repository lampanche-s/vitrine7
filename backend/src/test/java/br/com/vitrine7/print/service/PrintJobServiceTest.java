package br.com.vitrine7.print.service;

import br.com.vitrine7.bar.tab.dto.BarTabResponse;
import br.com.vitrine7.bar.tab.service.BarTabService;
import br.com.vitrine7.print.dto.PrintJobDtos;
import br.com.vitrine7.print.repository.PrintJobRepository;
import br.com.vitrine7.receipt.dto.ReceiptResponse;
import br.com.vitrine7.receipt.service.ReceiptService;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrintJobServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-06T23:00:00Z"),
            ZoneOffset.UTC
    );

    private final PrintJobRepository repository = mock(PrintJobRepository.class);
    private final ReceiptService receiptService = mock(ReceiptService.class);
    private final ReceiptTextRenderer renderer = mock(ReceiptTextRenderer.class);
    private final PrePaymentNoteRenderer prePaymentRenderer = mock(PrePaymentNoteRenderer.class);
    private final BarTabService barTabService = mock(BarTabService.class);
    private final PrintJobService service = new PrintJobService(
            repository,
            receiptService,
            renderer,
            prePaymentRenderer,
            barTabService,
            CLOCK
    );

    @Test
    void queuesReceiptSnapshotForAuthenticatedUser() {
        UUID checkoutId = UUID.randomUUID();
        ReceiptResponse receipt = mock(ReceiptResponse.class);
        VitrineUserPrincipal principal = mock(VitrineUserPrincipal.class);

        when(principal.getId()).thenReturn(15L);
        when(repository.findActiveByCheckoutId(checkoutId, "RECEIPT")).thenReturn(Optional.empty());
        when(receiptService.getReceipt(checkoutId, principal)).thenReturn(receipt);
        when(renderer.render(receipt)).thenReturn("RECIBO\n");

        PrintJobDtos.Created created = service.create(checkoutId, principal);

        assertEquals("PENDING", created.status());
        verify(repository).create(
                created.id(),
                checkoutId,
                15L,
                "RECEIPT",
                "RECIBO\n"
        );
    }


    @Test
    void reusesActiveJobInsteadOfQueuingDuplicate() {
        UUID checkoutId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        VitrineUserPrincipal principal = mock(VitrineUserPrincipal.class);
        PrintJobDtos.Created active = new PrintJobDtos.Created(jobId, "PENDING");

        when(repository.findActiveByCheckoutId(checkoutId, "RECEIPT"))
                .thenReturn(Optional.of(active));

        assertEquals(active, service.create(checkoutId, principal));
        verify(repository).findActiveByCheckoutId(checkoutId, "RECEIPT");
    }

    @Test
    void queuesPrePaymentNoteSeparatelyFromReceipt() {
        UUID checkoutId = UUID.randomUUID();
        VitrineUserPrincipal principal = mock(VitrineUserPrincipal.class);
        BarTabResponse tab = new BarTabResponse(
                44L,
                "Mesa 4",
                "PAYMENT_PENDING",
                checkoutId,
                "AWAITING_PAYMENT",
                1500L,
                0L,
                1500L,
                "GENERAL_RECEIPT",
                null,
                true,
                null,
                java.util.List.of(),
                9L,
                null,
                null
        );

        when(principal.getId()).thenReturn(9L);
        when(barTabService.findById(44L)).thenReturn(tab);
        when(repository.findActiveByCheckoutId(checkoutId, "PREPAYMENT_NOTE"))
                .thenReturn(Optional.empty());
        when(prePaymentRenderer.render(tab)).thenReturn("CONFERENCIA\n");

        PrintJobDtos.Created created = service.createPrePaymentNote(44L, principal);

        assertEquals("PENDING", created.status());
        verify(repository).create(
                created.id(),
                checkoutId,
                9L,
                "PREPAYMENT_NOTE",
                "CONFERENCIA\n"
        );
    }

    @Test
    void reservesPendingJobUsingCurrentClock() {
        UUID jobId = UUID.randomUUID();
        PrintJobDtos.Delivery delivery = new PrintJobDtos.Delivery(
                jobId,
                "RECIBO\n",
                1
        );
        OffsetDateTime now = OffsetDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC);

        when(repository.reserveNext(any(OffsetDateTime.class)))
                .thenReturn(Optional.of(delivery));

        assertEquals(delivery, service.reserveNext());
        verify(repository).reserveNext(now);
    }
}
