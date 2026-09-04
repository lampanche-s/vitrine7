import { describe, expect, it } from "vitest";

import {
  DEFAULT_REPORT_PERIOD,
  exportReportPeriodOptions,
  isProtectedReportPeriod,
  reportPeriodOptions,
  resolveReportDateRange,
  type ReportPeriodPreset,
} from "./report-period";

const now = new Date(2026, 7, 25, 14, 30);

describe("report periods", () => {
  it("starts on today and only exposes today and yesterday publicly", () => {
    expect(DEFAULT_REPORT_PERIOD).toBe("today");
    expect(isProtectedReportPeriod("today")).toBe(false);
    expect(isProtectedReportPeriod("previousDay")).toBe(false);

    for (const period of [
      "currentWeek",
      "previousWeek",
      "currentMonth",
      "previousMonth",
      "custom",
    ] as const) {
      expect(isProtectedReportPeriod(period)).toBe(true);
    }
  });
  it.each([
    ["today", "2026-08-25", "2026-08-25"],
    ["previousDay", "2026-08-24", "2026-08-24"],
    ["currentWeek", "2026-08-24", "2026-08-25"],
    ["previousWeek", "2026-08-17", "2026-08-23"],
    ["currentMonth", "2026-08-01", "2026-08-25"],
    ["previousMonth", "2026-07-01", "2026-07-31"],
    ["custom", "2026-08-03", "2026-08-19"],
  ] as const)(
    "resolve %s using local calendar dates",
    (preset, expectedFrom, expectedTo) => {
      const range = resolveReportDateRange(
        preset as ReportPeriodPreset,
        "2026-08-03",
        "2026-08-19",
        now
      );

      expect(range.startDate).toBe(expectedFrom);
      expect(range.endDate).toBe(expectedTo);
    }
  );

  it("exposes all requested consultation options", () => {
    expect(reportPeriodOptions.map((option) => option.label)).toEqual([
      "Hoje",
      "Ontem",
      "Semana atual",
      "Semana passada",
      "Mês atual",
      "Mês passado",
      "Período personalizado",
    ]);
  });

  it("uses the same supported periods for consultation and export", () => {
    expect(exportReportPeriodOptions).toEqual(reportPeriodOptions);
  });

  it("rejects an inverted custom period", () => {
    expect(() =>
      resolveReportDateRange(
        "custom",
        "2026-08-20",
        "2026-08-10",
        now
      )
    ).toThrow("A data inicial não pode ser posterior à data final.");
  });
});
