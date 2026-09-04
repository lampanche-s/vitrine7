package br.com.vitrine7.print.service;

import br.com.vitrine7.bar.tab.dto.BarTabResponse;
import br.com.vitrine7.bar.tab.service.BarTabService;
import br.com.vitrine7.cashclosing.dto.CashClosingDay;
import br.com.vitrine7.cashclosing.dto.CashClosingResponse;
import br.com.vitrine7.cashclosing.service.CashClosingService;
import br.com.vitrine7.print.dto.PrintJobDtos;
import br.com.vitrine7.print.PrintDocumentKind;
import br.com.vitrine7.print.repository.PrintJobRepository;
import br.com.vitrine7.receipt.dto.ReceiptResponse;
import br.com.vitrine7.receipt.service.ReceiptService;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
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
    private final CashClosingTextRenderer cashClosingRenderer = mock(CashClosingTextRenderer.class);
    private final BarTabService barTabService = mock(BarTabService.class);
    private final CashClosingService cashClosingService = mock(CashClosingService.class);
    private final PrintJobService service = new PrintJobService(
            repository,
            receiptService,
            renderer,
            prePaymentRenderer,
            cashClosingRenderer,
            barTabService,
            cashClosingService,
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
    void queuesPrePaymentNoteForOpenTabWithoutCheckoutOrPayment() {
        VitrineUserPrincipal principal = mock(VitrineUserPrincipal.class);
        BarTabResponse tab = new BarTabResponse(
                44L,
                "Mesa 4",
                null,
                null,
                "OPEN",
                null,
                null,
                1500L,
                0L,
                1500L,
                "GENERAL_RECEIPT",
                null,
                true,
                null,
                null,
                null,
                false,
                java.util.List.of(new br.com.vitrine7.bar.tab.dto.BarTabLineResponse(
                        1L, 2L, "ITEM", "Cafe", 1500L, 1, 1500L
                )),
                9L,
                null,
                null
        );

        when(principal.getId()).thenReturn(9L);
        when(barTabService.findById(44L)).thenReturn(tab);
        when(prePaymentRenderer.render(tab)).thenReturn("CONFERENCIA\n");

        PrintJobDtos.Created created = service.createPrePaymentNote(44L, principal);

        assertEquals("PENDING", created.status());
        verify(repository).createPrePaymentNote(
                created.id(),
                44L,
                9L,
                "CONFERENCIA\n"
        );
        verifyNoInteractions(receiptService, renderer);
    }

    @Test
    void allowsRepeatedPrePaymentNotesUsingTheCurrentOpenTab() {
        VitrineUserPrincipal principal = mock(VitrineUserPrincipal.class);
        BarTabResponse tab = openTabWithLines();

        when(principal.getId()).thenReturn(9L);
        when(barTabService.findById(44L)).thenReturn(tab);
        when(prePaymentRenderer.render(tab)).thenReturn("CONFERENCIA\n");

        service.createPrePaymentNote(44L, principal);
        service.createPrePaymentNote(44L, principal);

        verify(repository, times(2)).createPrePaymentNote(
                any(UUID.class),
                org.mockito.ArgumentMatchers.eq(44L),
                org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.eq("CONFERENCIA\n")
        );
        verifyNoInteractions(receiptService, renderer);
    }

    @Test
    void rejectsPrePaymentNoteForOpenTabWithoutLines() {
        VitrineUserPrincipal principal = mock(VitrineUserPrincipal.class);
        BarTabResponse tab = tabWithStatusAndLines("OPEN", java.util.List.of());

        when(barTabService.findById(44L)).thenReturn(tab);

        assertThrows(
                br.com.vitrine7.common.exception.BusinessException.class,
                () -> service.createPrePaymentNote(44L, principal)
        );
        verifyNoInteractions(repository, receiptService, renderer);
    }

    @Test
    void rejectsPrePaymentNoteOutsideConsumption() {
        VitrineUserPrincipal principal = mock(VitrineUserPrincipal.class);

        for (String status : java.util.List.of("PAYMENT_PENDING", "CLOSED")) {
            BarTabResponse tab = tabWithStatusAndLines(
                    status,
                    openTabWithLines().lines()
            );
            when(barTabService.findById(44L)).thenReturn(tab);

            assertThrows(
                    br.com.vitrine7.common.exception.BusinessException.class,
                    () -> service.createPrePaymentNote(44L, principal)
            );
        }

        verifyNoInteractions(repository, receiptService, renderer);
    }

    private BarTabResponse openTabWithLines() {
        return tabWithStatusAndLines(
                "OPEN",
                java.util.List.of(new br.com.vitrine7.bar.tab.dto.BarTabLineResponse(
                        1L, 2L, "ITEM", "Cafe", 1500L, 1, 1500L
                ))
        );
    }

    private BarTabResponse tabWithStatusAndLines(
            String status,
            java.util.List<br.com.vitrine7.bar.tab.dto.BarTabLineResponse> lines
    ) {
        return new BarTabResponse(
                44L, "Mesa 4", null, null, status, null, null,
                1500L, 0L, 1500L, "GENERAL_RECEIPT", null, true,
                null, null, null, false,
                lines,
                9L, null, null
        );
    }

    @Test
    void queuesCashClosingForSelectedBusinessDate() {
        VitrineUserPrincipal principal = mock(VitrineUserPrincipal.class);
        CashClosingResponse report = mock(CashClosingResponse.class);
        LocalDate businessDate = LocalDate.of(2026, 8, 12);

        when(principal.getId()).thenReturn(9L);
        when(cashClosingService.get(CashClosingDay.TODAY, principal))
                .thenReturn(report);
        when(report.businessDate()).thenReturn(businessDate);
        when(repository.findActiveCashClosing(9L, businessDate))
                .thenReturn(Optional.empty());
        when(cashClosingRenderer.render(report)).thenReturn("FECHAMENTO\n");

        PrintJobDtos.Created created = service.createCashClosing(
                CashClosingDay.TODAY,
                principal
        );

        assertEquals("PENDING", created.status());
        verify(repository).createCashClosing(
                created.id(),
                9L,
                businessDate,
                "FECHAMENTO\n"
        );
    }

    @Test
    void queuesItemAndServiceOrdersWithDirectBarTabReference() {
        VitrineUserPrincipal principal = mock(VitrineUserPrincipal.class);
        when(principal.getId()).thenReturn(19L);

        for (PrintDocumentKind kind : java.util.List.of(
                PrintDocumentKind.ITEM_ORDER,
                PrintDocumentKind.SERVICE_ORDER
        )) {
            PrintJobDtos.Created created = service.createOperationalOrder(
                    44L,
                    kind,
                    "PEDIDO\n",
                    principal
            );

            assertEquals("PENDING", created.status());
            verify(repository).createOperationalOrder(
                    created.id(),
                    44L,
                    19L,
                    kind.name(),
                    "PEDIDO\n"
            );
        }
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
