import type {
  BarCatalogItem,
} from "../../entities/catalog-item";

import type {
  AddBarCommandItemInput,
  BarCommand,
  BarCommandStatus,
  CloseBarCommandInput,
  OpenBarCommandInput,
} from "../../entities/command";

import {
  getBarCommandItemKey,
} from "../../entities/command";

import type {
  BarPaymentMethod,
  BarHistoricalPaymentMethod,
  BarReceiptDocument,
  BarSaleHistoryEntry,
} from "../../entities/sale-history";

import {
  httpClient,
} from "../../shared/http";

import {
  listAllPages,
} from "./listAllPages";

import {
  cancelCheckout,
  createIdempotencyKey,
  headersForIdempotency,
  runCheckoutPayment,
  type BackendCheckoutPaymentMethod,
} from "./checkoutPayments";

import type {
  BarRepository,
  BarRepositorySnapshot,
  CloseBarCommandRepositoryResult,
} from "../contracts";

type PageResponse<T> = {
  items: T[];
  page: number;
  totalPages: number;
  totalElements?: number;
};

type CatalogEntryResponse = {
  id: number;
  name: string;
  type: "ITEM" | "SERVICE";
  priceCents: number;
  stockQuantity: number | null;
  minimumStockQuantity: number | null;
  createdAt: string;
  updatedAt: string;
};

type BarTabLineResponse = {
  id: number;
  catalogEntryId: number;
  entryType: "ITEM" | "SERVICE";
  itemName: string;
  unitPriceCents: number;
  quantity: number;
  lineTotalCents: number;
};

type BarTabResponse = {
  id: number;
  name: string;
  status: "OPEN" | "PAYMENT_PENDING" | "CLOSED" | "CANCELLED";
  checkoutId: string | null;
  checkoutStatus: string | null;
  totalCents: number;
  lines: BarTabLineResponse[];
  createdAt: string;
};

type BarHistoryResponse = {
  operationId: number;
  checkoutId: string;
  displayName: string;
  operationalStatus: string;
  checkoutStatus: string | null;
  totalCents: number;
  documentType: string | null;
  paymentMethod: "CASH" | "PIX" | "CREDIT" | "DEBIT" | null;
  paymentStatus: string | null;
  paymentId: string | null;
  paymentReversedAt: string | null;
  paymentReversalReason: string | null;
  cashReceivedCents: number | null;
  cashChangeCents: number | null;
  lineCount: number;
  totalUnits: number;
  finishedAt: string;
};

function centsFromAmount(value: number | undefined) {
  return Math.round((value ?? 0) * 100);
}

function documentToBackend() {
  return "GENERAL_RECEIPT" as const;
}

function paymentMethodToBackend(
  method: BarPaymentMethod
): BackendCheckoutPaymentMethod {
  if (method === "Crédito" || method === "Cartão") {
    return "CREDIT_CARD";
  }

  if (method === "Débito") {
    return "DEBIT_CARD";
  }

  if (method === "Pix") {
    return "PIX";
  }

  return "CASH";
}

function paymentMethodFromHistory(
  method: BarHistoryResponse["paymentMethod"]
): BarHistoricalPaymentMethod {
  if (method === "PIX") {
    return "Pix";
  }

  if (method === "CREDIT") {
    return "Crédito";
  }

  if (method === "DEBIT") {
    return "Débito";
  }

  if (method === "CASH") {
    return "Dinheiro";
  }

  return "Cartão";
}

function documentFromBackend(): BarReceiptDocument {
  return "Recibo geral";
}

function formatHistoryTime(value: string): string {
  return new Date(value).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function paymentHistoryStatus(
  status: string | null
): "Concluída" | "Estornada" {
  if (status === "REVERSED") {
    return "Estornada";
  }

  return "Concluída";
}

function isCompletedHistoryEntry(entry: BarHistoryResponse): boolean {
  return (
    entry.operationalStatus === "CLOSED" &&
    entry.checkoutStatus === "FINALIZED" &&
    [
      "APPROVED",
      "REVERSED",
    ].includes(
      entry.paymentStatus ?? ""
    )
  );
}

function mapHistoryEntry(entry: BarHistoryResponse): BarSaleHistoryEntry {
  const units =
    entry.totalUnits === 1
      ? "1 unidade"
      : `${entry.totalUnits} unidades`;

  const lines =
    entry.lineCount === 1
      ? "1 item"
      : `${entry.lineCount} itens`;

  return {
    id: entry.operationId,
    checkoutId: entry.checkoutId,
    paymentId: entry.paymentId,
    paymentStatus: entry.paymentStatus,
    paymentReversedAt:
      entry.paymentReversedAt,
    paymentReversalReason:
      entry.paymentReversalReason,
    origin: `Comanda ${entry.displayName}`,
    description: `${lines}, ${units}`,
    amount: entry.totalCents / 100,
    method: paymentMethodFromHistory(entry.paymentMethod),
    document: documentFromBackend(),
    cashReceived:
      entry.cashReceivedCents === null
        ? null
        : entry.cashReceivedCents / 100,
    cashChange:
      entry.cashChangeCents === null
        ? null
        : entry.cashChangeCents / 100,
    status: paymentHistoryStatus(
      entry.paymentStatus
    ),
    time: formatHistoryTime(entry.finishedAt),
    completedAt: entry.finishedAt,
  };
}

function mapCatalogEntry(
  entry: CatalogEntryResponse
): BarCatalogItem {
  return {
    id: entry.id,
    name: entry.name,
    type: entry.type,
    price: entry.priceCents / 100,
    stockQuantity: entry.stockQuantity,
    minimumStockQuantity:
      entry.minimumStockQuantity,
  };
}

function mapTab(tab: BarTabResponse): BarCommand {
  return {
    id: tab.id,
    name: tab.name,
    status:
      tab.status === "PAYMENT_PENDING"
        ? "awaitingPayment"
        : "open",
    checkoutId: tab.checkoutId,
    checkoutStatus: tab.checkoutStatus,
    openedAt: new Date(tab.createdAt).toLocaleTimeString("pt-BR", {
      hour: "2-digit",
      minute: "2-digit",
    }),
    items: tab.lines.map((line) => {
      const catalogItemId =
        line.catalogEntryId;

      return {
        key: getBarCommandItemKey(
          catalogItemId
        ),
        catalogItemId,
        name: line.itemName,
        unitPrice:
          line.unitPriceCents / 100,
        quantity: line.quantity,
      };
    }),
  };
}

async function listAll<T>(path: string): Promise<T[]> {
  return listAllPages(
    (page) =>
      httpClient.get<PageResponse<T>>(
        `${path}?page=${page}&size=100`
      )
  );
}

async function refreshCatalogEntries():
Promise<BarCatalogItem[]> {
  const entries =
    await listAll<CatalogEntryResponse>(
      "/catalog"
    );

  return entries.map(
    mapCatalogEntry
  );
}

function catalogEntryPayload(input: {
  name: string;
  type: "ITEM" | "SERVICE";
  price: number;
  stockQuantity: number | null;
  minimumStockQuantity: number | null;
}) {
  return {
    name: input.name.trim(),
    type: input.type,
    priceCents:
      Math.round(input.price * 100),
    stockQuantity:
      input.type === "ITEM"
        ? input.stockQuantity
        : null,
    minimumStockQuantity:
      input.type === "ITEM"
        ? input.minimumStockQuantity
        : null,
  };
}

async function listAllTabsByStatus(
  status: "OPEN" | "PAYMENT_PENDING"
): Promise<BarTabResponse[]> {
  return listAllPages(
    (page) =>
      httpClient.get<
        PageResponse<BarTabResponse>
      >(
        `/bar/tabs?page=${page}&size=100&status=${status}`
      )
  );
}

async function refreshCommands(): Promise<BarCommand[]> {
  const [
    openTabs,
    pendingTabs,
  ] = await Promise.all([
    listAllTabsByStatus("OPEN"),
    listAllTabsByStatus(
      "PAYMENT_PENDING"
    ),
  ]);

  return [
    ...openTabs,
    ...pendingTabs,
  ].map(mapTab);
}

async function listHistoryPage(input: {
  page: number;
  size: number;
  search?: string;
  from?: string;
  to?: string;
}) {
  const params = new URLSearchParams({
    page: String(input.page),
    size: String(input.size),
  });

  const search = input.search?.trim();

  if (search) {
    params.set("search", search);
  }

  if (input.from) {
    params.set("from", input.from);
  }

  if (input.to) {
    params.set("to", input.to);
  }

  const response =
    await httpClient.get<PageResponse<BarHistoryResponse>>(
      `/bar/history?${params.toString()}`
    );

  return {
    entries: response.items
      .filter(isCompletedHistoryEntry)
      .map(mapHistoryEntry),
    page: response.page,
    totalPages: response.totalPages,
    totalElements:
      response.totalElements ?? response.items.length,
  };
}

async function runTerminalPayment(
  checkoutId: string,
  payment: BarPaymentMethod
) {
  const method = paymentMethodToBackend(payment);

  const result =
    await runCheckoutPayment({
      checkoutId,
      processing: "terminal",
      method,
    });

  if (
    result.payment.status !== "APPROVED" ||
    result.checkout.status !== "FINALIZED"
  ) {
    const message =
      result.terminalTransaction.responseMessage ??
      result.terminalTransaction.errorMessage ??
      "Pagamento recusado pela maquininha.";

    throw new Error(message);
  }

  return result;
}

async function runPixPayment(
  checkoutId: string,
) {
  const result =
    await runCheckoutPayment({
      checkoutId,
      processing: "pix",
      method: "PIX",
    });

  if (
    result.payment.status !== "APPROVED" ||
    result.checkout.status !== "FINALIZED"
  ) {
    throw new Error(
      "Pagamento Pix não aprovado."
    );
  }

  return result;
}

async function runCashPayment(
  checkoutId: string,
  cashReceivedCents: number
) {
  const result =
    await runCheckoutPayment({
      checkoutId,
      processing: "cash",
      method: "CASH",
      cashReceivedCents,
    });

  if (
    result.payment.status !== "APPROVED" ||
    result.checkout.status !== "FINALIZED"
  ) {
    throw new Error(
      "Pagamento em dinheiro não aprovado."
    );
  }

  return result;
}

async function runPayment(
  checkoutId: string,
  payment: BarPaymentMethod,
  amountCents: number,
  cashReceived?: number
) {
  const method = paymentMethodToBackend(payment);

  if (method === "CASH") {
    return runCashPayment(
      checkoutId,
      cashReceived === undefined
        ? amountCents
        : centsFromAmount(cashReceived)
    );
  }

  if (method === "PIX") {
    return runPixPayment(checkoutId);
  }

  return runTerminalPayment(checkoutId, payment);
}

async function getTab(tabId: number) {
  return httpClient.get<BarTabResponse>(`/bar/tabs/${tabId}`);
}

export const httpBarRepository: BarRepository = {
  async getSnapshot():
  Promise<BarRepositorySnapshot> {
    const [
      catalogEntries,
      commands,
    ] = await Promise.all([
      refreshCatalogEntries(),
      refreshCommands(),
    ]);

    return {
      commands,
      catalogEntries,
      historyEntries: [],
    };
  },

  async listHistory(input) {
    return listHistoryPage(input);
  },

  async openCommand(input: OpenBarCommandInput) {
    const tab = await httpClient.post<BarTabResponse>(
      "/bar/tabs",
      {
        name: input.name,
      },
      headersForIdempotency(createIdempotencyKey())
    );

    return mapTab(tab);
  },

  async setCommandStatus(
    commandId: number,
    status: BarCommandStatus
  ) {
    const tab = await getTab(commandId);

    if (status === "awaitingPayment") {
      if (tab.status === "PAYMENT_PENDING") {
        return mapTab(tab);
      }

      const prepared = await httpClient.post<BarTabResponse>(
        `/bar/tabs/${commandId}/prepare`,
        {
          documentType: "GENERAL_RECEIPT",
          cpf: null,
          discountCents: 0,
        },
        headersForIdempotency(createIdempotencyKey())
      );

      return mapTab(prepared);
    }

    if (tab.status === "PAYMENT_PENDING") {
      if (!tab.checkoutId) {
        throw new Error(
          "A comanda esta aguardando pagamento, mas nao possui checkout vinculado."
        );
      }

      await cancelCheckout(
        tab.checkoutId,
        "Retorno da comanda para consumo."
      );

      return mapTab(await getTab(commandId));
    }

    return mapTab(tab);
  },

  async addCommandItem(
    commandId: number,
    input: AddBarCommandItemInput
  ) {
    const tab =
      await httpClient.put<BarTabResponse>(
        `/bar/tabs/${commandId}/catalog/${input.catalogItemId}`,
        {
          quantity:
            input.quantity ?? 1,
        }
      );

    return mapTab(tab);
  },

  async updateCommandItemQuantity(
    commandId: number,
    itemKey: string,
    quantity: number,
    unitPrice?: number
  ) {
    const catalogItemId =
      Number(
        itemKey.split(":").at(-1)
      );

    if (
      !Number.isSafeInteger(
        catalogItemId
      ) ||
      catalogItemId <= 0
    ) {
      throw new Error(
        "O item ou serviço da comanda é inválido."
      );
    }

    const path =
      `/bar/tabs/${commandId}/catalog/${catalogItemId}`;

    if (quantity <= 0) {
      const tab =
        await httpClient.delete<
          BarTabResponse
        >(path);

      return mapTab(tab);
    }

    const tab =
      await httpClient.put<
        BarTabResponse
      >(
        path,
        {
          quantity,

          ...(typeof unitPrice ===
          "number"
            ? {
                unitPriceCents:
                  Math.round(
                    unitPrice * 100
                  ),
              }
            : {}),
        }
      );

    return mapTab(tab);
  },

  async removeCommandItem(commandId: number, itemKey: string) {
    return this.updateCommandItemQuantity(commandId, itemKey, 0);
  },

  async cancelCommand(commandId: number) {
    const tab = await getTab(commandId);

    if (tab.status === "PAYMENT_PENDING") {
      if (!tab.checkoutId) {
        throw new Error(
          "A comanda esta aguardando pagamento, mas nao possui checkout vinculado."
        );
      }

      await cancelCheckout(
        tab.checkoutId,
        "Cancelamento da comanda pelo operador."
      );
    }

    await httpClient.post(
      `/bar/tabs/${commandId}/cancel`,
      {
        reason: "Cancelado pelo operador.",
      }
    );
  },

  async closeCommand(
    input: CloseBarCommandInput
  ): Promise<CloseBarCommandRepositoryResult> {
    const current = await getTab(input.commandId);
    const prepared =
      current.status === "PAYMENT_PENDING"
        ? current
        : await httpClient.post<BarTabResponse>(
            `/bar/tabs/${input.commandId}/prepare`,
            {
              documentType: documentToBackend(),
              cpf: null,
              discountCents: 0,
            },
            headersForIdempotency(createIdempotencyKey())
          );

    if (!prepared.checkoutId) {
      throw new Error(
        "A comanda nao possui checkout disponivel para pagamento."
      );
    }

    const paymentResult = await runPayment(
      prepared.checkoutId,
      input.payment,
      prepared.totalCents,
      input.cashReceived
    );

    const closedCommand = mapTab({
      ...prepared,
      status: "CLOSED",
    });

    const catalogEntries =
      await refreshCatalogEntries()
        .catch(() => null);

    return {
      closedCommand,
      catalogEntries,
      historyEntry: {
        id: prepared.id,
        checkoutId: prepared.checkoutId,
            origin: `Comanda ${prepared.name}`,
        description: closedCommand.items
          .map((item) => `${item.quantity}x ${item.name}`)
          .join(", "),
        receiptItems: closedCommand.items.map((item) => ({
          quantity: item.quantity,
          name: item.name,
          unitPrice: item.unitPrice,
          total: item.unitPrice * item.quantity,
        })),
        amount: prepared.totalCents / 100,
        method: input.payment,
        document: input.document,
        cashReceived:
          input.payment === "Dinheiro"
            ? input.cashReceived ?? prepared.totalCents / 100
            : undefined,
        cashChange:
          input.payment === "Dinheiro"
            ? Math.max(
                0,
                (input.cashReceived ?? prepared.totalCents / 100) -
                  prepared.totalCents / 100
              )
            : undefined,
        time: input.time,
        completedAt:
          paymentResult.checkout.finalizedAt ??
          new Date().toISOString(),
      },
    };
  },

  async createCatalogEntry(input): Promise<BarCatalogItem> {
    const entry =
      await httpClient.post<
        CatalogEntryResponse
      >(
        "/catalog",
        catalogEntryPayload(input)
      );

    return mapCatalogEntry(
      entry
    );
  },

  async updateCatalogEntry(
    entryId,
    input
  ): Promise<BarCatalogItem> {
    const entry =
      await httpClient.put<
        CatalogEntryResponse
      >(
        `/catalog/${entryId}`,
        catalogEntryPayload(input)
      );

    return mapCatalogEntry(
      entry
    );
  },

  async removeCatalogEntry(
    entryId
  ): Promise<void> {
    await httpClient.delete(
      `/catalog/${entryId}`
    );
  },
};
