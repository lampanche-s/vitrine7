import {
  describe,
  expect,
  it,
} from "vitest";

import {
  createReceiptPdf,
  createReceiptPrintHtml,
  type ReceiptDocument,
} from "./receiptDocument";

function getPageHeight(pdf: Awaited<ReturnType<typeof createReceiptPdf>>) {
  return pdf.internal.pageSize.getHeight();
}

describe("receiptDocument", () => {
  it("mantém largura térmica de 80 mm e altura dinâmica sem compactar artificialmente", async () => {
    const baseReceipt: ReceiptDocument = {
      title: "Comanda 7",
      amount: "R$ 22,00",
      payment: "Dinheiro",
      issuedAt: "18/07/2026, 12:30",
      lines: [
        {
          label: "Tipo",
          value: "Comanda",
        },
      ],
      items: [
        {
          quantity: 2,
          name: "Espeto bovino",
          unitPrice: 8,
          total: 16,
        },
      ],
    };

    const longReceipt: ReceiptDocument = {
      ...baseReceipt,
      items: Array.from({
        length: 18,
      }).map((_, index) => ({
        quantity: index + 1,
        name: `Item nominal ${index + 1}`,
        unitPrice: 6,
        total: (index + 1) * 6,
      })),
    };

    const shortPdf = await createReceiptPdf(baseReceipt);
    const longPdf = await createReceiptPdf(longReceipt);

    expect(shortPdf.internal.pageSize.getWidth()).toBe(80);
    expect(getPageHeight(longPdf)).toBeGreaterThan(
      getPageHeight(shortPdf)
    );
  });

  it("gera template HTML térmico com blocos protegidos para impressão", () => {
    const receipt: ReceiptDocument = {
      title: "Comanda 7",
      amount: "R$ 25,00",
      payment: "Pix",
      paidAmount: "R$ 25,00",
      issuedAt: "18/07/2026, 12:30",
      lines: [
        {
          label: "Cliente",
          value: "Ana & Bruno <teste>",
        },
      ],
      items: [
        {
          quantity: 1,
          name: "Produto grande",
          unitPrice: 25,
          total: 25,
        },
      ],
    };

    const html = createReceiptPrintHtml(receipt);

    expect(html).toContain(
      'class="thermal-receipt"'
    );
    expect(html).toContain(
      'class="thermal-receipt__content"'
    );
    expect(html).toContain("Ana &amp; Bruno &lt;teste&gt;");
    expect(html).toContain(
      "Vitrine 7 Estética Automotiva e Espeto Bar"
    );
    expect(html).toContain(
      "DOC. NÃO FISCAL"
    );
    expect(html).toContain(
      "COD DESCRIÇÃO"
    );
    expect(html).toContain(
      "FORMA DE PAGAMENTO: Pix"
    );
    expect(html).toContain(
      "Obrigado pela preferência!"
    );
    expect(html).toContain(
      "Volte Sempre / Vitrine 7"
    );
    expect(html).not.toContain(
      "Nao substitui documento fiscal"
    );
  });
});
