import type {
  LavaClient,
  LavaClientInput,
} from "../../entities/client";

import type {
  LavaFinanceEntry,
  LavaWorkOrderPaymentInput,
  LavaWorkOrderPaymentResult,
  SettleLavaFinanceEntryInput,
} from "../../entities/payment";

import type {
  LavaService,
  LavaServiceInput,
} from "../../entities/service";

import type {
  CreateLavaOrderInput,
  LavaHistoryEntry,
  LavaProgressStage,
  LavaProgressVehicle,
} from "../../entities/work-order";

export type LavaRepositorySnapshot = {
  services: LavaService[];
  clients: LavaClient[];
  workOrders: LavaProgressVehicle[];
  financeEntries: LavaFinanceEntry[];
  historyEntries: LavaHistoryEntry[];
};

export type LavaHistoryPageRequest = {
  page: number;
  size: number;
  search?: string;
  from?: string;
  to?: string;
};

export type LavaHistoryPageResult = {
  entries: LavaHistoryEntry[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
  first: boolean;
  last: boolean;
};

export type LavaFinanceBreakdown = {
  key: string;
  revenueCents: number;
  paymentCount: number;
  revenueBasisPoints: number;
};

export type LavaFinanceSummary = {
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
  byPaymentMethod: LavaFinanceBreakdown[];
};

export type LavaFinanceTransaction = {
  paymentId: string;
  checkoutId: string;
  operationType: "LAVA_WORK_ORDER" | string;
  module: "LAVA";
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

export type LavaFinancePageRequest = {
  from: string;
  to: string;
  page: number;
  size: number;
  search?: string;
};

export type LavaFinancePageResult = {
  summary: LavaFinanceSummary;
  transactions: LavaFinanceTransaction[];
  page: number;
  totalPages: number;
  totalElements: number;
};

export type OpenLavaWorkOrderRepositoryResult = {
  workOrder: LavaProgressVehicle;
  financeEntry?: LavaFinanceEntry;
};

export type SettleLavaFinanceEntryRepositoryResult = {
  financeEntry: LavaFinanceEntry;
  workOrder?: LavaProgressVehicle;
};

export interface LavaRepository {
  getSnapshot(): Promise<LavaRepositorySnapshot>;

  listHistory(
    input: LavaHistoryPageRequest
  ): Promise<LavaHistoryPageResult>;

  listFinance(
    input: LavaFinancePageRequest
  ): Promise<LavaFinancePageResult>;

  createService(
    input: LavaServiceInput
  ): Promise<LavaService>;

  updateService(
    serviceId: number,
    input: LavaServiceInput
  ): Promise<LavaService>;

  setServiceActive(
    serviceId: number,
    active: boolean
  ): Promise<LavaService>;

  removeService(
    serviceId: number
  ): Promise<void>;

  createClient(
    input: LavaClientInput
  ): Promise<LavaClient>;

  updateClient(
    clientId: number,
    input: LavaClientInput
  ): Promise<LavaClient>;

  setClientActive(
    clientId: number,
    active: boolean
  ): Promise<LavaClient>;

  removeClient(
    clientId: number
  ): Promise<void>;

  openWorkOrder(
    input: CreateLavaOrderInput
  ): Promise<OpenLavaWorkOrderRepositoryResult>;

  getWorkOrder(
    workOrderId: number
  ): Promise<LavaProgressVehicle>;

  payWorkOrder(
    input: LavaWorkOrderPaymentInput
  ): Promise<LavaWorkOrderPaymentResult>;

  changeWorkOrderStage(
    workOrderId: number,
    stage: LavaProgressStage
  ): Promise<LavaProgressVehicle>;

  settleFinanceEntry(
    input: SettleLavaFinanceEntryInput
  ): Promise<SettleLavaFinanceEntryRepositoryResult>;

  markServiceTimeNotified(
    workOrderId: number,
    notifiedAt: string
  ): Promise<LavaProgressVehicle>;
}
