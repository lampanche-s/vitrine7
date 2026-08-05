import type {
  BarCatalogItem,
  BarCatalogItemInput,
} from "./catalog-item.types";

export function normalizeBarCatalogItemInput(
  input: BarCatalogItemInput
): BarCatalogItemInput {
  return {
    name: input.name.trim(),
    type: input.type,
    price: Math.max(0, input.price),
  };
}

export function isBarCatalogItemInputComplete(
  input: BarCatalogItemInput
): boolean {
  return (
    input.name.trim().length > 0 &&
    (input.type === "ITEM" ||
      input.type === "SERVICE") &&
    Number.isFinite(input.price) &&
    input.price > 0
  );
}

export function createBarCatalogItem(
  items: readonly BarCatalogItem[],
  item: BarCatalogItem
): BarCatalogItem[] {
  return [item, ...items];
}

export function updateBarCatalogItem(
  items: readonly BarCatalogItem[],
  updatedItem: BarCatalogItem
): BarCatalogItem[] {
  return items.map((item) =>
    item.id === updatedItem.id
      ? updatedItem
      : item
  );
}

export function removeBarCatalogItem(
  items: readonly BarCatalogItem[],
  itemId: number
): BarCatalogItem[] {
  return items.filter(
    (item) => item.id !== itemId
  );
}
