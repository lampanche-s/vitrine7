export type BarPaymentMethod =
  | "Crédito"
  | "Débito"
  | "Cartão"
  | "Pix"
  | "Dinheiro";

export type BarHistoricalPaymentMethod =
  BarPaymentMethod;

export type BarReceiptDocument =
  "Recibo geral";

export type BarSaleHistoryEntry = {
  id: number;
  checkoutId?: string | null;
  paymentId?: string | null;
  paymentStatus?: string | null;
  paymentReversedAt?: string | null;
  paymentReversalReason?: string | null;
  origin: string;
  description: string;
  receiptItems?: {
    quantity: number;
    name: string;
    unitPrice: number;
    total: number;
  }[];
  amount: number;
  method: BarHistoricalPaymentMethod;
  document: BarReceiptDocument;
  cashReceived?: number | null;
  cashChange?: number | null;
  status?: string;
  time: string;
  completedAt?: string;
};
