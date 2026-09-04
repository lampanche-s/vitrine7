export type BarCatalogItemType =
  | "ITEM"
  | "SERVICE";

export type BarCatalogItem = {
  id: number;
  name: string;
  type: BarCatalogItemType;
  price: number;
  stockEnabled: boolean;
  stockQuantity: number | null;
  minimumStockQuantity: number | null;
  supplierId: number | null;
};

export type BarCatalogItemInput = Omit<
  BarCatalogItem,
  "id" | "supplierId"
> & {
  supplierId?: number | null;
};
