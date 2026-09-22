import {
  createContext,
  type Dispatch,
} from "react";

import type {
  BarCatalogItem,
  BarCatalogItemInput,
} from "../../../entities/catalog-item";

import type {
  AddBarCommandItemInput,
  BarCommand,
  BarCommandStatus,
  CloseBarCommandInput,
  OpenBarCommandInput,
  VoucherBarCommandInput,
} from "../../../entities/command";

import type {
  BarHistoryPageRequest,
  BarHistoryPageResult,
} from "../../../data/contracts";

import type {
  BarDomainAction,
  BarDomainState,
} from "./bar-domain.types";

export type BarDomainContextValue = {
  state: BarDomainState;
  dispatch: Dispatch<BarDomainAction>;
  isLoading: boolean;
  isMutating: boolean;
  error: string | null;
  reload: () => Promise<void>;
  clearError: () => void;
  listHistory: (
    input: BarHistoryPageRequest
  ) => Promise<BarHistoryPageResult | null>;
  openCommand: (
    input: OpenBarCommandInput
  ) => Promise<BarCommand | null>;
  reopenCommand: (
    commandId: number
  ) => Promise<BarCommand | null>;
  setCommandStatus: (
    commandId: number,
    status: BarCommandStatus
  ) => Promise<boolean>;
  printPrePaymentNote: (commandId: number) => Promise<boolean>;
  printItems: (tabId: number) => Promise<boolean>;
  printServices: (tabId: number) => Promise<boolean>;
  printLine: (tabId: number, lineId: number) => Promise<boolean>;
  addCommandItem: (
    commandId: number,
    input: AddBarCommandItemInput
  ) => Promise<boolean>;
  updateCommandItemQuantity: (
    commandId: number,
    itemKey: string,
    quantity: number,
    unitPrice?: number
  ) => Promise<boolean>;
  removeCommandItem: (
    commandId: number,
    itemKey: string
  ) => Promise<boolean>;
  cancelCommand: (
    commandId: number
  ) => Promise<boolean>;
  closeCommand: (
    input: CloseBarCommandInput
  ) => Promise<BarCommand | null>;
  closeVoucher: (input: VoucherBarCommandInput) => Promise<BarCommand | null>;
  createCatalogEntry: (
    input: BarCatalogItemInput
  ) => Promise<BarCatalogItem | null>;
  verifyCatalogPassword: (password: string) => Promise<boolean>;
  updateCatalogEntry: (
    entryId: number,
    input: BarCatalogItemInput,
    password: string
  ) => Promise<BarCatalogItem | null>;
  removeCatalogEntry: (
    entryId: number,
    password: string
  ) => Promise<boolean>;
};

export const BarDomainContext =
  createContext<BarDomainContextValue | null>(null);
