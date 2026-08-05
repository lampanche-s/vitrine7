import type {
  ReceiptDocument,
} from "./receiptDocument";

export const RECEIPT_COLUMNS = 48;
export const RECEIPT_HEADER_COLUMNS = RECEIPT_COLUMNS;
export const RECEIPT_SEPARATOR = "=".repeat(RECEIPT_COLUMNS);
export const RECEIPT_DIVIDER = "-".repeat(RECEIPT_COLUMNS);
export const RECEIPT_FONT_STACK =
  'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "DejaVu Sans Mono", monospace';

function normalizeText(value: string) {
  return value.replace(/\s+/g, " ").trim();
}

function normalizeCurrency(value: string) {
  return value.replace(/\u00a0/g, " ").trim();
}

function wrapText(value: string, width: number) {
  const normalized = normalizeText(value);

  if (!normalized) {
    return [];
  }

  const lines: string[] = [];
  let current = "";

  normalized.split(" ").forEach((word) => {
    if (word.length > width) {
      if (current) {
        lines.push(current);
        current = "";
      }

      for (let index = 0; index < word.length; index += width) {
        lines.push(word.slice(index, index + width));
      }

      return;
    }

    const candidate = current ? `${current} ${word}` : word;

    if (candidate.length > width && current) {
      lines.push(current);
      current = word;
      return;
    }

    current = candidate;
  });

  if (current) {
    lines.push(current);
  }

  return lines;
}

function centerText(value: string, width = RECEIPT_COLUMNS) {
  return wrapText(value, width).map((line) =>
    `${" ".repeat(
      Math.max(0, Math.floor((width - line.length) / 2))
    )}${line}`
  );
}

function left(value: string, width: number) {
  const normalized = normalizeText(value);

  return normalized.length <= width
    ? normalized.padEnd(width)
    : normalized.slice(0, width);
}

function right(value: string, width: number) {
  const normalized = normalizeText(value);

  return normalized.length <= width
    ? normalized.padStart(width)
    : normalized;
}

function labelValueLines(label: string, value: string) {
  const prefix = `${normalizeText(label).toUpperCase()}: `;
  const availableWidth = Math.max(
    12,
    RECEIPT_COLUMNS - prefix.length
  );
  const wrapped = wrapText(value, availableWidth);

  return wrapped.map((line, index) =>
    index === 0
      ? `${prefix}${line}`
      : `${" ".repeat(prefix.length)}${line}`
  );
}

function amountLine(label: string, value: string) {
  const normalizedLabel =
    `${normalizeText(label).toUpperCase()}:`;
  const normalizedValue = normalizeCurrency(value);
  const spaces = Math.max(
    1,
    RECEIPT_COLUMNS -
      normalizedLabel.length -
      normalizedValue.length
  );

  return `${normalizedLabel}${" ".repeat(
    spaces
  )}${normalizedValue}`;
}

function formatItemValue(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

function hasUsefulDescription(receipt: ReceiptDocument) {
  const description = receipt.description?.trim();

  if (!description) {
    return false;
  }

  if (
    /^\d+\s+itens?,\s+\d+\s+unidades?$/i.test(
      description
    )
  ) {
    return false;
  }

  return !receipt.items?.some(
    (item) =>
      description.toLowerCase() ===
      `${item.quantity}x ${item.name}`.toLowerCase()
  );
}

export function buildReceiptTextLines(
  receipt: ReceiptDocument
) {
  const lines: string[] = [RECEIPT_SEPARATOR];

  lines.push(
    ...centerText(
      receipt.establishmentName ??
        "Vitrine 7 Estética Automotiva e Espeto Bar",
      RECEIPT_HEADER_COLUMNS
    )
  );

  lines.push(
    ...centerText(
      receipt.establishmentAddress ??
        "Rua Senhor do Bonfim, Monte Gordo, Camaçari/BA.",
      RECEIPT_HEADER_COLUMNS
    )
  );

  lines.push(RECEIPT_SEPARATOR);

  const documentLine = [
    "DOC. NÃO FISCAL",
    receipt.issuedAt ?? "DATA NÃO INFORMADA",
    `CUPOM: ${receipt.code ?? "----"}`,
  ].join(" | ");

  if (documentLine.length <= RECEIPT_COLUMNS) {
    lines.push(documentLine);
  } else {
    lines.push(
      `DOC. NÃO FISCAL | ${
        receipt.issuedAt ?? "DATA NÃO INFORMADA"
      }`
    );

    lines.push(
      `CUPOM: ${receipt.code ?? "----"}`.padStart(
        RECEIPT_COLUMNS
      )
    );
  }

  const detailLines = receipt.lines.length
    ? receipt.lines
    : [
        {
          label: "Operação",
          value: receipt.title,
        },
      ];

  lines.push(RECEIPT_DIVIDER);

  detailLines.forEach((line) => {
    lines.push(
      ...labelValueLines(line.label, line.value)
    );
  });

  lines.push(RECEIPT_DIVIDER);

  lines.push(
    [
      left("COD", 3),
      left("DESCRIÇÃO", 23),
      right("QTD", 3),
      right("VL.UN", 7),
      right("VL.TOT", 8),
    ].join(" ")
  );

  lines.push(RECEIPT_DIVIDER);

  receipt.items?.forEach((item, index) => {
    const descriptions = wrapText(item.name, 23);

    lines.push(
      [
        left(
          String(index + 1).padStart(3, "0"),
          3
        ),
        left(descriptions[0] ?? "-", 23),
        right(String(item.quantity), 3),
        right(formatItemValue(item.unitPrice), 7),
        right(formatItemValue(item.total), 8),
      ].join(" ")
    );

    descriptions
      .slice(1)
      .forEach((description) => {
        lines.push(
          [
            " ".repeat(3),
            left(description, 23),
            " ".repeat(3),
            " ".repeat(7),
            " ".repeat(8),
          ].join(" ")
        );
      });
  });

  lines.push(RECEIPT_DIVIDER);

  if (receipt.subtotal) {
    lines.push(
      amountLine("Subtotal", receipt.subtotal)
    );
  }

  if (receipt.discount) {
    lines.push(
      amountLine("Desconto", receipt.discount)
    );
  }

  lines.push(amountLine("Total", receipt.amount));
  lines.push(RECEIPT_DIVIDER);

  if (receipt.payment) {
    lines.push(
      ...labelValueLines(
        "Forma de pagamento",
        receipt.payment
      )
    );
  }

  if (receipt.paidAmount) {
    lines.push(
      amountLine(
        "Valor recebido",
        receipt.paidAmount
      )
    );
  }

  if (receipt.changeAmount) {
    lines.push(
      amountLine("Troco", receipt.changeAmount)
    );
  }

  if (hasUsefulDescription(receipt)) {
    lines.push(RECEIPT_DIVIDER);

    lines.push(
      ...labelValueLines(
        "Observação",
        receipt.description ?? ""
      )
    );
  }

  lines.push(RECEIPT_DIVIDER);
  lines.push(
    ...centerText("Obrigado pela preferência!")
  );
  lines.push(
    ...centerText("Volte Sempre / Vitrine 7")
  );
  lines.push(RECEIPT_SEPARATOR);

  return lines;
}
