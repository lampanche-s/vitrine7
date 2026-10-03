package br.com.vitrine7.print.service;

import br.com.vitrine7.bar.tab.dto.BarTabLineResponse;
import br.com.vitrine7.bar.tab.dto.BarTabResponse;
import br.com.vitrine7.common.config.BusinessProperties;
import br.com.vitrine7.catalog.entity.CatalogEntryType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalOrderRendererTest {

    private final OperationalOrderRenderer renderer =
            new OperationalOrderRenderer(new BusinessProperties(
                    "America/Bahia",
                    new BusinessProperties.Establishment("Vitrine 7", null, null, null)
            ));

    @Test
    void rendersOnlyAddedQuantityWithOperatorTimeAndAddedValue() {
        String text = renderer.renderAddedEntry(
                "Mesa 4", CatalogEntryType.ITEM, "Coca-Cola", 2, 1234L, "Romario",
                OffsetDateTime.parse("2026-10-03T23:15:00Z")
        );

        assertTrue(text.contains("COMANDA: Mesa 4"));
        assertTrue(text.contains("DATA: 03/10/2026 20:15"));
        assertTrue(text.contains("OPERADOR: Romario"));
        assertTrue(text.contains("TIPO: ITEM"));
        assertTrue(text.contains("2x COCA-COLA"));
        assertTrue(text.contains("VALOR: R$"));
        assertTrue(text.contains("24,68"));
    }

    @Test
    void rendersAddedServiceWithExplicitTypeAndIncrementalValue() {
        String text = renderer.renderAddedEntry(
                "Mesa 4", CatalogEntryType.SERVICE, "Lavagem completa", 1,
                35000L, "Will", OffsetDateTime.parse("2026-10-03T23:15:00Z")
        );

        assertTrue(text.startsWith("PEDIDO - SERVIÇO\n"));
        assertTrue(text.contains("TIPO: SERVIÇO"));
        assertTrue(text.contains("1x LAVAGEM COMPLETA"));
        assertTrue(text.contains("OPERADOR: Will"));
        assertTrue(text.contains("350,00"));
    }

    @Test
    void rendersAllItemsWithoutPrices() {
        BarTabLineResponse cola = line(1L, "ITEM", "Coca-Cola", 2);
        BarTabLineResponse water = line(2L, "ITEM", "Água", 3);
        String text = renderer.renderItems(tab(cola, water), List.of(cola, water));

        assertTrue(text.startsWith("PEDIDO - ITENS\n"));
        assertTrue(text.contains("COMANDA:\njose acerto com cibele"));
        assertTrue(text.contains("2x COCA-COLA"));
        assertTrue(text.contains("3x ÁGUA"));
        assertFalse(text.contains("VEÍCULO:"));
        assertFalse(text.contains("PLACA:"));
        assertFalse(text.contains("12,34"));
        assertFalse(text.contains("R$"));
    }

    @Test
    void rendersAllServices() {
        BarTabLineResponse wash = line(3L, "SERVICE", "Lavagem completa", 1);
        BarTabLineResponse vacuum = line(4L, "SERVICE", "Aspiração", 2);
        String text = renderer.renderServices(tab(wash, vacuum), List.of(wash, vacuum));

        assertTrue(text.startsWith("PEDIDO - SERVIÇOS\n"));
        assertTrue(text.contains("VEÍCULO: Onix prata"));
        assertTrue(text.contains("PLACA: ABC1D23"));
        assertTrue(text.contains("1x LAVAGEM COMPLETA"));
        assertTrue(text.contains("2x ASPIRAÇÃO"));
    }

    @Test
    void rendersSingleItem() {
        BarTabLineResponse cola = line(1L, "ITEM", "Coca-Cola", 3);
        String text = renderer.renderSingleItem(tab(cola), cola);

        assertTrue(text.startsWith("PEDIDO - ITEM\n"));
        assertTrue(text.contains("3x COCA-COLA"));
    }

    @Test
    void rendersSingleService() {
        BarTabLineResponse wash = line(3L, "SERVICE", "Lavagem completa", 1);
        String text = renderer.renderSingleService(tab(wash), wash);

        assertTrue(text.startsWith("PEDIDO - SERVIÇO\n"));
        assertTrue(text.contains("VEÍCULO: Onix prata"));
        assertTrue(text.contains("PLACA: ABC1D23"));
        assertTrue(text.contains("1x LAVAGEM COMPLETA"));
    }

    private BarTabLineResponse line(
            Long id,
            String type,
            String name,
            int quantity
    ) {
        return new BarTabLineResponse(
                id, id + 100, type, name, 1234L, quantity,
                1234L * quantity
        );
    }

    private BarTabResponse tab(BarTabLineResponse... lines) {
        return new BarTabResponse(
                44L, "jose acerto com cibele", null, null, "OPEN",
                null, null, 0L, 0L, 0L, null, null, false,
                null, null, "Onix prata", "ABC1D23", null, false, List.of(lines), 9L, null, null
        );
    }
}
