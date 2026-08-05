export type BarCatalogItemType =
  | "ITEM"
  | "SERVICE";

export type BarCatalogItem = {
  id: number;
  name: string;
  type: BarCatalogItemType;
  price: number;
};

export type BarCatalogItemInput = Omit<
  BarCatalogItem,
  "id"
>;
