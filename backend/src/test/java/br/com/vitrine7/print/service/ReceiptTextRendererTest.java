package br.com.vitrine7.print.service;

import br.com.vitrine7.receipt.dto.ReceiptEstablishmentResponse;
import br.com.vitrine7.receipt.dto.ReceiptLineResponse;
import br.com.vitrine7.receipt.dto.ReceiptOperationResponse;
import br.com.vitrine7.receipt.dto.ReceiptPaymentResponse;
import br.com.vitrine7.receipt.dto.ReceiptResponse;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptTextRendererTest {

    private final ReceiptTextRenderer renderer = new ReceiptTextRenderer();

    @Test
    void rendersThermalReceiptWithExpectedFinancialData() {
        OffsetDateTime issuedAt = OffsetDateTime.parse("2026-08-06T20:00:00-03:00");

        ReceiptResponse receipt = new ReceiptResponse(
                "NON_FISCAL_THERMAL_80MM",
                "COMPROVANTE NAO FISCAL",
                "ESTE DOCUMENTO NAO E FISCAL",
                new ReceiptEstablishmentResponse(
                        "Vitrine 7",
                        null,
                        null,
                        "Rua Senhor do Bonfim, Monte Gordo, Camacari/BA"
                ),
                new ReceiptOperationResponse(
                        "BAR_COMMAND",
                        12L,
                        UUID.randomUUID(),
                        "Comanda 12",
                        "CLOSED",
                        1L,
                        "Operador"
                ),
                List.of(new ReceiptLineResponse(
                        "Espeto de carne",
                        "ITEM",
                        2,
                        1_000L,
                        2_000L
                )),
                2_000L,
                0L,
                2_000L,
                new ReceiptPaymentResponse(
                        UUID.randomUUID(),
                        "PIX",
                        "MANUAL",
                        "APPROVED",
                        2_000L,
                        issuedAt,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                issuedAt
        );

        String text = renderer.render(receipt);

        assertTrue(text.contains("DOC. NAO FISCAL"));
        assertTrue(text.contains("ESPETO DE CARNE") || text.contains("Espeto de carne"));
        assertTrue(text.contains("TOTAL:"));
        assertTrue(text.contains("FORMA DE PAGAMENTO: Pix"));
    }
}
