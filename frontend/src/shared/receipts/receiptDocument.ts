import type {
  jsPDF as JsPdfDocument,
} from "jspdf";

import {
  buildReceiptTextLines,
  RECEIPT_FONT_STACK,
  RECEIPT_SEPARATOR,
} from "./receiptTextLayout";

export type ReceiptLine = {
  label: string;
  value: string;
};

export type ReceiptItemLine = {
  quantity: number;
  name: string;
  unitPrice: number;
  total: number;
};

export type ReceiptDocument = {
  checkoutId?: string;
  establishmentName?: string;
  establishmentAddress?: string;
  nonFiscalNotice?: string;
  title: string;
  subtitle?: string;
  code?: string;
  amount: string;
  subtotal?: string;
  discount?: string;
  status?: string;
  payment?: string;
  document?: string;
  issuedAt?: string;
  paidAmount?: string;
  changeAmount?: string;
  description?: string;
  lines: ReceiptLine[];
  items?: ReceiptItemLine[];
};

const receiptWidthMm = 80;
const receiptMarginMm = 3;
const receiptLineHeightMm = 3.25;

function normalizeFileName(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-zA-Z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .toLowerCase();
}

function isBoldReceiptLine(line: string) {
  return (
    line === RECEIPT_SEPARATOR ||
    line.startsWith("TOTAL:") ||
    line.includes(
      "Vitrine 7 Estética Automotiva"
    )
  );
}

function estimateReceiptHeight(
  receipt: ReceiptDocument
) {
  return Math.max(
    86,
    10 +
      buildReceiptTextLines(receipt).length *
        receiptLineHeightMm
  );
}

function setPdfFont(
  pdf: JsPdfDocument,
  weight: "normal" | "bold"
) {
  pdf.setFont("courier", weight);
}

export async function createReceiptPdf(
  receipt: ReceiptDocument
) {
  const {
    jsPDF,
  } = await import("jspdf");

  const pageHeight =
    estimateReceiptHeight(receipt);

  const pdf = new jsPDF({
    orientation: "portrait",
    unit: "mm",
    format: [receiptWidthMm, pageHeight],
  });

  pdf.setFillColor(255, 255, 255);
  pdf.rect(
    0,
    0,
    receiptWidthMm,
    pageHeight,
    "F"
  );

  pdf.setTextColor(0, 0, 0);
  pdf.setFontSize(6.2);

  let y = 6;

  buildReceiptTextLines(receipt).forEach(
    (line) => {
      setPdfFont(
        pdf,
        isBoldReceiptLine(line)
          ? "bold"
          : "normal"
      );

      pdf.text(
        line,
        receiptMarginMm,
        y
      );

      y += receiptLineHeightMm;
    }
  );

  return pdf;
}

export async function createReceiptPdfUrl(
  receipt: ReceiptDocument
) {
  const pdf = await createReceiptPdf(receipt);
  const blob = pdf.output("blob");

  return URL.createObjectURL(blob);
}

export async function downloadReceiptPdf(
  receipt: ReceiptDocument
) {
  const pdf = await createReceiptPdf(receipt);
  const fileName =
    normalizeFileName(receipt.title) ||
    "vitrine-7";

  pdf.save(`comprovante-${fileName}.pdf`);
}

export function createReceiptPreviewImage(
  receipt: ReceiptDocument
) {
  const canvas =
    document.createElement("canvas");
  const lines =
    buildReceiptTextLines(receipt);

  const width = 302;
  const horizontalPadding = 12;
  const verticalPadding = 16;
  const lineHeight = 12;

  canvas.width = width;
  canvas.height = Math.max(
    360,
    verticalPadding * 2 +
      lines.length * lineHeight
  );

  const context = canvas.getContext("2d");

  if (!context) {
    return null;
  }

  context.fillStyle = "#FFFFFF";
  context.fillRect(
    0,
    0,
    canvas.width,
    canvas.height
  );

  context.fillStyle = "#000000";
  context.textAlign = "left";
  context.textBaseline = "top";

  let fontSize = 9;

  do {
    context.font =
      `400 ${fontSize}px ${RECEIPT_FONT_STACK}`;

    fontSize -= 0.25;
  } while (
    Math.max(
      ...lines.map(
        (line) =>
          context.measureText(line).width
      )
    ) >
      width - horizontalPadding * 2 &&
    fontSize >= 7.5
  );

  const finalFontSize = fontSize + 0.25;
  let y = verticalPadding;

  lines.forEach((line) => {
    context.font = `${
      isBoldReceiptLine(line)
        ? 700
        : 400
    } ${finalFontSize}px ${RECEIPT_FONT_STACK}`;

    context.fillText(
      line,
      horizontalPadding,
      y
    );

    y += lineHeight;
  });

  return canvas.toDataURL("image/png");
}
