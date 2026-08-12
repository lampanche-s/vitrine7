package br.com.vitrine7.print.service;

import br.com.vitrine7.bar.tab.dto.BarTabLineResponse;
import br.com.vitrine7.bar.tab.dto.BarTabResponse;
import br.com.vitrine7.common.config.BusinessProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

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
                "PAYMENT_PENDING",
                UUID.randomUUID(),
                "AWAITING_PAYMENT",
                2600L,
                0L,
                2600L,
                "GENERAL_RECEIPT",
                null,
                true,
                null,
                List.of(new BarTabLineResponse(
                        1L,
                        5L,
                        "ITEM",
                        "Espeto de carne",
                        1300L,
                        2,
                        2600L
                )),
                1L,
                null,
                null
        );

        String text = renderer.render(tab);

        assertTrue(text.contains("CONFERENCIA DE CONSUMO"));
        assertTrue(text.contains("NAO E DOCUMENTO FISCAL"));
        assertTrue(text.contains("COMANDA: Mesa 9"));
        assertTrue(text.contains("2x"));
        assertTrue(text.contains("Espeto de carne"));
        assertTrue(text.contains("TOTAL"));
    }
}
