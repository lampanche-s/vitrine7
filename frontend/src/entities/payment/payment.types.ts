export type LavaFinanceStatus = "paid" | "pending";

export type LavaPaymentMethod =
  | "Crédito"
  | "Débito"
  | "Cartão"
  | "Pix"
  | "Dinheiro";

export type LavaHistoricalPaymentMethod =
  | LavaPaymentMethod;

export type LavaFinanceEntry = {
  id: number;
  workOrderId: number | null;
  vehicle: string;
  plate: string;
  service: string;
  amount: number;
  method: LavaHistoricalPaymentMethod | "A definir";
  document: string;
  status: LavaFinanceStatus;
  time: string;
};

export type SettleLavaFinanceEntryInput = {
  entryId: number;
  method: LavaPaymentMethod;
  document: string;
  time: string;
  paidAt?: string;
};

export type LavaWorkOrderPaymentMode =
  | "terminal"
  | "cash"
  | "manual";

export type LavaWorkOrderPaymentInput = {
  workOrderId: number;
  method: LavaPaymentMethod;
  mode: LavaWorkOrderPaymentMode;
  document: string;
  cashReceived?: number;
  manualReason?: string;
};

export type LavaWorkOrderPaymentResult = {
  workOrder: import("../work-order").LavaProgressVehicle;
  approved: boolean;
  message: string;
};
