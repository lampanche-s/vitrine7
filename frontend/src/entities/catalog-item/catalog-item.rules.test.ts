import {
  describe,
  expect,
  it,
} from "vitest";

import {
  isBarCatalogItemInputComplete,
  normalizeBarCatalogItemInput,
} from "./catalog-item.rules";

describe("catalog-item.rules", () => {
  it("normaliza estoque de item para inteiros não negativos", () => {
    expect(
      normalizeBarCatalogItemInput({
        name: "  Espeto bovino  ",
        type: "ITEM",
        price: 18,
        stockQuantity: 10.9,
        minimumStockQuantity: 2.8,
      })
    ).toEqual({
      name: "Espeto bovino",
      type: "ITEM",
      price: 18,
      stockQuantity: 10,
      minimumStockQuantity: 2,
    });
  });

  it("remove estoque de serviço", () => {
    expect(
      normalizeBarCatalogItemInput({
        name: "Lavagem",
        type: "SERVICE",
        price: 30,
        stockQuantity: 20,
        minimumStockQuantity: 5,
      })
    ).toMatchObject({
      type: "SERVICE",
      stockQuantity: null,
      minimumStockQuantity: null,
    });
  });

  it("exige estoque para item", () => {
    expect(
      isBarCatalogItemInputComplete({
        name: "Espeto",
        type: "ITEM",
        price: 18,
        stockQuantity: null,
        minimumStockQuantity: 2,
      })
    ).toBe(false);
  });
});
