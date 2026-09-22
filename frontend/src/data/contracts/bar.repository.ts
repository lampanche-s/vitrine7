import type {
  BarCatalogItem,
  BarCatalogItemInput,
} from "../../entities/catalog-item";

import type {
  AddBarCommandItemInput,
  BarCommand,
  BarCommandStatus,
  CloseBarCommandInput,
  OpenBarCommandInput,
  VoucherBarCommandInput,
} from "../../entities/command";

import type {
  BarSaleHistoryEntry,
} from "../../entities/sale-history";

export type BarRepositorySnapshot = {
  commands: BarCommand[];
  catalogEntries: BarCatalogItem[];
  historyEntries: BarSaleHistoryEntry[];
};

export type BarHistoryPageRequest = {
  page: number;
  size: number;
  search?: string;
  from?: string;
  to?: string;
};

export type BarHistoryPageResult = {
  entries: BarSaleHistoryEntry[];
  page: number;
  totalPages: number;
  totalElements: number;
};

export type CloseBarCommandRepositoryResult = {
  closedCommand: BarCommand;
  historyEntry: BarSaleHistoryEntry;
  catalogEntries: BarCatalogItem[] | null;
};

export interface BarRepository {
  getSnapshot(): Promise<BarRepositorySnapshot>;

  listHistory(
    input: BarHistoryPageRequest
  ): Promise<BarHistoryPageResult>;

  openCommand(
    input: OpenBarCommandInput
  ): Promise<BarCommand>;

  reopenCommand(
    commandId: number
  ): Promise<BarCommand>;

  setCommandStatus(
    commandId: number,
    status: BarCommandStatus
  ): Promise<BarCommand>;

  printPrePaymentNote(commandId: number): Promise<void>;

  printItems(tabId: number): Promise<void>;

  printServices(tabId: number): Promise<void>;

  printLine(tabId: number, lineId: number): Promise<void>;

  addCommandItem(
    commandId: number,
    input: AddBarCommandItemInput
  ): Promise<BarCommand>;

  updateCommandItemQuantity(
    commandId: number,
    itemKey: string,
    quantity: number,
    unitPrice?: number
  ): Promise<BarCommand>;

  removeCommandItem(
    commandId: number,
    itemKey: string
  ): Promise<BarCommand>;

  cancelCommand(
    commandId: number
  ): Promise<void>;

  closeCommand(
    input: CloseBarCommandInput
  ): Promise<CloseBarCommandRepositoryResult>;

  closeVoucher(input: VoucherBarCommandInput): Promise<BarCommand>;

  createCatalogEntry(
    input: BarCatalogItemInput
  ): Promise<BarCatalogItem>;

  verifyCatalogPassword(password: string): Promise<boolean>;

  updateCatalogEntry(
    entryId: number,
    input: BarCatalogItemInput,
    password: string
  ): Promise<BarCatalogItem>;

  removeCatalogEntry(
    entryId: number,
    password: string
  ): Promise<void>;
}
