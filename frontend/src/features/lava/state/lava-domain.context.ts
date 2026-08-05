import {
  createContext,
} from "react";

import type {
  LavaClientInput,
} from "../../../entities/client";

import type {
  LavaWorkOrderPaymentInput,
  LavaWorkOrderPaymentResult,
  SettleLavaFinanceEntryInput,
} from "../../../entities/payment";

import type {
  LavaServiceInput,
} from "../../../entities/service";

import type {
  LavaFinancePageRequest,
  LavaFinancePageResult,
  LavaHistoryPageResult,
  OpenLavaWorkOrderRepositoryResult,
} from "../../../data/contracts";

import type {
  CreateLavaOrderInput,
  LavaProgressStage,
} from "../../../entities/work-order";

import type {
  LavaDomainState,
} from "./lava-domain.types";

export type LavaDomainContextValue = {
  state: LavaDomainState;

  isLoading: boolean;
  isMutating: boolean;
  error: string | null;

  reload: () => Promise<void>;

  createService: (
    input: LavaServiceInput
  ) => Promise<boolean>;

  updateService: (
    serviceId: number,
    input: LavaServiceInput
  ) => Promise<boolean>;

  setServiceActive: (
    serviceId: number,
    active: boolean
  ) => Promise<boolean>;

  removeService: (
    serviceId: number
  ) => Promise<boolean>;

  createClient: (
    input: LavaClientInput
  ) => Promise<boolean>;

  updateClient: (
    clientId: number,
    input: LavaClientInput
  ) => Promise<boolean>;

  setClientActive: (
    clientId: number,
    active: boolean
  ) => Promise<boolean>;

  removeClient: (
    clientId: number
  ) => Promise<boolean>;

  openWorkOrder: (
    input: CreateLavaOrderInput
  ) => Promise<OpenLavaWorkOrderRepositoryResult | null>;

  payWorkOrder: (
    input: LavaWorkOrderPaymentInput
  ) => Promise<LavaWorkOrderPaymentResult | null>;

  changeWorkOrderStage: (
    workOrderId: number,
    stage: LavaProgressStage
  ) => Promise<boolean>;

  settleFinanceEntry: (
    input: SettleLavaFinanceEntryInput
  ) => Promise<boolean>;

  listHistory: (input: {
    page: number;
    size: number;
    search?: string;
    from?: string;
    to?: string;
  }) => Promise<LavaHistoryPageResult | null>;

  listFinance: (
    input: LavaFinancePageRequest
  ) => Promise<LavaFinancePageResult | null>;

  markServiceTimeNotified: (
    workOrderId: number,
    notifiedAt: string
  ) => Promise<boolean>;
};

export const LavaDomainContext =
  createContext<LavaDomainContextValue | null>(null);
