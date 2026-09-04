package br.com.vitrine7.bar.tab.entity;

import br.com.vitrine7.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BarTabEntityTest {

    @Test
    void transitionsFromOpenToPaymentPendingToClosed() {
        BarTabEntity tab = openTab();
        UUID checkoutId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.parse("2026-08-25T15:00:00-03:00");

        tab.markPaymentPending(checkoutId, 2_000L, 100L, UUID.randomUUID(), "fingerprint", now);
        assertEquals(BarTabStatus.PAYMENT_PENDING, tab.getStatus());

        tab.markClosed(checkoutId, now.plusMinutes(5));
        assertEquals(BarTabStatus.CLOSED, tab.getStatus());
        assertEquals(BarTabClosureType.PAYMENT, tab.getClosureType());
    }

    @Test
    void voucherClosesWithoutCheckoutAndCanBeReopenedKeepingEmployee() {
        BarTabEntity tab = BarTabEntity.open(
                "João", "joao", UUID.randomUUID(), "fingerprint", null, 19L, 7L
        );
        OffsetDateTime now = OffsetDateTime.parse("2026-08-25T15:00:00-03:00");

        tab.captureVehicleSnapshot("Onix", "ABC1D23");
        tab.markVoucherClosed(2_000L, now);

        assertEquals(BarTabStatus.CLOSED, tab.getStatus());
        assertEquals(BarTabClosureType.VOUCHER, tab.getClosureType());
        assertNull(tab.getCheckoutSessionId());

        tab.reopenClosedVoucher(7L, now.plusMinutes(10));
        assertEquals(BarTabStatus.OPEN, tab.getStatus());
        assertNull(tab.getClosureType());
        assertEquals(19L, tab.getEmployeeId());
        assertNull(tab.getVehicleNameSnapshot());
        assertNull(tab.getVehiclePlateSnapshot());
    }

    @Test
    void preparationRejectsAnyStatusOtherThanOpen() {
        BarTabEntity tab = openTab();
        tab.cancelOpen(7L, "Cancelada para teste", OffsetDateTime.now());

        assertThrows(
                BusinessException.class,
                () -> tab.markPaymentPending(
                        UUID.randomUUID(),
                        2_000L,
                        0L,
                        UUID.randomUUID(),
                        "fingerprint",
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void releasedCheckoutReturnsPaymentPendingTabToOpen() {
        BarTabEntity tab = openTab();
        UUID checkoutId = UUID.randomUUID();
        tab.markPaymentPending(
                checkoutId,
                2_000L,
                100L,
                UUID.randomUUID(),
                "fingerprint",
                OffsetDateTime.now()
        );

        tab.reopenAfterCheckoutRelease(checkoutId);

        assertEquals(BarTabStatus.OPEN, tab.getStatus());
        assertNull(tab.getCheckoutSessionId());
        assertEquals(0L, tab.getDiscountCents());
        assertEquals(tab.getSubtotalCents(), tab.getTotalCents());
    }

    private BarTabEntity openTab() {
        return BarTabEntity.open(
                "Mesa 1",
                "mesa 1",
                UUID.randomUUID(),
                "fingerprint",
                null,
                7L
        );
    }
}
