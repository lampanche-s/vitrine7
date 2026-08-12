export type CashClosingDay =
  | "TODAY"
  | "YESTERDAY";

export type CashClosingPaymentBreakdown = {
  method: string;
  amountCents: number;
  saleCount: number;
};

export type CashClosingOperation = {
  operationId: number;
  displayName: string;
  completedAt: string;
  paymentMethod: string;
  paymentStatus: "APPROVED" | "REVERSED" | string;
  amountCents: number;
  cashReceivedCents: number;
  cashChangeCents: number;
};

export type CashClosingReport = {
  businessDate: string;
  userName: string;
  closed: boolean;
  closedAt: string | null;
  grossSalesCents: number;
  totalReceivedCents: number;
  saleCount: number;
  averageTicketCents: number;
  reversedCents: number;
  reversedCount: number;
  cashReceivedCents: number;
  cashChangeCents: number;
  openCommandCount: number;
  openCommandAmountCents: number;
  firstSaleAt: string | null;
  lastSaleAt: string | null;
  paymentBreakdown: CashClosingPaymentBreakdown[];
  operations: CashClosingOperation[];
};

export interface CashClosingRepository {
  get(day: CashClosingDay): Promise<CashClosingReport>;
  close(day: CashClosingDay): Promise<CashClosingReport>;
}
