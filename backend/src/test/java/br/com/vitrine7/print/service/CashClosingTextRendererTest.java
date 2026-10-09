package br.com.vitrine7.print.service;

import br.com.vitrine7.cashclosing.dto.CashClosingResponse;
import br.com.vitrine7.common.config.BusinessProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CashClosingTextRendererTest {

    private final CashClosingTextRenderer renderer = new CashClosingTextRenderer(
            new BusinessProperties("America/Bahia", null)
    );

    @Test
    void printsBothSectionsInOrderAndWrapsLongDescriptionsOnThermalPaper() {
        CashClosingResponse report = report(List.of(new CashClosingResponse.Operation(
                1L, "Mesa", null, "PIX", "APPROVED", 6500L, 0L, 0L,
                List.of(
                        new CashClosingResponse.Line("SERVICE", "Atendimento", 2, 2000L, 4000L),
                        new CashClosingResponse.Line("ITEM", "Produto com descricao longa para conferencia do caixa", 5, 500L, 2500L)
                )
        )));

        String text = renderer.renderSummary(report);

        assertTrue(text.contains("RESUMO DO CAIXA"));
        assertTrue(text.contains("09/10/2026 05:00"));
        assertTrue(text.contains("10/10/2026 04:59"));
        assertTrue(text.indexOf("\nITENS\n") < text.indexOf("\nSERVICOS\n"));
        assertTrue(text.contains("SUBTOTAL DE ITENS:"));
        assertTrue(text.contains("SUBTOTAL DE SERVICOS:"));
        assertTrue(text.contains("TOTAL GERAL:"));
        assertTrue(text.lines().anyMatch(line -> line.matches("SUBTOTAL DE ITENS:.*25,00")));
        assertTrue(text.lines().anyMatch(line -> line.matches("SUBTOTAL DE SERVICOS:.*40,00")));
        assertTrue(text.lines().anyMatch(line -> line.matches("TOTAL GERAL:.*65,00")));
        assertTrue(text.lines().allMatch(line -> line.length() <= 48), text);
        assertFalse(text.contains("OPERACOES DO DIA"));
        assertFalse(text.contains("descontos rateados"));
    }

    @Test
    void printsEmptySectionsAndExplainsDiscountedValues() {
        String empty = renderer.renderSummary(report(List.of()));
        assertTrue(empty.contains("Nenhum registro no periodo."));
        assertTrue(empty.contains("TOTAL GERAL:"));

        String discounted = renderer.renderSummary(report(List.of(new CashClosingResponse.Operation(
                1L, "Mesa", null, "PIX", "APPROVED", 900L, 0L, 0L,
                List.of(new CashClosingResponse.Line("ITEM", "Cafe", 1, 1000L, 1000L))
        ))));
        assertTrue(discounted.contains("Valores com descontos rateados."));
        assertTrue(discounted.lines().anyMatch(line -> line.matches("TOTAL GERAL:.*9,00")));
    }

    private CashClosingResponse report(List<CashClosingResponse.Operation> operations) {
        CashClosingResponse report = mock(CashClosingResponse.class);
        when(report.businessDate()).thenReturn(LocalDate.of(2026, 10, 9));
        when(report.userName()).thenReturn("Operador");
        when(report.operations()).thenReturn(operations);
        return report;
    }
}
