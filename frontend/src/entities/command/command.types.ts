import type {
  BarPaymentMethod,
  BarReceiptDocument,
} from "../sale-history";

export type BarCommandStatus =
  | "open"
  | "awaitingPayment";

export type BarCommandItem = {
  key: string;
  catalogItemId: number;
  name: string;
  unitPrice: number;
  quantity: number;
};

export type BarCommand = {
  id: number;
  name: string;
  status: BarCommandStatus;
  checkoutId?: string | null;
  checkoutStatus?: string | null;
  openedAt: string;
  items: BarCommandItem[];
};

export type OpenBarCommandInput = {
  name: string;
  openedAt: string;
};

export type AddBarCommandItemInput = {
  catalogItemId: number;
  quantity?: number;
};

export type ResolvedBarCommandItemInput = {
  catalogItemId: number;
  name: string;
  unitPrice: number;
  quantity: number;
};

export type CloseBarCommandInput = {
  commandId: number;
  payment: BarPaymentMethod;
  document: BarReceiptDocument;
  cashReceived?: number;
  time: string;
};
