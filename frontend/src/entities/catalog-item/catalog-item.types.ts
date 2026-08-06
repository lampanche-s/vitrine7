export type BarCatalogItemType =
  | "ITEM"
  | "SERVICE";

export type BarCatalogItem = {
  id: number;
  name: string;
  type: BarCatalogItemType;
  price: number;
  stockQuantity: number | null;
  minimumStockQuantity: number | null;
};

export type BarCatalogItemInput = Omit<
  BarCatalogItem,
  "id"
>;
