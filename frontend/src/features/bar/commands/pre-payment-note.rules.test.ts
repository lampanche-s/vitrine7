import {
  describe,
  expect,
  it,
} from "vitest";

import {
  canPrintPrePaymentNote,
} from "./pre-payment-note.rules";

describe("canPrintPrePaymentNote", () => {
  it("habilita a nota para uma comanda aberta com consumo", () => {
    expect(canPrintPrePaymentNote({
      status: "open",
      items: [{}],
    } as never)).toBe(true);
  });

  it("desabilita a nota para uma comanda vazia", () => {
    expect(canPrintPrePaymentNote({
      status: "open",
      items: [],
    } as never)).toBe(false);
  });
});
