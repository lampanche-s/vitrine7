export type Supplier = {
  id: number;
  name: string;
  cnpj: string | null;
  phone: string | null;
  cep: string | null;
};

export type SupplierInput = Omit<Supplier, "id">;

export type SupplierCatalogItem = {
  id: number;
  name: string;
  price: number;
  stockEnabled: boolean;
  stockQuantity: number | null;
  minimumStockQuantity: number | null;
};

export type SupplierDetail = Supplier & {
  catalogEntries: SupplierCatalogItem[];
};
