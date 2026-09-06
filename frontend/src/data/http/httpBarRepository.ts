import type {
  BarCatalogItem,
} from "../../entities/catalog-item";

import type {
  AddBarCommandItemInput,
  BarCommand,
  BarCommandStatus,
  CloseBarCommandInput,
  OpenBarCommandInput,
  VoucherBarCommandInput,
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
  stockEnabled: boolean;
  stockQuantity: number | null;
  minimumStockQuantity: number | null;
  supplierId: number | null;
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
  clientId: number | null;
  clientName: string | null;
  employeeId: number | null;
  closureType: "PAYMENT" | "VOUCHER" | null;
  status: "OPEN" | "PAYMENT_PENDING" | "CLOSED" | "CANCELLED";
  checkoutId: string | null;
  checkoutStatus: string | null;
  totalCents: number;
  lines: BarTabLineResponse[];
  closedAt?: string | null;
  vehicleName?: string | null;
  vehiclePlate?: string | null;
  reopenUntil?: string | null;
  canReopen?: boolean;
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
  paymentMethod: "CASH" | "PIX" | "CREDIT" | "DEBIT" | "MULTIPLE" | null;
  paymentStatus: string | null;
  paymentId: string | null;
  paymentReversedAt: string | null;
  paymentReversalReason: string | null;
  cashReceivedCents: number | null;
  cashChangeCents: number | null;
  lineCount: number;
  totalUnits: number;
  finishedAt: string;
  reopenUntil: string | null;
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

  if (method === "MULTIPLE") {
    return "Múltiplas";
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
    reopenUntil: entry.reopenUntil,
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
    stockEnabled: entry.stockEnabled,
    stockQuantity: entry.stockQuantity,
    minimumStockQuantity:
      entry.minimumStockQuantity,
    supplierId: entry.supplierId,
  };
}

function mapTab(tab: BarTabResponse): BarCommand {
  return {
    id: tab.id,
    name: tab.name,
    clientId: tab.clientId,
    clientName: tab.clientName,
    employeeId: tab.employeeId,
    status:
      tab.status === "PAYMENT_PENDING"
        ? "awaitingPayment"
        : "open",
    checkoutId: tab.checkoutId,
    checkoutStatus: tab.checkoutStatus,
    vehicleName: tab.vehicleName ?? null,
    vehiclePlate: tab.vehiclePlate ?? null,
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
        lineId: line.id,
        catalogItemId,
        entryType: line.entryType,
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
  stockEnabled: boolean;
  stockQuantity: number | null;
  minimumStockQuantity: number | null;
  supplierId?: number | null;
}) {
  return {
    name: input.name.trim(),
    type: input.type,
    priceCents:
      Math.round(input.price * 100),
    stockEnabled:
      input.type === "ITEM" &&
      input.stockEnabled,
    stockQuantity:
      input.type === "ITEM" &&
      input.stockEnabled
        ? input.stockQuantity
        : null,
    minimumStockQuantity:
      input.type === "ITEM" &&
      input.stockEnabled
        ? input.minimumStockQuantity
        : null,
    supplierId:
      input.type === "ITEM"
        ? input.supplierId
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
  payment: BarPaymentMethod,
  amountCents: number
) {
  const method = paymentMethodToBackend(payment);

  const result = await runCheckoutPayment({
    checkoutId,
    processing: "terminal",
    method,
    amountCents,
  });

  if (result.payment.status !== "APPROVED") {
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
  amountCents: number
) {
  const result = await runCheckoutPayment({
    checkoutId,
    processing: "pix",
    method: "PIX",
    amountCents,
  });

  if (result.payment.status !== "APPROVED") {
    throw new Error("Pagamento Pix não aprovado.");
  }

  return result;
}

async function runCashPayment(
  checkoutId: string,
  amountCents: number,
  cashReceivedCents: number
) {
  const result = await runCheckoutPayment({
    checkoutId,
    processing: "cash",
    method: "CASH",
    amountCents,
    cashReceivedCents,
  });

  if (result.payment.status !== "APPROVED") {
    throw new Error("Pagamento em dinheiro não aprovado.");
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
      amountCents,
      cashReceived === undefined
        ? amountCents
        : centsFromAmount(cashReceived)
    );
  }

  if (method === "PIX") {
    return runPixPayment(checkoutId, amountCents);
  }

  return runTerminalPayment(checkoutId, payment, amountCents);
}

type ExistingCheckoutPayment = {
  id: string;
  method: BackendCheckoutPaymentMethod;
  status: string;
  amountCents: number;
  cashReceivedCents: number | null;
};

async function existingApprovedPayments(checkoutId: string) {
  return (await httpClient.get<ExistingCheckoutPayment[]>(
    `/checkouts/${checkoutId}/payments`
  )).filter((payment) => payment.status === "APPROVED");
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
        clientId: input.clientId ?? null,
        employeeId: input.employeeId ?? null,
      },
      headersForIdempotency(createIdempotencyKey())
    );

    return mapTab(tab);
  },

  async reopenCommand(commandId: number) {
    const tab = await httpClient.post<BarTabResponse>(
      `/bar/tabs/${commandId}/reopen`,
      {}
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

  async printPrePaymentNote(commandId: number) {
    await httpClient.post(
      `/bar/tabs/${commandId}/prepayment-print-jobs`
    );
  },

  async printItems(tabId: number) {
    await httpClient.post(
      `/bar/tabs/${tabId}/print/items`
    );
  },

  async printServices(tabId: number) {
    await httpClient.post(
      `/bar/tabs/${tabId}/print/services`
    );
  },

  async printLine(tabId: number, lineId: number) {
    await httpClient.post(
      `/bar/tabs/${tabId}/lines/${lineId}/print`
    );
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
          ...(input.vehicleName != null ? { vehicleName: input.vehicleName } : {}),
          ...(input.vehiclePlate != null ? { vehiclePlate: input.vehiclePlate } : {}),
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
              vehicleName: input.vehicleName ?? null,
              vehiclePlate: input.vehiclePlate ?? null,
            },
            headersForIdempotency(createIdempotencyKey())
          );

    if (!prepared.checkoutId) {
      throw new Error(
        "A comanda nao possui checkout disponivel para pagamento."
      );
    }

    const parts = input.payments?.length
      ? input.payments
      : [{
          method: input.payment ?? "Dinheiro",
          amount: prepared.totalCents / 100,
          cashReceived: input.cashReceived,
        }];
    const existing = await existingApprovedPayments(prepared.checkoutId);
    const unusedExisting = [...existing];
    let lastPaymentResult: Awaited<ReturnType<typeof runPayment>> | null = null;

    for (const part of parts) {
      const amountCents = centsFromAmount(part.amount);
      const backendMethod = paymentMethodToBackend(part.method);
      const cashReceivedCents = part.method === "Dinheiro"
        ? centsFromAmount(part.cashReceived ?? part.amount)
        : null;

      const existingIndex = unusedExisting.findIndex((payment) =>
        payment.method === backendMethod &&
        payment.amountCents === amountCents &&
        (backendMethod !== "CASH" ||
          payment.cashReceivedCents === cashReceivedCents)
      );

      if (existingIndex >= 0) {
        unusedExisting.splice(existingIndex, 1);
        continue;
      }

      lastPaymentResult = await runPayment(
        prepared.checkoutId,
        part.method,
        amountCents,
        part.cashReceived
      );
    }

    const finalTab = await getTab(input.commandId);
    if (finalTab.status !== "CLOSED" || finalTab.checkoutStatus !== "FINALIZED") {
      throw new Error(
        "Os pagamentos foram registrados, mas a comanda ainda possui saldo pendente."
      );
    }

    const closedCommand = mapTab(finalTab);
    const catalogEntries = await refreshCatalogEntries().catch(() => null);
    const method = parts.length > 1
      ? "Múltiplas" as const
      : parts[0]?.method ?? "Dinheiro";
    const cashReceived = parts
      .filter((part) => part.method === "Dinheiro")
      .reduce((sum, part) => sum + (part.cashReceived ?? part.amount), 0);
    const cashAmount = parts
      .filter((part) => part.method === "Dinheiro")
      .reduce((sum, part) => sum + part.amount, 0);

    return {
      closedCommand,
      catalogEntries,
      historyEntry: {
        id: finalTab.id,
        checkoutId: finalTab.checkoutId,
        origin: `Comanda ${finalTab.name}`,
        description: closedCommand.items
          .map((item) => `${item.quantity}x ${item.name}`)
          .join(", "),
        receiptItems: closedCommand.items.map((item) => ({
          quantity: item.quantity,
          name: item.name,
          unitPrice: item.unitPrice,
          total: item.unitPrice * item.quantity,
        })),
        amount: finalTab.totalCents / 100,
        method,
        document: input.document,
        cashReceived: cashReceived > 0 ? cashReceived : undefined,
        cashChange: cashReceived > 0
          ? Math.max(0, cashReceived - cashAmount)
          : undefined,
        time: input.time,
        completedAt:
          lastPaymentResult?.checkout.finalizedAt ??
          new Date().toISOString(),
      },
    };
  },

  async closeVoucher(input: VoucherBarCommandInput) {
    return mapTab(await httpClient.post<BarTabResponse>(
      `/bar/tabs/${input.commandId}/voucher`,
      { vehicleName: input.vehicleName ?? null, vehiclePlate: input.vehiclePlate ?? null }
    ));
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
