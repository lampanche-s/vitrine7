import type {
  BarCatalogItem,
  BarCatalogItemInput,
} from "./catalog-item.types";

export function normalizeBarCatalogItemInput(
  input: BarCatalogItemInput
): BarCatalogItemInput {
  const tracksStock =
    input.type === "ITEM" &&
    input.stockEnabled;

  return {
    name: input.name.trim(),
    type: input.type,
    price: Math.max(0, input.price),
    stockEnabled: tracksStock,
    stockQuantity: tracksStock
      ? Math.max(
          0,
          Math.floor(input.stockQuantity ?? 0)
        )
      : null,
    minimumStockQuantity: tracksStock
      ? Math.max(
          0,
          Math.floor(
            input.minimumStockQuantity ?? 0
          )
        )
      : null,
  };
}

export function isBarCatalogItemInputComplete(
  input: BarCatalogItemInput
): boolean {
  const hasValidStock =
    input.type === "SERVICE" ||
    !input.stockEnabled ||
    (
      Number.isInteger(input.stockQuantity) &&
      (input.stockQuantity ?? -1) >= 0 &&
      Number.isInteger(
        input.minimumStockQuantity
      ) &&
      (input.minimumStockQuantity ?? -1) >= 0
    );

  return (
    input.name.trim().length > 0 &&
    (input.type === "ITEM" ||
      input.type === "SERVICE") &&
    Number.isFinite(input.price) &&
    input.price > 0 &&
    hasValidStock
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
