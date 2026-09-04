import {
  describe,
  expect,
  it,
} from "vitest";

import type {
  BarCommand,
} from "../../../entities/command";

import type {
  BarCatalogItem,
} from "../../../entities/catalog-item";

import {
  closeBarCommand,
  shouldRequestVehicleDetails,
} from "./close-command.rules";

const service: BarCatalogItem = {
  id: 1,
  name: "Lavagem",
  type: "SERVICE",
  price: 30,
  stockEnabled: false,
  stockQuantity: null,
  minimumStockQuantity: null,
  supplierId: null,
};

const item: BarCatalogItem = {
  ...service,
  id: 2,
  name: "Refrigerante",
  type: "ITEM",
};

function commandWith(
  clientId: number | null,
  catalogItemId: number
): BarCommand {
  return {
    id: 1,
    name: "Comanda 1",
    clientId,
    status: "open",
    openedAt: "12:00",
    items: [{
      key: "1",
      catalogItemId,
      name: "Produto",
      unitPrice: 10,
      quantity: 1,
    }],
  };
}

describe("closeBarCommand", () => {
  it.each<[string, number | null, number, boolean]>([
    ["avulsa com SERVICE", null, 1, true],
    ["avulsa somente com ITEM", null, 2, false],
    ["cliente cadastrado com SERVICE", 10, 1, false],
    ["cliente cadastrado somente com ITEM", 10, 2, false],
  ])(
    "solicita veículo apenas para comanda %s",
    (_description, clientId, catalogItemId, expected) => {
      expect(
        shouldRequestVehicleDetails(
          commandWith(clientId, catalogItemId),
          [service, item]
        )
      ).toBe(expected);
    }
  );

  it("preserva itens nominais com quantidade e valores para o recibo", () => {
    const command: BarCommand = {
      id: 7,
      name: "Comanda 7",
      status: "awaitingPayment",
      openedAt: "12:00",
      items: [
        {
          key: "1",
          catalogItemId: 1,
          name: "Espeto bovino",
          unitPrice: 8,
          quantity: 2,
        },
        {
          key: "2",
          catalogItemId: 2,
          name: "Refrigerante lata",
          unitPrice: 6,
          quantity: 1,
        },
      ],
    };

    const result = closeBarCommand(
      [command],
      [],
      {
        commandId: 7,
        payment: "Dinheiro",
        document: "Recibo geral",
        cashReceived: 25,
        time: "18/07/2026, 12:30",
      }
    );

    expect(result.historyEntry.receiptItems).toEqual([
      {
        quantity: 2,
        name: "Espeto bovino",
        unitPrice: 8,
        total: 16,
      },
      {
        quantity: 1,
        name: "Refrigerante lata",
        unitPrice: 6,
        total: 6,
      },
    ]);
    expect(result.historyEntry.description).toBe(
      "2x Espeto bovino, 1x Refrigerante lata"
    );
    expect(result.historyEntry.cashReceived).toBe(25);
    expect(result.historyEntry.cashChange).toBe(3);
  });
});
