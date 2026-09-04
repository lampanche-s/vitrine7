import type { Supplier, SupplierDetail, SupplierInput } from "../../entities/supplier";

export interface SuppliersRepository {
  list(): Promise<Supplier[]>;
  detail(supplierId: number): Promise<SupplierDetail>;
  create(input: SupplierInput): Promise<Supplier>;
  update(supplierId: number, input: SupplierInput): Promise<Supplier>;
  remove(supplierId: number): Promise<void>;
}
