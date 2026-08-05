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
      | "LAVA_WORK_ORDER"
      | string;
    operationId: number;
    checkoutId: string;
    displayName: string;
    status: string;
    responsibleUserId: number | null;
    responsibleUserName: string | null;
  };

  customer: {
    name: string | null;
    phone: string | null;
  } | null;

  vehicle: {
    name: string | null;
    plate: string | null;
    size: string | null;
  } | null;

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

  payment: {
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
  };

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

    case "LAVA_WORK_ORDER":
      return "Ordem de serviço";

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
  payment: OfficialReceiptResponse["payment"]
): ReceiptLine[] {
  if (
    payment.status ===
    "REVERSAL_PENDING"
  ) {
    return [
      {
        label: "Pagamento",
        value: "Estorno pendente",
      },
    ];
  }

  if (payment.status !== "REVERSED") {
    return [];
  }

  return [
    {
      label: "Pagamento",
      value: "Estornado",
    },

    ...optionalLine(
      "Estornado em",
      payment.reversedAt
        ? formatDateTime(
            payment.reversedAt
          )
        : null
    ),

    ...optionalLine(
      "Motivo",
      payment.reversalReason
    ),
  ];
}

export function mapOfficialReceipt(
  response: OfficialReceiptResponse
): ReceiptDocument {
  const lines: ReceiptLine[] = [
    {
      label: "Tipo",
      value: operationLabel(response.operation.type),
    },

    ...optionalLine(
      "Responsável",
      response.operation.responsibleUserName
    ),

    ...optionalLine("Cliente", response.customer?.name),

    ...optionalLine("Telefone", response.customer?.phone),

    ...optionalLine("Veículo", response.vehicle?.name),

    ...optionalLine("Placa", response.vehicle?.plate),

    ...optionalLine("Porte", response.vehicle?.size),

    ...paymentReversalLines(
      response.payment
    ),
  ];

  return {
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
    payment: paymentLabel(response.payment.method),
    document: "Recibo geral",
    issuedAt: formatDateTime(response.issuedAt),
    paidAmount:
      response.payment.cashReceivedCents === null
        ? undefined
        : formatBrlCurrency(
            response.payment.cashReceivedCents / 100
          ),
    changeAmount:
      response.payment.cashChangeCents === null
        ? undefined
        : formatBrlCurrency(
            response.payment.cashChangeCents / 100
          ),
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
