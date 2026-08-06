import type {
  BarDomainState,
} from "../../features/bar/state";

export function createInitialBarDomainState(): BarDomainState {
  return {
    commands: [
      {
        id: 1,
        name: "Mesa 01",
        status: "open",
        openedAt: "19:00",
        items: [
          {
            key: "1",
            catalogItemId: 1,
            name: "Espeto bovino",
            unitPrice: 18,
            quantity: 2,
          },
        ],
      },
    ],
    catalogEntries: [
      {
        id: 1,
        name: "Espeto bovino",
        type: "ITEM",
        price: 18,
        stockQuantity: 10,
        minimumStockQuantity: 3,
      },
      {
        id: 2,
        name: "Taxa de entrega",
        type: "SERVICE",
        price: 5,
        stockQuantity: null,
        minimumStockQuantity: null,
      },
    ],
    historyEntries: [],
  };
}

export function createEmptyBarDomainState(): BarDomainState {
  return {
    commands: [],
    catalogEntries: [],
    historyEntries: [],
  };
}
