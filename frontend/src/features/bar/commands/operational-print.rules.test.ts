import {
  describe,
  expect,
  it,
} from "vitest";

import type {
  BarCommand,
  BarCommandItem,
} from "../../../entities/command";

import {
  canPrintItems,
  canPrintLine,
  canPrintServices,
  operationalPrintLineLabel,
} from "./operational-print.rules";

function line(
  lineId: number,
  entryType: "ITEM" | "SERVICE"
): BarCommandItem {
  return {
    key: `catalog:${lineId}`,
    lineId,
    catalogItemId: lineId,
    entryType,
    name: entryType === "ITEM" ? "Água" : "Lavagem",
    unitPrice: 10,
    quantity: 1,
  };
}

function command(
  status: BarCommand["status"],
  items: BarCommandItem[]
): BarCommand {
  return {
    id: 1,
    name: "Mesa 1",
    status,
    openedAt: "10:00",
    items,
  };
}

describe("regras de impressão operacional", () => {
  it("habilita cada impressão coletiva somente quando há linha do tipo correspondente", () => {
    const item = line(10, "ITEM");
    const service = line(20, "SERVICE");

    expect(canPrintItems(command("open", [item]))).toBe(true);
    expect(canPrintItems(command("open", [service]))).toBe(false);
    expect(canPrintServices(command("open", [service]))).toBe(true);
    expect(canPrintServices(command("open", [item]))).toBe(false);
  });

  it("define uma única ação individual a partir do tipo estrutural", () => {
    const item = line(10, "ITEM");
    const service = line(20, "SERVICE");
    const open = command("open", [item, service]);

    expect(canPrintLine(open, item)).toBe(true);
    expect(operationalPrintLineLabel(item)).toBe("Imprimir este item");
    expect(canPrintLine(open, service)).toBe(true);
    expect(operationalPrintLineLabel(service)).toBe("Imprimir este serviço");
  });

  it("bloqueia todas as impressões fora do consumo", () => {
    const item = line(10, "ITEM");
    const service = line(20, "SERVICE");
    const pending = command("awaitingPayment", [item, service]);

    expect(canPrintItems(pending)).toBe(false);
    expect(canPrintServices(pending)).toBe(false);
    expect(canPrintLine(pending, item)).toBe(false);
    expect(canPrintLine(pending, service)).toBe(false);
  });
});
