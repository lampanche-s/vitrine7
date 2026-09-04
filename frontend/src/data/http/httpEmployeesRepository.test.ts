import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  httpClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

vi.mock("../../shared/http", () => ({ httpClient: mocks.httpClient }));

import { httpEmployeesRepository } from "./httpEmployeesRepository";

const response = {
  summary: { voucherValueCents: 79075, voucherCount: 2, averageVoucherCents: 39537, consumedUnits: 4 },
  byEmployee: [{ employeeId: 1, employeeName: "Cibele", voucherCount: 2, totalCents: 79075, consumption: 4 }],
  byEntry: [{ catalogEntryId: 3, name: "Produto", type: "ITEM", quantity: 4, totalCents: 79075, evolutionPercent: null }],
  byDay: [{ date: "2026-08-27", voucherCount: 2, totalCents: 79075 }],
};

describe("httpEmployeesRepository report", () => {
  beforeEach(() => Object.values(mocks.httpClient).forEach((mock) => mock.mockReset()));

  it("mapeia os campos reais do DTO do relatório", async () => {
    mocks.httpClient.get.mockResolvedValue(response);
    await expect(httpEmployeesRepository.report()).resolves.toEqual({
      summary: { voucherValue: 790.75, voucherCount: 2, averageVoucher: 395.37, consumedUnits: 4 },
      byEmployee: [{ employeeId: 1, employeeName: "Cibele", voucherCount: 2, totalCents: 79075, total: 790.75, consumption: 4 }],
      byEntry: [{ catalogEntryId: 3, name: "Produto", type: "ITEM", quantity: 4, totalCents: 79075, total: 790.75, evolutionPercent: null }],
      byDay: [{ date: "2026-08-27", voucherCount: 2, totalCents: 79075, total: 790.75 }],
    });
  });

  it("não envia employeeId para Todos e consulta novamente com outro período", async () => {
    mocks.httpClient.get.mockResolvedValue(response);
    await httpEmployeesRepository.report({ from: "2026-08-27T03:00:00.000Z", to: "2026-08-28T03:00:00.000Z" });
    await httpEmployeesRepository.report({ from: "2026-08-26T03:00:00.000Z", to: "2026-08-27T03:00:00.000Z" });

    expect(mocks.httpClient.get).toHaveBeenNthCalledWith(1, "/employees/vouchers/report?from=2026-08-27T03%3A00%3A00.000Z&to=2026-08-28T03%3A00%3A00.000Z");
    expect(mocks.httpClient.get).toHaveBeenNthCalledWith(2, "/employees/vouchers/report?from=2026-08-26T03%3A00%3A00.000Z&to=2026-08-27T03%3A00%3A00.000Z");
    expect(mocks.httpClient.get.mock.calls.flat().join(" ")).not.toMatch(/employeeId=(undefined|null)/);
  });
});
