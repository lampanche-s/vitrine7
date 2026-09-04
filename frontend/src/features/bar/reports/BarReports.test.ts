import {
  describe,
  expect,
  it,
} from "vitest";
import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";

import type { SalesReport } from "../../../data/contracts";

import type {
  BarSaleHistoryEntry,
} from "../../../entities/sale-history";

import {
  createReportRequestGuard,
  getLatestRows,
} from "./barReports.selectors";
import {
  BarReportsQueryError,
  BarReportsSummary,
} from "./BarReports";
import { requiresPasswordForPeriodSelection } from "../../reports/report-period";

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

const report: SalesReport = {
  scope: "ALL",
  period: {
    from: "2026-08-01T03:00:00Z",
    to: "2026-08-26T03:00:00Z",
    timeZone: "America/Bahia",
  },
  totalReceivedCents: 12345,
  operationCount: 2,
  averageTicketCents: 6173,
  totalUnits: 7,
  itemRevenueCents: 3345,
  serviceRevenueCents: 9000,
  itemUnits: 4,
  serviceUnits: 3,
  distribution: [
    { entryType: "ITEM", revenueCents: 3345, quantity: 4, participationPercentage: 27.1 },
    { entryType: "SERVICE", revenueCents: 9000, quantity: 3, participationPercentage: 72.9 },
  ],
  byPaymentMethod: [
    {
      method: "PIX",
      amountCents: 12345,
      paymentCount: 2,
      participationPercentage: 100,
    },
  ],
  dailyEvolution: [
    { date: "2026-08-25", operationCount: 2, receivedCents: 12345, averageTicketCents: 6173 },
  ],
  servicePerformance: [
    { name: "Lavagem completa", entryType: "SERVICE", quantity: 3, revenueCents: 9000, averagePriceCents: 3000 },
  ],
  productPerformance: [],
  topEntries: [
    {
      name: "Lavagem completa",
      entryType: "SERVICE",
      quantity: 3,
      grossCents: 9000,
    },
  ],
  latestOperations: [
    {
      operationId: 42,
      displayName: "Comanda 42",
      completedAt: "2026-08-25T15:30:00Z",
      paymentMethod: "PIX",
      responsibleUserName: "Ana",
      grossCents: 12345,
      discountCents: 0,
      netCents: 12345,
      lineCount: 1,
      totalUnits: 7,
      itemUnits: 4,
      serviceUnits: 3,
    },
  ],
  operations: [
    {
      operationId: 42,
      displayName: "Comanda 42",
      completedAt: "2026-08-25T15:30:00Z",
      paymentMethod: "PIX",
      responsibleUserName: "Ana",
      grossCents: 12345,
      discountCents: 0,
      netCents: 12345,
      lineCount: 1,
      totalUnits: 7,
      itemUnits: 4,
      serviceUnits: 3,
    },
  ],
  lines: [],
};

describe("BarReports summary", () => {
  it.each([
    ["today", false],
    ["previousDay", false],
    ["currentWeek", true],
    ["previousWeek", true],
    ["currentMonth", true],
    ["previousMonth", true],
    ["custom", true],
  ] as const)("guards selection of %s: %s", (nextPeriod, expected) => {
    expect(requiresPasswordForPeriodSelection("today", nextPeriod)).toBe(expected);
  });

  it("requires a new password when changing between protected periods", () => {
    expect(
      requiresPasswordForPeriodSelection("currentMonth", "previousWeek")
    ).toBe(true);
  });

  it("allows returning from a protected period to today", () => {
    expect(
      requiresPasswordForPeriodSelection("currentMonth", "today")
    ).toBe(false);
  });
  it("renders units, top entries and payment amount with count", () => {
    const html = renderToStaticMarkup(
      createElement(BarReportsSummary, { report })
    );

    expect(html).toContain("Unidades vendidas");
    expect(html).toContain(">7<");
    expect(html).toContain("Lavagem completa");
    expect(html).toContain("Preço médio praticado");
    expect(html).toContain("Pix");
    expect(html).toContain("2 pagamentos");
    expect(html).toContain("Comanda 42");
    expect(html).toContain("Ana");
  });

  it("renders explicit empty states", () => {
    const emptyReport: SalesReport = {
      ...report,
      totalReceivedCents: 0,
      operationCount: 0,
      averageTicketCents: 0,
      totalUnits: 0,
      itemRevenueCents: 0,
      serviceRevenueCents: 0,
      itemUnits: 0,
      serviceUnits: 0,
      distribution: [],
      byPaymentMethod: [],
      dailyEvolution: [],
      servicePerformance: [],
      productPerformance: [],
      topEntries: [],
      latestOperations: [],
      operations: [],
    };
    const html = renderToStaticMarkup(
      createElement(BarReportsSummary, { report: emptyReport })
    );

    expect(html).toContain("Nenhum pagamento concluído no período.");
    expect(html).toContain("Nenhuma receita recebida no período.");
    expect(html).toContain("Nenhuma operação concluída no período.");
  });

  it("renders query errors accessibly", () => {
    const html = renderToStaticMarkup(
      createElement(BarReportsQueryError, {
        message: "Falha ao consultar o relatório.",
      })
    );

    expect(html).toContain('role="alert"');
    expect(html).toContain("Falha ao consultar o relatório.");
  });

  it("invalidates an older response when filters change", () => {
    const guard = createReportRequestGuard();
    const firstRequestIsCurrent = guard.begin();
    const secondRequestIsCurrent = guard.begin();

    expect(firstRequestIsCurrent()).toBe(false);
    expect(secondRequestIsCurrent()).toBe(true);
  });
});
