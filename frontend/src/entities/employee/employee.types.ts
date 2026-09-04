export type Employee = { id: number; name: string };
export type EmployeeInput = { name: string };

export type EmployeeVoucher = {
  operationId: number;
  employeeId: number;
  employeeName: string;
  registeredTabName: string;
  closedAt: string;
  total: number;
  lineCount: number;
  totalUnits: number;
  operatorName: string;
  lines: { catalogEntryId: number; itemName: string; type: "ITEM" | "SERVICE"; quantity: number; unitPrice: number; total: number }[];
};

export type EmployeeVoucherReport = {
  summary: { voucherValue: number; voucherCount: number; averageVoucher: number; consumedUnits: number };
  byEmployee: { employeeId: number; employeeName: string; voucherCount: number; total: number; consumption: number }[];
  byEntry: { catalogEntryId: number; name: string; type: "ITEM" | "SERVICE"; quantity: number; total: number; evolutionPercent: number | null }[];
  byDay: { date: string; voucherCount: number; total: number }[];
};
