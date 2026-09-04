import {
  formatBrlCurrency,
} from "../lib/currency";

import {
  httpClient,
} from "../http";

import type {
  ReceiptDocument,
  ReceiptLine,
} from "./receiptDocument";

export type OfficialReceiptResponse = {
  receiptType: string;
  title: string;
  nonFiscalNotice: string;

  establishment: {
    name: string | null;
    document: string | null;
    phone: string | null;
    address: string | null;
  };

  operation: {
    type:
      | "BAR_COMMAND"
      | string;
    operationId: number;
    checkoutId: string;
    displayName: string;
    status: string;
    responsibleUserId: number | null;
    responsibleUserName: string | null;
    vehicleName: string | null;
    vehiclePlate: string | null;
  };

  lines: {
    description: string;
    category: string | null;
    quantity: number;
    unitPriceCents: number;
    totalCents: number;
  }[];

  subtotalCents: number;
  discountCents: number;
  totalCents: number;

  payments: {
    paymentId: string;
    method: string;
    processingMode: string;
    status: string;
    approvedAmountCents: number;
    approvedAt: string;
    cashReceivedCents: number | null;
    cashChangeCents: number | null;
    terminalProvider: string | null;
    reversedAt: string | null;
    reversalReason: string | null;
  }[];

  issuedAt: string;
};

function paymentLabel(method: string): string {
  switch (method) {
    case "CASH":
      return "Dinheiro";

    case "PIX":
      return "Pix";

    case "CREDIT":
    case "CREDIT_CARD":
      return "Crédito";

    case "DEBIT":
    case "DEBIT_CARD":
      return "Débito";

    default:
      return method;
  }
}

function operationLabel(type: string): string {
  switch (type) {
    case "BAR_COMMAND":
      return "Comanda";

    default:
      return type;
  }
}

function formatDateTime(value: string): string {
  return new Date(value)
    .toLocaleString("pt-BR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    })
    .replace(",", "");
}

function optionalLine(
  label: string,
  value: string | null | undefined
): ReceiptLine[] {
  if (!value?.trim()) {
    return [];
  }

  return [
    {
      label,
      value,
    },
  ];
}

function paymentReversalLines(
  payments: OfficialReceiptResponse["payments"]
): ReceiptLine[] {
  if (payments.length === 0 || !payments.every((payment) => payment.status === "REVERSED")) {
    return [];
  }

  const latest = [...payments].sort((left, right) =>
    String(right.reversedAt ?? "").localeCompare(String(left.reversedAt ?? ""))
  )[0];

  return [
    { label: "Pagamento", value: "Estornado" },
    ...optionalLine(
      "Estornado em",
      latest?.reversedAt ? formatDateTime(latest.reversedAt) : null
    ),
    ...optionalLine("Motivo", latest?.reversalReason),
  ];
}

export function mapOfficialReceipt(
  response: OfficialReceiptResponse
): ReceiptDocument {
  const payments = response.payments ?? [];
  const cashReceivedCents = payments.reduce(
    (sum, payment) => sum + (payment.cashReceivedCents ?? 0),
    0
  );
  const cashChangeCents = payments.reduce(
    (sum, payment) => sum + (payment.cashChangeCents ?? 0),
    0
  );
  const paymentDescription = payments.length <= 1
    ? paymentLabel(payments[0]?.method ?? "")
    : payments
        .map((payment) => `${paymentLabel(payment.method)} ${formatBrlCurrency(payment.approvedAmountCents / 100)}`)
        .join(" + ");

  const lines: ReceiptLine[] = [
    {
      label: "Tipo",
      value: operationLabel(response.operation.type),
    },

    ...optionalLine(
      "Responsável",
      response.operation.responsibleUserName
    ),

    ...optionalLine("Veículo", response.operation.vehicleName),
    ...optionalLine("Placa", response.operation.vehiclePlate),


    ...paymentReversalLines(payments),
  ];

  return {
    checkoutId: response.operation.checkoutId,
    establishmentName:
      response.establishment.name ??
      "Vitrine 7 Estética Automotiva e Espeto Bar",
    establishmentAddress:
      response.establishment.address ??
      "Rua Senhor do Bonfim, Monte Gordo, Camaçari/BA.",
    nonFiscalNotice: response.nonFiscalNotice,
    title: response.operation.displayName,
    code: String(
      response.operation.operationId
    ).padStart(4, "0"),
    amount: formatBrlCurrency(response.totalCents / 100),
    subtotal: formatBrlCurrency(response.subtotalCents / 100),
    discount: formatBrlCurrency(response.discountCents / 100),
    payment: paymentDescription,
    document: "Recibo geral",
    issuedAt: formatDateTime(response.issuedAt),
    paidAmount:
      cashReceivedCents <= 0
        ? undefined
        : formatBrlCurrency(cashReceivedCents / 100),
    changeAmount:
      cashChangeCents <= 0
        ? undefined
        : formatBrlCurrency(cashChangeCents / 100),
    items: response.lines.map((item) => ({
      quantity: item.quantity,
      name: item.description,
      unitPrice: item.unitPriceCents / 100,
      total: item.totalCents / 100,
    })),
    lines,
  };
}

export async function loadOfficialReceipt(
  checkoutId: string
): Promise<ReceiptDocument> {
  const response =
    await httpClient.get<OfficialReceiptResponse>(
      `/checkouts/${checkoutId}/receipt`
    );

  return mapOfficialReceipt(response);
}
