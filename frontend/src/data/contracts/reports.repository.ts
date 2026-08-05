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
  operationCount: number;
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
  byPaymentMethod: SalesReportPaymentBreakdown[];
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
  }): Promise<SalesReport>;

  export(input: {
    from: string;
    to: string;
    scope: Exclude<SalesReportScope, "ALL">;
  }): Promise<SalesReport>;

  downloadSystemBackup(): Promise<SystemBackupDownload>;
}
