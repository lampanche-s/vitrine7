import type {
  LavaClient,
  LavaClientInput,
} from "../../entities/client";

import type {
  LavaService,
  LavaServiceInput,
} from "../../entities/service";

import {
  normalizeLavaServiceInput,
  parseLavaServiceDurationMinutes,
} from "../../entities/service";

import {
  formatBrazilianPhone,
} from "../../shared/formatters/phoneInput";

import {
  formatBrlCurrency,
} from "../../shared/lib/currency";

import {
  currencyInputToNumber,
} from "../../shared/formatters/currencyInput";

import {
  HttpError,
  httpClient,
} from "../../shared/http";

import {
  createIdempotencyKey,
  headersForIdempotency,
  runCheckoutPayment,
  type BackendCheckoutPaymentMethod,
  type CheckoutResponse,
  type PaymentConfirmationResponse,
  type TerminalPaymentConfirmationResponse,
} from "./checkoutPayments";

import type {
  CreateLavaOrderInput,
  LavaHistoryEntry,
  LavaOrderFiscalDocument,
  LavaProgressVehicle,
  LavaVehicleSize,
} from "../../entities/work-order";

import type {
  LavaPaymentMethod,
  LavaWorkOrderPaymentInput,
  LavaWorkOrderPaymentResult,
} from "../../entities/payment";

import type {
  LavaFinancePageResult,
  LavaFinanceTransaction,
  LavaRepository,
  LavaRepositorySnapshot,
  OpenLavaWorkOrderRepositoryResult,
} from "../contracts";

type PageResponse<T> = {
  items: T[];
  page: number;
  number?: number;
  size: number;
  totalPages: number;
  totalElements: number;
  first: boolean;
  last: boolean;
};

type LavaClientResponse = {
  id: number;
  name: string;
  phone: string | null;
  vehicleName: string;
  plate: string;
  visitsCount: number | null;
  lastServiceLabel: string | null;
  active: boolean;
};

type LavaClientRequest = {
  name: string;
  phone: string;
  vehicleName: string;
  plate: string;
};

type LavaServiceResponse = {
  id: number;
  name: string;
  category: string;
  smallVehiclePriceCents: number;
  mediumVehiclePriceCents: number;
  durationMinutes: number;
  durationLabel: string;
  active: boolean;
};

type LavaServiceRequest = {
  name: string;
  category: string;
  smallVehiclePriceCents: number;
  mediumVehiclePriceCents: number;
  durationMinutes: number;
};

type LavaWorkOrderLineResponse = {
  id: number;
  serviceId: number;
  serviceNameSnapshot: string;
  priceCents: number;
  createdAt: string;
  updatedAt: string;
};

type LavaWorkOrderResponse = {
  id: number;
  registeredClientId: number | null;
  customerNameSnapshot: string;
  customerPhoneDigitsSnapshot: string | null;
  vehicleNameSnapshot: string | null;
  vehiclePlateSnapshot: string | null;
  vehicleSize: "SMALL" | "MEDIUM";
  status:
    | "OPEN"
    | "PAYMENT_PENDING"
    | "PAID"
    | "COMPLETED"
    | "CANCELLED";
  checkoutSessionId: string | null;
  checkoutStatus:
    | "DRAFT"
    | "READY_FOR_PAYMENT"
    | "PAYMENT_PROCESSING"
    | "PAYMENT_FAILED"
    | "PAID"
    | "FINALIZED"
    | "CANCELLED"
    | "EXPIRED"
    | null;
  discountCents: number;
  subtotalCents: number;
  totalCents: number;
  documentType: string | null;
  prepared: boolean;
  paidAt: string | null;
  cancelledAt: string | null;
  services: LavaWorkOrderLineResponse[];
  createdAt: string;
  updatedAt: string;
};

type LavaHistoryResponse = {
  workOrderId: number;
  checkoutId: string | null;
  customerSnapshot: string;
  phoneSnapshot: string | null;
  vehicleSnapshot: string | null;
  plateSnapshot: string | null;
  vehicleSize: "SMALL" | "MEDIUM" | string | null;
  status: "PAID" | "COMPLETED" | "CANCELLED" | string;
  subtotalCents: number;
  discountCents: number;
  totalCents: number;
  paymentMethod:
    | "CASH"
    | "PIX"
    | "CREDIT"
    | "DEBIT"
    | null;
  paymentStatus: "APPROVED" | string | null;
  paymentId: string | null;
  paymentReversedAt: string | null;
  paymentReversalReason: string | null;
  serviceCount: number;
  createdAt: string;
  paidAt: string | null;
  completedAt: string | null;
  cancelledAt: string | null;
  createdByUserId: number | null;
  detailPath: string;
};

type FinanceBreakdownResponse = {
  key: string;
  revenueCents: number;
  paymentCount: number;
  revenueBasisPoints: number;
};

type FinanceSummaryResponse = {
  period: {
    from: string;
    to: string;
    timeZone: string;
  };
  totalRevenueCents: number;
  paymentCount: number;
  averageTicketCents: number;
  firstApprovedAt: string | null;
  lastApprovedAt: string | null;
  byModule: FinanceBreakdownResponse[];
  byOperationType: FinanceBreakdownResponse[];
  byPaymentMethod: FinanceBreakdownResponse[];
};

type FinanceTransactionResponse = {
  paymentId: string;
  checkoutId: string;
  operationType: "LAVA_WORK_ORDER" | string;
  module: "BAR" | "LAVA";
  method: "CASH" | "PIX" | "CREDIT" | "DEBIT" | string;
  processingMode: string;
  status: "APPROVED" | string;
  amountCents: number;
  approvedAt: string;
  responsibleUserId: number | null;
  responsibleUserName: string | null;
  operationId: number | null;
  displayName: string;
  description: string;
  detailPath: string;
};

type CreateLavaWorkOrderRequest = {
  clientId: number | null;
  customerName?: string;
  phone?: string;
  vehicleName?: string;
  plate?: string;
  serviceId: number;
  vehicleSize: "SMALL" | "MEDIUM";
};

type PrepareLavaWorkOrderRequest = {
  documentType: "GENERAL_RECEIPT";
  cpf: null;
  discountCents: number;
};

type PaymentResponse = {
  status: "APPROVED" | "DECLINED" | "PROCESSING" | string;
  amountCents: number;
  cashReceivedCents: number | null;
  cashChangeCents: number | null;
};

type LavaPaymentConfirmationResponse =
  PaymentConfirmationResponse & {
    payment: PaymentConfirmationResponse["payment"] &
      PaymentResponse;
    checkout: CheckoutResponse & {
      paidAt: string | null;
    };
  };

type LavaTerminalPaymentConfirmationResponse =
  TerminalPaymentConfirmationResponse &
    LavaPaymentConfirmationResponse;

type ApiErrorPayload = {
  message?: string;
  fieldErrors?: {
    field?: string;
    message?: string;
  }[];
};

const LAVA_CLIENTS_PATH = "/lava/clients";
const LAVA_SERVICES_PATH = "/lava/services";
const LAVA_WORK_ORDERS_PATH = "/lava/work-orders";
const LAVA_HISTORY_PATH = "/lava/history";
const FINANCE_SUMMARY_PATH = "/finance/summary";
const FINANCE_TRANSACTIONS_PATH =
  "/finance/transactions";
const HTTP_PAGE_SIZE = 100;
const LAVA_HISTORY_PAGE_SIZE = 20;
const unsupportedLavaHttpMessage =
  "Esta operação do Lava Jato ainda não está conectada ao backend HTTP.";

const LAVA_PAYABLE_STATUSES = [
  "OPEN",
  "PAYMENT_PENDING",
  "PAID",
] as const;

function createClientRequest(
  input: LavaClientInput
): LavaClientRequest {
  return {
    name: input.name,
    phone: input.phone,
    vehicleName: input.vehicle,
    plate: input.plate,
  };
}

function centsToCurrency(value: number): string {
  return formatBrlCurrency(value / 100);
}

function currencyInputToCents(value: string): number {
  return Math.round(
    currencyInputToNumber(value) * 100
  );
}

function mapVehicleSizeToBackend(
  vehicleSize: LavaVehicleSize
): CreateLavaWorkOrderRequest["vehicleSize"] {
  return vehicleSize === "Médio"
    ? "MEDIUM"
    : "SMALL";
}

function mapVehicleSizeFromBackend(
  vehicleSize: LavaWorkOrderResponse["vehicleSize"]
): LavaVehicleSize {
  return vehicleSize === "MEDIUM"
    ? "Médio"
    : "Pequeno";
}

function createWorkOrderRequest(
  input: CreateLavaOrderInput
): CreateLavaWorkOrderRequest {
  if (input.clientId !== null) {
    return {
      clientId: input.clientId,
      serviceId: input.serviceId,
      vehicleSize: mapVehicleSizeToBackend(
        input.vehicleSize
      ),
    };
  }

  return {
    clientId: null,
    customerName:
      input.clientName.trim() || undefined,
    phone: input.clientPhone,
    vehicleName:
      input.vehicle.trim() || undefined,
    plate: input.plate.trim() || undefined,
    serviceId: input.serviceId,
    vehicleSize: mapVehicleSizeToBackend(
      input.vehicleSize
    ),
  };
}

function documentToBackend():
  PrepareLavaWorkOrderRequest["documentType"] {
  return "GENERAL_RECEIPT";
}

function paymentMethodToBackend(
  method: LavaPaymentMethod
): BackendCheckoutPaymentMethod {
  if (
    method === "Crédito" ||
    method === "Cartão"
  ) {
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
  method: LavaHistoryResponse["paymentMethod"]
): import("../../entities/payment").LavaHistoricalPaymentMethod | null {
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

  return null;
}

function documentFromBackend():
  LavaOrderFiscalDocument {
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

function centsFromAmount(value: number | undefined) {
  return Math.round((value ?? 0) * 100);
}

function createServiceRequest(
  input: LavaServiceInput
): LavaServiceRequest {
  const normalizedInput =
    normalizeLavaServiceInput(input);

  const durationMinutes =
    parseLavaServiceDurationMinutes(
      normalizedInput.duration
    );

  if (durationMinutes === null) {
    throw new Error(
      "Informe uma duração válida para o serviço."
    );
  }

  return {
    name: normalizedInput.name,
    category: normalizedInput.category,
    smallVehiclePriceCents: currencyInputToCents(
      normalizedInput.smallCarPrice
    ),
    mediumVehiclePriceCents: currencyInputToCents(
      normalizedInput.mediumCarPrice
    ),
    durationMinutes,
  };
}

function mapClient(
  client: LavaClientResponse
): LavaClient {
  return {
    id: client.id,
    name: client.name,
    phone: formatBrazilianPhone(client.phone ?? ""),
    vehicle: client.vehicleName,
    plate: client.plate,
    lastService:
      client.lastServiceLabel ?? "Ainda sem serviço",
    visits: client.visitsCount ?? 0,
    active: client.active,
  };
}

function mapService(
  service: LavaServiceResponse
): LavaService {
  const smallCarPrice = centsToCurrency(
    service.smallVehiclePriceCents
  );
  const mediumCarPrice = centsToCurrency(
    service.mediumVehiclePriceCents
  );

  return {
    id: service.id,
    name: service.name,
    category: service.category,
    price: smallCarPrice,
    smallCarPrice,
    mediumCarPrice,
    duration:
      service.durationLabel ??
      `${service.durationMinutes} min`,
    active: service.active,
  };
}

function mapWorkOrderStage(
  workOrder: LavaWorkOrderResponse
): LavaProgressVehicle["stage"] {
  if (workOrder.status === "CANCELLED") {
    return "cancelled";
  }

  if (workOrder.status === "COMPLETED") {
    return "done";
  }

  return "waiting";
}

function mapWorkOrder(
  workOrder: LavaWorkOrderResponse
): LavaProgressVehicle {
  const firstService = workOrder.services[0];

  return {
    id: workOrder.id,
    financeEntryId: workOrder.id,
    clientId: workOrder.registeredClientId,
    clientName: workOrder.customerNameSnapshot,
    clientPhone: formatBrazilianPhone(
      workOrder.customerPhoneDigitsSnapshot ?? ""
    ),
    serviceId: firstService?.serviceId ?? null,
    vehicle: workOrder.vehicleNameSnapshot ?? "",
    plate: workOrder.vehiclePlateSnapshot ?? "",
    service:
      workOrder.services
        .map((service) =>
          service.serviceNameSnapshot
        )
        .join(" + ") || "Sem serviço",
    vehicleSize: mapVehicleSizeFromBackend(
      workOrder.vehicleSize
    ),
    notes: "",
    stage: mapWorkOrderStage(workOrder),
    elapsed:
      workOrder.status === "OPEN"
        ? "Em aberto"
        : workOrder.status,
    amount: workOrder.totalCents / 100,
    checkoutId: workOrder.checkoutSessionId,
    checkoutStatus: workOrder.checkoutStatus,
    operationalStatus: workOrder.status,
    prepared: workOrder.prepared,
    paymentStatus:
      workOrder.paidAt || workOrder.status === "PAID"
        ? "APPROVED"
        : null,
    paidAt: workOrder.paidAt ?? undefined,
    createdAt: workOrder.createdAt,
    serviceDurationMinutes: null,
  };
}

function normalizeDetailPath(path: string): string {
  return path.startsWith("/api/v1")
    ? path.slice("/api/v1".length)
    : path;
}

function paymentHistoryStatus(
  status: string | null
): "Concluída" | "Estorno pendente" | "Estornada" {
  if (status === "REVERSED") {
    return "Estornada";
  }

  if (status === "REVERSAL_PENDING") {
    return "Estorno pendente";
  }

  return "Concluída";
}

function isCompletedHistoryEntry(
  entry: LavaHistoryResponse
): boolean {
  return (
    entry.status === "COMPLETED" &&
    [
      "APPROVED",
      "REVERSAL_PENDING",
      "REVERSED",
    ].includes(entry.paymentStatus ?? "") &&
    entry.paymentMethod !== null &&
    entry.completedAt !== null
  );
}

async function mapHistoryEntry(
  entry: LavaHistoryResponse
): Promise<LavaHistoryEntry | null> {
  if (!isCompletedHistoryEntry(entry)) {
    return null;
  }

  const detailPath = normalizeDetailPath(
    entry.detailPath
  );

  const workOrder =
    await findWorkOrderByPath(detailPath);

  const method = paymentMethodFromHistory(
    entry.paymentMethod
  );

  if (method === null) {
    return null;
  }

  const completedAt =
    entry.completedAt ?? workOrder.updatedAt;

  return {
    id: entry.workOrderId,
    checkoutId: entry.checkoutId,
    paymentId: entry.paymentId,
    paymentReversedAt:
      entry.paymentReversedAt,
    paymentReversalReason:
      entry.paymentReversalReason,
    clientName: entry.customerSnapshot,
    clientPhone: formatBrazilianPhone(
      entry.phoneSnapshot ?? ""
    ),
    vehicle: entry.vehicleSnapshot ?? "",
    plate: entry.plateSnapshot ?? "",
    vehicleSize:
      entry.vehicleSize === "SMALL" ||
      entry.vehicleSize === "MEDIUM"
        ? mapVehicleSizeFromBackend(
            entry.vehicleSize
          )
        : null,
    service:
      workOrder.services
        .map((service) =>
          service.serviceNameSnapshot
        )
        .join(" + ") || `${entry.serviceCount} serviço(s)`,
    serviceCount: entry.serviceCount,
    receiptItems: workOrder.services.map((service) => ({
      quantity: 1,
      name: service.serviceNameSnapshot,
      unitPrice: service.priceCents / 100,
      total: service.priceCents / 100,
    })),
    amount: entry.totalCents / 100,
    method,
    document: documentFromBackend(),
    status: paymentHistoryStatus(
      entry.paymentStatus
    ),
    paymentStatus:
      entry.paymentStatus as
        | "APPROVED"
        | "REVERSAL_PENDING"
        | "REVERSED",
    time: formatHistoryTime(completedAt),
    createdAt: entry.createdAt,
    paidAt: entry.paidAt,
    completedAt,
    detailPath,
  };
}

function isApiErrorPayload(
  payload: unknown
): payload is ApiErrorPayload {
  return (
    typeof payload === "object" &&
    payload !== null
  );
}

function getBackendErrorMessage(
  error: HttpError
): string {
  if (!isApiErrorPayload(error.payload)) {
    return error.message;
  }

  const fieldMessages =
    error.payload.fieldErrors
      ?.map((fieldError) => fieldError.message)
      .filter((message): message is string =>
        Boolean(message)
      ) ?? [];

  if (fieldMessages.length > 0) {
    return fieldMessages.join(" ");
  }

  return error.payload.message ?? error.message;
}

async function withReadableBackendError<T>(
  operation: () => Promise<T>
): Promise<T> {
  try {
    return await operation();
  } catch (error) {
    if (error instanceof HttpError) {
      throw new HttpError(
        getBackendErrorMessage(error),
        error.status,
        error.payload
      );
    }

    throw error;
  }
}

async function withReadableFinanceError<T>(
  operation: () => Promise<T>
): Promise<T> {
  try {
    return await withReadableBackendError(operation);
  } catch (error) {
    if (
      error instanceof HttpError &&
      error.status === 401
    ) {
      throw new HttpError(
        "Faça login para acessar o financeiro do Lava Jato.",
        error.status,
        error.payload
      );
    }

    if (
      error instanceof HttpError &&
      error.status === 403
    ) {
      throw new HttpError(
        "Sua sessão não possui permissão para acessar o financeiro do Lava Jato.",
        error.status,
        error.payload
      );
    }

    throw error;
  }
}

async function listClientsPage(
  page: number
): Promise<PageResponse<LavaClientResponse>> {
  return httpClient.get<PageResponse<LavaClientResponse>>(
    `${LAVA_CLIENTS_PATH}?page=${page}&size=${HTTP_PAGE_SIZE}&sort=name&direction=ASC`
  );
}

async function listServicesPage(
  page: number
): Promise<PageResponse<LavaServiceResponse>> {
  return httpClient.get<PageResponse<LavaServiceResponse>>(
    `${LAVA_SERVICES_PATH}?page=${page}&size=${HTTP_PAGE_SIZE}&sort=name&direction=ASC`
  );
}

async function listWorkOrdersPage(
  status: (typeof LAVA_PAYABLE_STATUSES)[number],
  page: number
): Promise<PageResponse<LavaWorkOrderResponse>> {
  return httpClient.get<PageResponse<LavaWorkOrderResponse>>(
    `${LAVA_WORK_ORDERS_PATH}?page=${page}&size=${HTTP_PAGE_SIZE}&status=${status}`
  );
}

async function listAllClients(): Promise<LavaClient[]> {
  return withReadableBackendError(async () => {
    const clientsById = new Map<number, LavaClient>();
    let currentPage = 0;

    while (true) {
      const response =
        await listClientsPage(currentPage);

      response.items.forEach((client) => {
        clientsById.set(
          client.id,
          mapClient(client)
        );
      });

      const nextPage = response.page + 1;

      if (nextPage >= response.totalPages) {
        break;
      }

      currentPage = nextPage;
    }

    return Array.from(clientsById.values());
  });
}

async function listAllServices(): Promise<LavaService[]> {
  return withReadableBackendError(async () => {
    const servicesById = new Map<number, LavaService>();
    let currentPage = 0;

    while (true) {
      const response =
        await listServicesPage(currentPage);

      response.items.forEach((service) => {
        servicesById.set(
          service.id,
          mapService(service)
        );
      });

      const nextPage = response.page + 1;

      if (nextPage >= response.totalPages) {
        break;
      }

      currentPage = nextPage;
    }

    return Array.from(servicesById.values());
  });
}

async function listAllOpenWorkOrders(): Promise<LavaProgressVehicle[]> {
  return withReadableBackendError(async () => {
    const workOrdersById =
      new Map<number, LavaProgressVehicle>();

    for (const status of LAVA_PAYABLE_STATUSES) {
      let currentPage = 0;

      while (true) {
        const response =
          await listWorkOrdersPage(
            status,
            currentPage
          );

        response.items.forEach((workOrder) => {
          workOrdersById.set(
            workOrder.id,
            mapWorkOrder(workOrder)
          );
        });

        const nextPage = response.page + 1;

        if (nextPage >= response.totalPages) {
          break;
        }

        currentPage = nextPage;
      }
    }

    return Array.from(workOrdersById.values());
  });
}

async function findWorkOrderByPath(
  path: string
): Promise<LavaWorkOrderResponse> {
  return withReadableBackendError(() =>
    httpClient.get<LavaWorkOrderResponse>(path)
  );
}

async function mapHistoryEntries(
  entries: LavaHistoryResponse[]
): Promise<LavaHistoryEntry[]> {
  const mappedEntries = await Promise.all(
    entries.map(mapHistoryEntry)
  );
  const entriesById =
    new Map<number, LavaHistoryEntry>();

  mappedEntries.forEach((entry) => {
    if (entry) {
      entriesById.set(entry.id, entry);
    }
  });

  return Array.from(entriesById.values());
}

async function refreshHistory(): Promise<LavaHistoryEntry[]> {
  return withReadableBackendError(async () => {
    const history =
      await httpClient.get<LavaHistoryResponse[]>(
        `${LAVA_HISTORY_PATH}/recent?limit=${LAVA_HISTORY_PAGE_SIZE}`
      );

    return mapHistoryEntries(history);
  });
}

async function listHistoryPage(input: {
  page: number;
  size: number;
  search?: string;
  from?: string;
  to?: string;
}) {
  return withReadableBackendError(async () => {
    const params = new URLSearchParams({
      page: String(input.page),
      size: String(input.size),
      status: "COMPLETED",
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
      await httpClient.get<PageResponse<LavaHistoryResponse>>(
        `${LAVA_HISTORY_PATH}?${params.toString()}`
      );

    return {
      entries: await mapHistoryEntries(response.items),
      page: response.number ?? response.page,
      size: response.size,
      totalPages: response.totalPages,
      totalElements: response.totalElements,
      first: response.first,
      last: response.last,
    };
  });
}

function createLavaFinanceParams(input: {
  from: string;
  to: string;
  page?: number;
  size?: number;
  search?: string;
}): URLSearchParams {
  const params = new URLSearchParams({
    from: input.from,
    to: input.to,
    module: "LAVA",
    operationType: "LAVA_WORK_ORDER",
  });

  if (typeof input.page === "number") {
    params.set("page", String(input.page));
  }

  if (typeof input.size === "number") {
    params.set("size", String(input.size));
  }

  const search = input.search?.trim();

  if (search) {
    params.set("search", search);
  }

  return params;
}

function isApprovedLavaFinanceTransaction(
  transaction: FinanceTransactionResponse
): transaction is LavaFinanceTransaction {
  return (
    transaction.module === "LAVA" &&
    transaction.operationType ===
      "LAVA_WORK_ORDER" &&
    transaction.status === "APPROVED"
  );
}

async function listFinancePage(input: {
  from: string;
  to: string;
  page: number;
  size: number;
  search?: string;
}): Promise<LavaFinancePageResult> {
  return withReadableFinanceError(async () => {
    const summaryParams =
      createLavaFinanceParams({
        from: input.from,
        to: input.to,
        search: input.search,
      });
    const transactionParams =
      createLavaFinanceParams(input);

    transactionParams.set(
      "page",
      String(input.page)
    );
    transactionParams.set(
      "size",
      String(input.size)
    );

    const [summary, transactions] =
      await Promise.all([
        httpClient.get<FinanceSummaryResponse>(
          `${FINANCE_SUMMARY_PATH}?${summaryParams.toString()}`
        ),
        httpClient.get<PageResponse<FinanceTransactionResponse>>(
          `${FINANCE_TRANSACTIONS_PATH}?${transactionParams.toString()}`
        ),
      ]);

    return {
      summary: {
        period: summary.period,
        totalRevenueCents:
          summary.totalRevenueCents,
        paymentCount: summary.paymentCount,
        averageTicketCents:
          summary.averageTicketCents,
        firstApprovedAt:
          summary.firstApprovedAt,
        lastApprovedAt: summary.lastApprovedAt,
        byPaymentMethod:
          summary.byPaymentMethod,
      },
      transactions: transactions.items.filter(
        isApprovedLavaFinanceTransaction
      ),
      page: transactions.number ?? transactions.page,
      totalPages: transactions.totalPages,
      totalElements:
        transactions.totalElements ??
        transactions.items.length,
    };
  });
}

async function findWorkOrderById(
  workOrderId: number
): Promise<LavaWorkOrderResponse> {
  return withReadableBackendError(() =>
    httpClient.get<LavaWorkOrderResponse>(
      `${LAVA_WORK_ORDERS_PATH}/${workOrderId}`
    )
  );
}

async function completePaidWorkOrder(
  workOrderId: number
): Promise<LavaWorkOrderResponse> {
  return withReadableBackendError(async () => {
    const completedWorkOrder =
      await httpClient.post<LavaWorkOrderResponse>(
        `${LAVA_WORK_ORDERS_PATH}/${workOrderId}/complete`
      );

    if (completedWorkOrder.status === "COMPLETED") {
      return findWorkOrderById(workOrderId);
    }

    throw new Error(
      "A conclusão da OS não retornou o estado final esperado."
    );
  });
}

async function completeBackendWorkOrder(
  workOrderId: number
): Promise<LavaProgressVehicle> {
  return mapWorkOrder(
    await completePaidWorkOrder(workOrderId)
  );
}

async function openBackendWorkOrder(
  input: CreateLavaOrderInput
): Promise<OpenLavaWorkOrderRepositoryResult> {
  return withReadableBackendError(async () => {
    const createdWorkOrder =
      await httpClient.post<LavaWorkOrderResponse>(
        LAVA_WORK_ORDERS_PATH,
        createWorkOrderRequest(input),
        {
          headers: {
            "Idempotency-Key":
              createIdempotencyKey(),
          },
        }
      );

    return {
      workOrder: mapWorkOrder(createdWorkOrder),
    };
  });
}

function getTerminalPaymentMessage(
  result: TerminalPaymentConfirmationResponse
) {
  return (
    result.terminalTransaction.responseMessage ??
    result.terminalTransaction.errorMessage ??
    result.terminalTransaction.providerFailureMessage ??
    (
      result.payment.status === "DECLINED"
        ? "Pagamento recusado pela maquininha."
        : "Pagamento não aprovado."
    )
  );
}

async function prepareWorkOrderForPayment(
  workOrder: LavaWorkOrderResponse
): Promise<LavaWorkOrderResponse> {
  if (
    workOrder.status === "PAID" ||
    workOrder.status === "COMPLETED"
  ) {
    throw new Error(
      "Esta ordem de serviço já possui pagamento aprovado."
    );
  }

  if (
    workOrder.prepared &&
    workOrder.checkoutSessionId
  ) {
    return workOrder;
  }

  return withReadableBackendError(() =>
    httpClient.post<LavaWorkOrderResponse>(
      `${LAVA_WORK_ORDERS_PATH}/${workOrder.id}/prepare`,
      {
        documentType: documentToBackend(),
        cpf: null,
        discountCents: 0,
      },
      headersForIdempotency(createIdempotencyKey())
    )
  );
}

async function runCashPayment(
  checkoutId: string,
  input: LavaWorkOrderPaymentInput
) {
  return runCheckoutPayment({
    checkoutId,
    processing: "cash",
    method: "CASH",
    cashReceivedCents: centsFromAmount(
      input.cashReceived
    ),
  }) as Promise<LavaPaymentConfirmationResponse>;
}

async function runManualPayment(
  checkoutId: string,
  input: LavaWorkOrderPaymentInput
) {
  const method = paymentMethodToBackend(input.method);

  if (method === "PIX") {
    return runCheckoutPayment({
      checkoutId,
      processing: "pix",
      method: "PIX",
    }) as Promise<LavaPaymentConfirmationResponse>;
  }

  return runCheckoutPayment({
    checkoutId,
    processing: "manual-card",
    method,
    reason: input.manualReason ?? "",
  }) as Promise<LavaPaymentConfirmationResponse>;
}

async function runTerminalPayment(
  checkoutId: string,
  input: LavaWorkOrderPaymentInput
) {
  return runCheckoutPayment({
    checkoutId,
    processing: "terminal",
    method: paymentMethodToBackend(input.method),
  }) as Promise<LavaTerminalPaymentConfirmationResponse>;
}

async function payBackendWorkOrder(
  input: LavaWorkOrderPaymentInput
): Promise<LavaWorkOrderPaymentResult> {
  return withReadableBackendError(async () => {
    const currentWorkOrder =
      await findWorkOrderById(input.workOrderId);

    const preparedWorkOrder =
      await prepareWorkOrderForPayment(
        currentWorkOrder
      );

    const checkoutId =
      preparedWorkOrder.checkoutSessionId;

    if (!checkoutId) {
      throw new Error(
        "O backend não retornou o checkout da ordem de serviço."
      );
    }

    if (input.mode === "cash") {
      const result =
        await runCashPayment(checkoutId, input);

      const updatedWorkOrder =
        result.payment.status === "APPROVED"
          ? await findWorkOrderById(input.workOrderId)
          : await findWorkOrderById(input.workOrderId);

      return {
        workOrder: {
          ...mapWorkOrder(updatedWorkOrder),
          paymentStatus: result.payment.status,
          cashReceived:
            result.payment.cashReceivedCents === null
              ? null
              : result.payment.cashReceivedCents / 100,
          cashChange:
            result.payment.cashChangeCents === null
              ? null
              : result.payment.cashChangeCents / 100,
        },
        approved:
          result.payment.status === "APPROVED",
        message:
          result.payment.status === "APPROVED"
            ? "Pagamento em dinheiro aprovado."
            : "Pagamento em dinheiro não aprovado.",
      };
    }

    if (input.mode === "manual") {
      const result =
        await runManualPayment(checkoutId, input);

      const updatedWorkOrder =
        result.payment.status === "APPROVED"
          ? await findWorkOrderById(input.workOrderId)
          : await findWorkOrderById(input.workOrderId);

      return {
        workOrder: {
          ...mapWorkOrder(updatedWorkOrder),
          paymentStatus: result.payment.status,
        },
        approved:
          result.payment.status === "APPROVED",
        message:
          result.payment.status === "APPROVED"
            ? input.method === "Pix"
              ? "Pagamento Pix aprovado."
              : "Pagamento manual aprovado."
            : input.method === "Pix"
              ? "Pagamento Pix não aprovado."
              : "Pagamento manual não aprovado.",
      };
    }

    const result =
      await runTerminalPayment(checkoutId, input);

    if (result.payment.status !== "APPROVED") {
      const updatedWorkOrder =
        await findWorkOrderById(input.workOrderId);

      return {
        workOrder: {
          ...mapWorkOrder(updatedWorkOrder),
          paymentStatus: result.payment.status,
        },
        approved: false,
        message: getTerminalPaymentMessage(result),
      };
    }

    return {
      workOrder: {
        ...mapWorkOrder(
          await findWorkOrderById(input.workOrderId)
        ),
        paymentStatus: result.payment.status,
      },
      approved: true,
      message: "Pagamento aprovado.",
    };
  });
}

function unsupportedLavaHttpOperation(): never {
  throw new Error(unsupportedLavaHttpMessage);
}

export const httpLavaRepository: LavaRepository = {
  async getSnapshot(): Promise<LavaRepositorySnapshot> {
    return {
      services: await listAllServices(),
      clients: await listAllClients(),
      workOrders: await listAllOpenWorkOrders(),
      financeEntries: [],
      historyEntries: await refreshHistory(),
    };
  },

  async listHistory(input) {
    return listHistoryPage(input);
  },

  async listFinance(input) {
    return listFinancePage(input);
  },

  async createClient(input) {
    return withReadableBackendError(async () => {
      const client =
        await httpClient.post<LavaClientResponse>(
          LAVA_CLIENTS_PATH,
          createClientRequest(input)
        );

      return mapClient(client);
    });
  },

  async updateClient(clientId, input) {
    return withReadableBackendError(async () => {
      const client =
        await httpClient.put<LavaClientResponse>(
          `${LAVA_CLIENTS_PATH}/${clientId}`,
          createClientRequest(input)
        );

      return mapClient(client);
    });
  },

  async setClientActive(clientId, active) {
    return withReadableBackendError(async () => {
      const client =
        await httpClient.patch<LavaClientResponse>(
          `${LAVA_CLIENTS_PATH}/${clientId}/active`,
          {
            active,
          }
        );

      return mapClient(client);
    });
  },

  async removeClient(clientId) {
    await withReadableBackendError(() =>
      httpClient.delete<void>(
        `${LAVA_CLIENTS_PATH}/${clientId}`
      )
    );
  },

  async createService(input) {
    return withReadableBackendError(async () => {
      const service =
        await httpClient.post<LavaServiceResponse>(
          LAVA_SERVICES_PATH,
          createServiceRequest(input)
        );

      return mapService(service);
    });
  },

  async updateService(serviceId, input) {
    return withReadableBackendError(async () => {
      const service =
        await httpClient.put<LavaServiceResponse>(
          `${LAVA_SERVICES_PATH}/${serviceId}`,
          createServiceRequest(input)
        );

      return mapService(service);
    });
  },

  async setServiceActive(serviceId, active) {
    return withReadableBackendError(async () => {
      const service =
        await httpClient.patch<LavaServiceResponse>(
          `${LAVA_SERVICES_PATH}/${serviceId}/active`,
          {
            active,
          }
        );

      return mapService(service);
    });
  },

  async removeService(serviceId) {
    await withReadableBackendError(() =>
      httpClient.delete<void>(
        `${LAVA_SERVICES_PATH}/${serviceId}`
      )
    );
  },

  async openWorkOrder(input) {
    return openBackendWorkOrder(input);
  },

  async getWorkOrder(workOrderId) {
    const workOrder =
      await findWorkOrderById(workOrderId);

    return mapWorkOrder(workOrder);
  },

  async payWorkOrder(input) {
    return payBackendWorkOrder(input);
  },

  async changeWorkOrderStage(workOrderId, stage) {
    if (stage === "done") {
      return completeBackendWorkOrder(workOrderId);
    }

    unsupportedLavaHttpOperation();
  },

  async settleFinanceEntry() {
    unsupportedLavaHttpOperation();
  },

  async markServiceTimeNotified() {
    unsupportedLavaHttpOperation();
  },
};
