package br.com.vitrine7.bar.tab.service;

import br.com.vitrine7.bar.tab.entity.BarTabEntity;
import br.com.vitrine7.bar.tab.entity.BarTabLineEntity;
import br.com.vitrine7.bar.tab.entity.BarTabStatus;
import br.com.vitrine7.bar.tab.repository.BarTabLineRepository;
import br.com.vitrine7.bar.tab.repository.BarTabRepository;
import br.com.vitrine7.checkout.entity.CheckoutSessionEntity;
import br.com.vitrine7.payment.core.entity.PaymentEntity;
import br.com.vitrine7.payment.core.entity.PaymentStatus;
import br.com.vitrine7.payment.core.repository.PaymentRepository;
import br.com.vitrine7.print.repository.PrintJobRepository;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

class BarTabPaymentFlowTest {

    @Test
    void finalizationDecreasesStockExactlyOnce() {
        BarTabRepository tabRepository = mock(BarTabRepository.class);
        BarTabLineRepository lineRepository = mock(BarTabLineRepository.class);
        BarTabStockService stockService = mock(BarTabStockService.class);
        BarTabFinalizationHandler handler = new BarTabFinalizationHandler(
                tabRepository,
                lineRepository,
                stockService
        );
        UUID checkoutId = UUID.randomUUID();
        BarTabEntity tab = paymentPendingTab(checkoutId);
        BarTabLineEntity line = mock(BarTabLineEntity.class);
        CheckoutSessionEntity checkout = mock(CheckoutSessionEntity.class);
        when(checkout.getId()).thenReturn(checkoutId);
        when(checkout.getSourceId()).thenReturn(31L);
        when(tabRepository.findByCheckoutSessionIdForUpdate(checkoutId))
                .thenReturn(Optional.of(tab));
        when(lineRepository.findAllByTabIdOrderByIdAsc(31L))
                .thenReturn(List.of(line));

        handler.finalizeOperation(
                checkout,
                7L,
                OffsetDateTime.parse("2026-08-25T15:05:00-03:00")
        );

        verify(stockService, times(1)).validateAndDecrease(List.of(line));
        assertEquals(BarTabStatus.CLOSED, tab.getStatus());
    }

    @Test
    void reopeningClosedPaymentRestoresStockAndSupersedesApprovedPaymentOnce() {
        BarTabRepository tabRepository = mock(BarTabRepository.class);
        BarTabLineRepository lineRepository = mock(BarTabLineRepository.class);
        BarTabStockService stockService = mock(BarTabStockService.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        PrintJobRepository printJobRepository = mock(PrintJobRepository.class);
        BarTabService tabService = mock(BarTabService.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-25T18:30:00Z"),
                ZoneOffset.of("-03:00")
        );
        BarTabReopenService reopenService = new BarTabReopenService(
                tabRepository,
                lineRepository,
                stockService,
                paymentRepository,
                printJobRepository,
                tabService,
                clock
        );
        UUID checkoutId = UUID.randomUUID();
        BarTabEntity tab = paymentPendingTab(checkoutId);
        tab.markClosed(
                checkoutId,
                OffsetDateTime.parse("2026-08-25T15:10:00-03:00")
        );
        BarTabLineEntity line = mock(BarTabLineEntity.class);
        PaymentEntity payment = PaymentEntity.approvedCash(
                checkoutId,
                UUID.randomUUID(),
                "fingerprint",
                2_000L,
                2_000L,
                7L,
                OffsetDateTime.parse("2026-08-25T15:05:00-03:00")
        );
        VitrineUserPrincipal principal = mock(VitrineUserPrincipal.class);
        when(principal.getId()).thenReturn(7L);
        when(tabRepository.findByIdForUpdate(31L)).thenReturn(Optional.of(tab));
        when(lineRepository.findAllByTabIdOrderByIdAsc(31L)).thenReturn(List.of(line));
        when(paymentRepository.findApprovedByCheckoutForUpdate(checkoutId))
                .thenReturn(List.of(payment));

        reopenService.reopen(31L, principal);

        verify(stockService, times(1)).restore(List.of(line));
        verify(printJobRepository, times(1)).cancelActiveForCheckout(
                checkoutId,
                OffsetDateTime.parse("2026-08-25T15:30:00-03:00")
        );
        assertEquals(PaymentStatus.SUPERSEDED, payment.getStatus());
        assertEquals(BarTabStatus.OPEN, tab.getStatus());
    }

    @Test
    void reopeningVoucherRestoresStockWithoutLookingForPayments() {
        BarTabRepository tabRepository = mock(BarTabRepository.class);
        BarTabLineRepository lineRepository = mock(BarTabLineRepository.class);
        BarTabStockService stockService = mock(BarTabStockService.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        PrintJobRepository printJobRepository = mock(PrintJobRepository.class);
        BarTabService tabService = mock(BarTabService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-25T18:30:00Z"), ZoneOffset.of("-03:00"));
        BarTabReopenService service = new BarTabReopenService(tabRepository, lineRepository, stockService,
                paymentRepository, printJobRepository, tabService, clock);
        BarTabEntity tab = BarTabEntity.open("João", "joao", UUID.randomUUID(), "fingerprint", null, 19L, 7L);
        ReflectionTestUtils.setField(tab, "id", 31L);
        tab.markVoucherClosed(2_000L, OffsetDateTime.parse("2026-08-25T15:10:00-03:00"));
        BarTabLineEntity line = mock(BarTabLineEntity.class);
        VitrineUserPrincipal principal = mock(VitrineUserPrincipal.class);
        when(principal.getId()).thenReturn(7L);
        when(tabRepository.findByIdForUpdate(31L)).thenReturn(Optional.of(tab));
        when(lineRepository.findAllByTabIdOrderByIdAsc(31L)).thenReturn(List.of(line));

        service.reopen(31L, principal);

        verify(stockService).restore(List.of(line));
        verify(paymentRepository, never()).findApprovedByCheckoutForUpdate(org.mockito.ArgumentMatchers.any());
        assertEquals(BarTabStatus.OPEN, tab.getStatus());
        assertEquals(19L, tab.getEmployeeId());
    }

    private BarTabEntity paymentPendingTab(UUID checkoutId) {
        BarTabEntity tab = BarTabEntity.open(
                "Mesa 1",
                "mesa 1",
                UUID.randomUUID(),
                "fingerprint",
                null,
                7L
        );
        ReflectionTestUtils.setField(tab, "id", 31L);
        tab.markPaymentPending(
                checkoutId,
                2_000L,
                0L,
                UUID.randomUUID(),
                "fingerprint",
                OffsetDateTime.parse("2026-08-25T15:00:00-03:00")
        );
        return tab;
    }
}
