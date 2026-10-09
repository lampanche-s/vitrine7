package br.com.vitrine7.cashclosing.service;

import br.com.vitrine7.cashclosing.dto.CashClosingResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CashClosingSummaryTest {

    @Test
    void consolidatesDescriptionsWithinEachTypeAndExcludesReversals() {
        CashClosingSummary summary = CashClosingSummary.from(List.of(
                operation("APPROVED", 2500L,
                        line("ITEM", "Cafe", 2, 1000L),
                        line("SERVICE", "Atendimento", 1, 1500L)),
                operation("APPROVED", 1400L,
                        line("ITEM", "Cafe", 3, 900L),
                        line("SERVICE", "Cafe", 1, 500L)),
                operation("REVERSED", 5000L,
                        line("ITEM", "Cafe", 10, 5000L))
        ));

        assertEquals(List.of(new CashClosingSummary.Entry("Cafe", 5, 1900L)), summary.items());
        assertEquals(List.of(
                new CashClosingSummary.Entry("Atendimento", 1, 1500L),
                new CashClosingSummary.Entry("Cafe", 1, 500L)
        ), summary.services());
        assertEquals(1900L, summary.itemTotalCents());
        assertEquals(2000L, summary.serviceTotalCents());
    }

    @Test
    void allocatesDiscountsAcrossTypesAndDescriptionsWithoutLosingCents() {
        CashClosingSummary summary = CashClosingSummary.from(List.of(
                operation("APPROVED", 7L,
                        line("ITEM", "A", 1, 3L),
                        line("ITEM", "B", 1, 3L),
                        line("SERVICE", "C", 1, 4L)),
                operation("APPROVED", 1L,
                        line("ITEM", "A", 1, 1L),
                        line("ITEM", "B", 1, 1L),
                        line("ITEM", "Gratis", 1, 0L))
        ));

        assertEquals(5L, summary.itemTotalCents());
        assertEquals(3L, summary.serviceTotalCents());
        assertEquals(8L, summary.itemTotalCents() + summary.serviceTotalCents());
        assertEquals(new CashClosingSummary.Entry("Gratis", 1, 0L), summary.items().get(2));
    }

    @Test
    void handlesEmptyPeriodsAndServiceOnlySales() {
        assertEquals(new CashClosingSummary(List.of(), List.of()), CashClosingSummary.from(List.of()));
        CashClosingSummary summary = CashClosingSummary.from(List.of(
                operation("APPROVED", 999L, line("SERVICE", "Servico", 2, 2000L))
        ));
        assertEquals(List.of(), summary.items());
        assertEquals(List.of(new CashClosingSummary.Entry("Servico", 2, 999L)), summary.services());
    }

    private CashClosingResponse.Operation operation(
            String status, long amount, CashClosingResponse.Line... lines
    ) {
        return new CashClosingResponse.Operation(1L, "Mesa", null, "PIX", status,
                amount, 0L, 0L, List.of(lines));
    }

    private CashClosingResponse.Line line(String type, String name, int quantity, long total) {
        return new CashClosingResponse.Line(type, name, quantity, total / quantity, total);
    }
}
