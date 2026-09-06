package br.com.vitrine7.print.service;

import br.com.vitrine7.bar.tab.dto.BarTabLineResponse;
import br.com.vitrine7.bar.tab.dto.BarTabResponse;
import br.com.vitrine7.common.config.BusinessProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PrePaymentNoteRendererTest {

    @Test
    void rendersSimpleConsumerConferenceBeforePayment() {
        PrePaymentNoteRenderer renderer = new PrePaymentNoteRenderer(
                new BusinessProperties(
                        "America/Bahia",
                        new BusinessProperties.Establishment("Vitrine 7", null, null, null)
                ),
                Clock.fixed(Instant.parse("2026-08-12T15:00:00Z"), ZoneOffset.UTC)
        );

        BarTabResponse tab = new BarTabResponse(
                9L,
                "Mesa 9",
                null,
                null,
                "OPEN",
                null,
                null,
                5000L,
                0L,
                5000L,
                "GENERAL_RECEIPT",
                null,
                true,
                null,
                null,
                "Onix prata",
                "ABC1D23",
                null,
                false,
                List.of(new BarTabLineResponse(
                        1L,
                        5L,
                        "ITEM",
                        "Espeto de carne",
                        1300L,
                        2,
                        2600L
                ), new BarTabLineResponse(
                        2L,
                        6L,
                        "SERVICE",
                        "Refrigerante",
                        1200L,
                        2,
                        2400L
                )),
                1L,
                null,
                null
        );

        String text = renderer.render(tab);

        assertTrue(text.contains("CONFERENCIA DE CONSUMO"));
        assertTrue(text.contains("NAO E DOCUMENTO FISCAL"));
        assertTrue(text.contains("COMANDA: Mesa 9"));
        assertTrue(text.contains("VEÍCULO: Onix prata"));
        assertTrue(text.contains("PLACA: ABC1D23"));
        assertTrue(text.contains("2x"));
        assertTrue(text.contains("Espeto de carne"));
        assertTrue(text.contains("Refrigerante"));
        assertTrue(text.contains("R$ 13,00"));
        assertTrue(text.contains("R$ 24,00"));
        assertTrue(text.contains("R$ 50,00"));
        assertTrue(text.contains("TOTAL"));
    }
}
