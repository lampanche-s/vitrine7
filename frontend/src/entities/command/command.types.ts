import type {
  BarPaymentMethod,
  BarReceiptDocument,
} from "../sale-history";

export type BarCommandStatus =
  | "open"
  | "awaitingPayment";

export type BarCommandItem = {
  key: string;
  lineId?: number;
  catalogItemId: number;
  entryType?: "ITEM" | "SERVICE";
  name: string;
  unitPrice: number;
  quantity: number;
};

export type BarCommand = {
  id: number;
  name: string;
  clientId?: number | null;
  clientName?: string | null;
  employeeId?: number | null;
  status: BarCommandStatus;
  checkoutId?: string | null;
  checkoutStatus?: string | null;
  vehicleName?: string | null;
  vehiclePlate?: string | null;
  openedAt: string;
  items: BarCommandItem[];
};

export type OpenBarCommandInput = {
  name: string;
  clientId?: number | null;
  employeeId?: number | null;
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

export type BarPaymentPartInput = {
  method: BarPaymentMethod;
  amount: number;
  cashReceived?: number;
};

export type CloseBarCommandInput = {
  commandId: number;
  payments?: BarPaymentPartInput[];
  payment?: BarPaymentMethod;
  cashReceived?: number;
  document: BarReceiptDocument;
  time: string;
  vehicleName?: string | null;
  vehiclePlate?: string | null;
};

export type VoucherBarCommandInput = {
  commandId: number;
  vehicleName?: string | null;
  vehiclePlate?: string | null;
};
