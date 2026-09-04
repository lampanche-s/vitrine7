export type SalesReportScope =
  | "ALL"
  | "ITEM"
  | "SERVICE";

export type SalesReportPeriod = {
  from: string | null;
  to: string | null;
  timeZone: string;
};

export type SalesReportPaymentBreakdown = {
  method: string;
  amountCents: number;
  paymentCount: number;
  participationPercentage: number;
};

export type SalesReportTypeSummary = {
  entryType: "ITEM" | "SERVICE";
  revenueCents: number;
  quantity: number;
  participationPercentage: number;
};

export type SalesReportDaily = {
  date: string;
  operationCount: number;
  receivedCents: number;
  averageTicketCents: number;
};

export type SalesReportPerformance = {
  name: string;
  entryType: "ITEM" | "SERVICE";
  quantity: number;
  revenueCents: number;
  averagePriceCents: number;
};

export type SalesReportCatalogEntry = {
  name: string;
  entryType: "ITEM" | "SERVICE";
  quantity: number;
  grossCents: number;
};

export type SalesReportOperation = {
  operationId: number;
  displayName: string;
  completedAt: string;
  paymentMethod: string;
  responsibleUserName: string | null;
  grossCents: number;
  discountCents: number;
  netCents: number;
  lineCount: number;
  totalUnits: number;
  itemUnits: number;
  serviceUnits: number;
};

export type SalesReportLine = {
  operationId: number;
  displayName: string;
  completedAt: string;
  itemName: string;
  entryType: "ITEM" | "SERVICE";
  quantity: number;
  unitPriceCents: number;
  totalCents: number;
};

export type SalesReport = {
  scope: SalesReportScope;
  period: SalesReportPeriod;
  totalReceivedCents: number;
  operationCount: number;
  averageTicketCents: number;
  totalUnits: number;
  itemRevenueCents: number;
  serviceRevenueCents: number;
  itemUnits: number;
  serviceUnits: number;
  distribution: SalesReportTypeSummary[];
  byPaymentMethod: SalesReportPaymentBreakdown[];
  dailyEvolution: SalesReportDaily[];
  servicePerformance: SalesReportPerformance[];
  productPerformance: SalesReportPerformance[];
  topEntries: SalesReportCatalogEntry[];
  latestOperations: SalesReportOperation[];
  operations: SalesReportOperation[];
  lines: SalesReportLine[];
};

export type SystemBackupDownload = {
  blob: Blob;
  fileName: string;
};

export interface ReportsRepository {
  summary(input?: {
    from?: string;
    to?: string;
    scope?: SalesReportScope;
    reportPassword?: string;
  }): Promise<SalesReport>;

  export(input: {
    from: string;
    to: string;
    scope: Exclude<SalesReportScope, "ALL">;
    reportPassword?: string;
  }): Promise<SalesReport>;

  verifyProtectedReportPassword(password: string): Promise<void>;

  downloadSystemBackup(): Promise<SystemBackupDownload>;
}
