import type { Supplier, SupplierInput } from "../../entities/supplier";
import type { SuppliersRepository } from "../contracts/suppliers.repository";
import { httpClient } from "../../shared/http";
import { listAllPages, type HttpPageResponse } from "./listAllPages";

type SupplierResponse = {
  id: number;
  name: string;
  cnpj: string | null;
  phone: string | null;
  cep: string | null;
};

type SupplierDetailResponse = SupplierResponse & {
  catalogEntries: {
    id: number;
    name: string;
    priceCents: number;
    stockEnabled: boolean;
    stockQuantity: number | null;
    minimumStockQuantity: number | null;
  }[];
};

function mapSupplier(supplier: SupplierResponse): Supplier {
  return { id: supplier.id, name: supplier.name, cnpj: supplier.cnpj, phone: supplier.phone, cep: supplier.cep };
}

function body(input: SupplierInput) {
  return { name: input.name, cnpj: input.cnpj || null, phone: input.phone || null, cep: input.cep || null };
}

export const httpSuppliersRepository: SuppliersRepository = {
  async list() {
    const suppliers = await listAllPages<SupplierResponse>((page) =>
      httpClient.get<HttpPageResponse<SupplierResponse>>(`/suppliers?page=${page}&size=100&sort=name&direction=ASC`)
    );
    return suppliers.map(mapSupplier);
  },
  async detail(supplierId) {
    const supplier = await httpClient.get<SupplierDetailResponse>(`/suppliers/${supplierId}`);
    return {
      ...mapSupplier(supplier),
      catalogEntries: supplier.catalogEntries.map((entry) => ({
        id: entry.id, name: entry.name, price: entry.priceCents / 100,
        stockEnabled: entry.stockEnabled, stockQuantity: entry.stockQuantity,
        minimumStockQuantity: entry.minimumStockQuantity,
      })),
    };
  },
  async create(input) { return mapSupplier(await httpClient.post<SupplierResponse>("/suppliers", body(input))); },
  async update(supplierId, input) { return mapSupplier(await httpClient.put<SupplierResponse>(`/suppliers/${supplierId}`, body(input))); },
  async remove(supplierId) { await httpClient.delete<void>(`/suppliers/${supplierId}`); },
};
