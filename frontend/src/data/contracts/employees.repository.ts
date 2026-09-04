import type { Employee, EmployeeInput, EmployeeVoucher, EmployeeVoucherReport } from "../../entities/employee";

export type VoucherFilters = { employeeId?: number; from?: string; to?: string };

export interface EmployeesRepository {
  list(): Promise<Employee[]>;
  create(input: EmployeeInput): Promise<Employee>;
  update(id: number, input: EmployeeInput): Promise<Employee>;
  remove(id: number): Promise<void>;
  listVouchers(filters?: VoucherFilters): Promise<EmployeeVoucher[]>;
  report(filters?: VoucherFilters): Promise<EmployeeVoucherReport>;
}
