import type { EmployeeVoucher } from "../../entities/employee";
import type {
  EmployeesRepository,
  VoucherFilters,
} from "../contracts/employees.repository";
import { httpClient } from "../../shared/http";
import { listAllPages, type HttpPageResponse } from "./listAllPages";

type EmployeeResponse = { id: number; name: string };
type VoucherResponse = Omit<EmployeeVoucher, "total" | "lines"> & {
  totalCents: number;
  lines: {
    catalogEntryId: number;
    itemName: string;
    type: "ITEM" | "SERVICE";
    quantity: number;
    unitPriceCents: number;
    lineTotalCents: number;
  }[];
};
type ReportResponse = {
  summary: {
    voucherValueCents: number;
    voucherCount: number;
    averageVoucherCents: number;
    consumedUnits: number;
  };
  byEmployee: {
    employeeId: number;
    employeeName: string;
    voucherCount: number;
    totalCents: number;
    consumption: number;
  }[];
  byEntry: {
    catalogEntryId: number;
    name: string;
    type: "ITEM" | "SERVICE";
    quantity: number;
    totalCents: number;
    evolutionPercent: number | null;
  }[];
  byDay: { date: string; voucherCount: number; totalCents: number }[];
};

function query(filters: VoucherFilters = {}) {
  const params = new URLSearchParams();
  if (filters.employeeId) params.set("employeeId", String(filters.employeeId));
  if (filters.from) params.set("from", filters.from);
  if (filters.to) params.set("to", filters.to);
  const value = params.toString();
  return value ? `?${value}` : "";
}

function mapVoucher(v: VoucherResponse): EmployeeVoucher {
  return {
    ...v,
    total: v.totalCents / 100,
    lines: v.lines.map((line) => ({
      catalogEntryId: line.catalogEntryId,
      itemName: line.itemName,
      type: line.type,
      quantity: line.quantity,
      unitPrice: line.unitPriceCents / 100,
      total: line.lineTotalCents / 100,
    })),
  };
}

export const httpEmployeesRepository: EmployeesRepository = {
  async list() {
    return listAllPages<EmployeeResponse>((page) =>
      httpClient.get<HttpPageResponse<EmployeeResponse>>(
        `/employees?page=${page}&size=100`,
      ),
    );
  },
  async create(input) {
    return httpClient.post<EmployeeResponse>("/employees", {
      name: input.name.trim(),
    });
  },
  async update(id, input) {
    return httpClient.put<EmployeeResponse>(`/employees/${id}`, {
      name: input.name.trim(),
    });
  },
  async remove(id) {
    await httpClient.delete<void>(`/employees/${id}`);
  },
  async listVouchers(filters) {
    return (
      await listAllPages<VoucherResponse>((page) =>
        httpClient.get<HttpPageResponse<VoucherResponse>>(
          `/employees/vouchers${query({ ...filters })}${query(filters) ? "&" : "?"}page=${page}&size=100`,
        ),
      )
    ).map(mapVoucher);
  },
  async report(filters) {
    const response = await httpClient.get<ReportResponse>(
      `/employees/vouchers/report${query(filters)}`,
    );
    return {
      summary: {
        voucherValue: response.summary.voucherValueCents / 100,
        voucherCount: response.summary.voucherCount,
        averageVoucher: response.summary.averageVoucherCents / 100,
        consumedUnits: response.summary.consumedUnits,
      },
      byEmployee: response.byEmployee.map((row) => ({
        ...row,
        total: row.totalCents / 100,
      })),
      byEntry: response.byEntry.map((row) => ({
        ...row,
        total: row.totalCents / 100,
      })),
      byDay: response.byDay.map((row) => ({
        ...row,
        total: row.totalCents / 100,
      })),
    };
  },
};
