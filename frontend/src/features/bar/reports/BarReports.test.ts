import {
  describe,
  expect,
  it,
} from "vitest";

import type {
  BarSaleHistoryEntry,
} from "../../../entities/sale-history";

import {
  getLatestRows,
} from "./barReports.selectors";

function entry(
  id: number,
  completedAt: string
): BarSaleHistoryEntry {
  return {
    id,
    origin: `Comanda ${id}`,
    description: "1 item",
    amount: 10,
    method: "Dinheiro",
    document: "Recibo geral",
    time: completedAt,
    completedAt,
  };
}

describe("BarReports latest rows", () => {
  it("ordena pelos 5 timestamps reais mais recentes", () => {
    const rows = getLatestRows([
      entry(1, "2026-07-17T19:00:00Z"),
      entry(2, "2026-07-17T19:05:00Z"),
      entry(3, "2026-07-17T19:10:00Z"),
      entry(4, "2026-07-17T19:15:00Z"),
      entry(5, "2026-07-17T19:20:00Z"),
      entry(6, "2026-07-17T19:25:00Z"),
    ]);

    expect(rows.map((row) => row.id)).toEqual([
      6,
      5,
      4,
      3,
      2,
    ]);
  });
});
