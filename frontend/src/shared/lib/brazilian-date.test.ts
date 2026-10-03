import { describe, expect, it } from "vitest";
import {
  formatIsoDateForBrazil,
  maskBrazilianDate,
  parseBrazilianDate,
} from "./brazilian-date";

describe("Brazilian date input", () => {
  it("formats sequential digits and pasted dates for entry", () => {
    expect(maskBrazilianDate("02102026")).toBe("02/10/2026");
    expect(maskBrazilianDate("02/10/2026")).toBe("02/10/2026");
    expect(maskBrazilianDate("0210")).toBe("02/10");
  });

  it("converts valid Brazilian dates to API dates", () => {
    expect(parseBrazilianDate("02/10/2026")).toBe("2026-10-02");
    expect(parseBrazilianDate("29/02/2024")).toBe("2024-02-29");
    expect(formatIsoDateForBrazil("2026-10-02")).toBe("02/10/2026");
  });

  it("rejects impossible and incomplete calendar dates", () => {
    for (const value of ["31/02/2026", "29/02/2025", "00/10/2026", "02/13/2026", "02/10/26"]) {
      expect(parseBrazilianDate(value)).toBeNull();
    }
    expect(formatIsoDateForBrazil("2026-02-31")).toBe("");
  });
});
