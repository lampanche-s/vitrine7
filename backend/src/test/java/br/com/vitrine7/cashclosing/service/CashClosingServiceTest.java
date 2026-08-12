package br.com.vitrine7.cashclosing.service;

import br.com.vitrine7.cashclosing.dto.CashClosingDay;
import br.com.vitrine7.cashclosing.dto.CashClosingResponse;
import br.com.vitrine7.cashclosing.repository.CashClosingRepository;
import br.com.vitrine7.common.config.BusinessProperties;
import br.com.vitrine7.system.user.security.VitrineUserPrincipal;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CashClosingServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-12T15:00:00Z"),
            ZoneOffset.UTC
    );

    private final CashClosingRepository repository = mock(CashClosingRepository.class);
    private final CashClosingService service = new CashClosingService(
            repository,
            CLOCK,
            new BusinessProperties(
                    "America/Bahia",
                    new BusinessProperties.Establishment("Vitrine 7", null, null, null)
            )
    );
    private final VitrineUserPrincipal principal = mock(VitrineUserPrincipal.class);

    @Test
    void summarizesOnlyApprovedSalesAndSeparatesReversals() {
        when(principal.getId()).thenReturn(7L);
        when(principal.getName()).thenReturn("Operador");
        when(repository.openCommands(eq(7L), any(), any())).thenReturn(
                new CashClosingRepository.OpenCommandsSummary(2L, 1800L)
        );
        when(repository.operations(eq(7L), any(), any())).thenReturn(List.of(
                operation(1L, "CASH", "APPROVED", 1000L, "2026-08-12T12:00:00Z"),
                operation(2L, "PIX", "APPROVED", 2500L, "2026-08-12T13:00:00Z"),
                operation(3L, "CASH", "REVERSED", 700L, "2026-08-12T14:00:00Z")
        ));
        when(repository.operationLines(List.of(1L, 2L, 3L))).thenReturn(List.of(
                line(1L, "ITEM", "Espeto", 2, 500L, 1000L),
                line(2L, "SERVICE", "Lavagem", 1, 2500L, 2500L),
                line(3L, "ITEM", "Refrigerante", 1, 700L, 700L)
        ));
        when(repository.findClosedAt(eq(7L), any())).thenReturn(java.util.Optional.empty());

        CashClosingResponse response = service.get(CashClosingDay.TODAY, principal);

        assertEquals(4200L, response.grossSalesCents());
        assertEquals(3500L, response.totalReceivedCents());
        assertEquals(1000L, response.itemSalesCents());
        assertEquals(2500L, response.serviceSalesCents());
        assertEquals(2L, response.saleCount());
        assertEquals(1750L, response.averageTicketCents());
        assertEquals(2L, response.openCommandCount());
        assertEquals(1800L, response.openCommandAmountCents());
        assertEquals(700L, response.reversedCents());
        assertEquals(1L, response.reversedCount());
        assertEquals(1500L, response.cashReceivedCents());
        assertEquals(500L, response.cashChangeCents());
        assertFalse(response.closed());
        assertEquals(2, response.paymentBreakdown().size());
    }

    @Test
    void closeRegistersSnapshotForCurrentUser() {
        when(principal.getId()).thenReturn(7L);
        when(principal.getName()).thenReturn("Operador");
        when(repository.openCommands(eq(7L), any(), any())).thenReturn(
                new CashClosingRepository.OpenCommandsSummary(2L, 1800L)
        );
        when(repository.operations(eq(7L), any(), any())).thenReturn(List.of());
        when(repository.operationLines(List.of())).thenReturn(List.of());
        when(repository.findClosedAt(eq(7L), any())).thenReturn(java.util.Optional.empty());

        CashClosingResponse response = service.close(CashClosingDay.YESTERDAY, principal);

        assertTrue(response.closed());
        verify(repository).upsert(eq(7L), eq(response.businessDate()), any(), eq(response.closedAt()));
    }

    private CashClosingResponse.Operation operation(
            long id,
            String method,
            String status,
            long amount,
            String completedAt
    ) {
        return new CashClosingResponse.Operation(
                id,
                "Comanda " + id,
                OffsetDateTime.parse(completedAt),
                method,
                status,
                amount,
                "CASH".equals(method) && "APPROVED".equals(status)
                        ? amount + 500L
                        : 0L,
                "CASH".equals(method) && "APPROVED".equals(status)
                        ? 500L
                        : 0L,
                List.of()
        );
    }

    private CashClosingRepository.OperationLine line(
            long operationId,
            String entryType,
            String itemName,
            int quantity,
            long unitPriceCents,
            long lineTotalCents
    ) {
        return new CashClosingRepository.OperationLine(
                operationId,
                entryType,
                itemName,
                quantity,
                unitPriceCents,
                lineTotalCents
        );
    }
}
