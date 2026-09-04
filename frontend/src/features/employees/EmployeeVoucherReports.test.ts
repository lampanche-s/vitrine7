import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import type { EmployeeVoucherReport } from "../../entities/employee";
import {
  EmployeeVoucherReportError,
  EmployeeVoucherReportResults,
} from "./EmployeeVoucherReports";
import { resolveVoucherReportRange } from "./employeeVoucherReport.filters";

const report: EmployeeVoucherReport = {
  summary: { voucherValue: 790.75, voucherCount: 2, averageVoucher: 395.375, consumedUnits: 4 },
  byEmployee: [{ employeeId: 1, employeeName: "Cibele", voucherCount: 2, total: 790.75, consumption: 4 }],
  byEntry: [{ catalogEntryId: 3, name: "Produto", type: "ITEM", quantity: 4, total: 790.75, evolutionPercent: null }],
  byDay: [{ date: "2026-08-27", voucherCount: 2, total: 790.75 }],
};

describe("EmployeeVoucherReports", () => {
  it("renderiza as métricas e tabelas quando voucherCount é 2", () => {
    const html = renderToStaticMarkup(createElement(EmployeeVoucherReportResults, { report }));
    expect(html).toContain("Quantidade de Vales");
    expect(html).toContain(">2<");
    expect(html).toContain("R$ 790,75");
    expect(html).toContain("Cibele");
    expect(html).toContain("Produto");
  });

  it("só mostra o estado vazio para resposta válida com contagem zero", () => {
    const html = renderToStaticMarkup(createElement(EmployeeVoucherReportResults, { report: { ...report, summary: { voucherValue: 0, voucherCount: 0, averageVoucher: 0, consumedUnits: 0 } } }));
    expect(html).toContain("Nenhum Vale encontrado para os filtros selecionados.");
    expect(html).not.toContain("Quantidade de Vales");
  });

  it("torna o erro de consulta visível", () => {
    const html = renderToStaticMarkup(createElement(EmployeeVoucherReportError, { message: "Falha ao consultar relatório." }));
    expect(html).toContain('role="alert"');
    expect(html).toContain("Falha ao consultar relatório.");
  });

  it("resolve Hoje como intervalo local fechado-aberto", () => {
    const result = resolveVoucherReportRange("today", "", "", new Date(2026, 7, 27, 10));
    expect(result.from).toBe(new Date(2026, 7, 27, 0).toISOString());
    expect(result.to).toBe(new Date(2026, 7, 28, 0).toISOString());
  });
});
