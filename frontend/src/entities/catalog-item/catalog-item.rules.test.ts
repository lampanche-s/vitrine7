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
        stockEnabled: true,
        stockQuantity: 10.9,
        minimumStockQuantity: 2.8,
      })
    ).toEqual({
      name: "Espeto bovino",
      type: "ITEM",
      price: 18,
      stockEnabled: true,
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
        stockEnabled: true,
        stockQuantity: 20,
        minimumStockQuantity: 5,
      })
    ).toMatchObject({
      type: "SERVICE",
      stockEnabled: false,
      stockQuantity: null,
      minimumStockQuantity: null,
    });
  });

  it("mantém fornecedor somente para item", () => {
    expect(normalizeBarCatalogItemInput({
      name: "Produto",
      type: "ITEM",
      price: 12,
      stockEnabled: false,
      stockQuantity: null,
      minimumStockQuantity: null,
      supplierId: 7,
    }).supplierId).toBe(7);

    expect(normalizeBarCatalogItemInput({
      name: "Serviço",
      type: "SERVICE",
      price: 12,
      stockEnabled: false,
      stockQuantity: null,
      minimumStockQuantity: null,
      supplierId: 7,
    }).supplierId).toBeNull();
  });

  it("remove estoque quando o controle está desligado", () => {
    expect(
      normalizeBarCatalogItemInput({
        name: "Produto livre",
        type: "ITEM",
        price: 12,
        stockEnabled: false,
        stockQuantity: -20,
        minimumStockQuantity: -5,
      })
    ).toMatchObject({
      stockEnabled: false,
      stockQuantity: null,
      minimumStockQuantity: null,
    });

    expect(
      isBarCatalogItemInputComplete({
        name: "Produto livre",
        type: "ITEM",
        price: 12,
        stockEnabled: false,
        stockQuantity: null,
        minimumStockQuantity: null,
      })
    ).toBe(true);
  });

  it("exige estoque para item com controle ativo", () => {
    expect(
      isBarCatalogItemInputComplete({
        name: "Espeto",
        type: "ITEM",
        price: 18,
        stockEnabled: true,
        stockQuantity: null,
        minimumStockQuantity: 2,
      })
    ).toBe(false);
  });
});
