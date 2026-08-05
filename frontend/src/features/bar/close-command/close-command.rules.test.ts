import {
  describe,
  expect,
  it,
} from "vitest";

import type {
  BarCommand,
} from "../../../entities/command";

import {
  closeBarCommand,
} from "./close-command.rules";

describe("closeBarCommand", () => {
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
