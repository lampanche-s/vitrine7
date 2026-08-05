import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
}));

vi.mock("../../shared/http", () => ({
  httpClient: {
    get: mocks.get,
  },
}));

import {
  httpReportsRepository,
} from "./httpReportsRepository";

describe("httpReportsRepository", () => {
  beforeEach(() => {
    mocks.get.mockReset();
    mocks.get.mockResolvedValue({});
  });

  it("carrega o resumo consolidado", async () => {
    await httpReportsRepository.summary({ scope: "ALL" });

    expect(mocks.get).toHaveBeenCalledWith(
      "/reports/sales/summary?scope=ALL"
    );
  });

  it("exporta item e serviço usando datas locais sem converter para UTC", async () => {
    await httpReportsRepository.export({
      from: "2026-08-01",
      to: "2026-08-05",
      scope: "SERVICE",
    });

    expect(mocks.get).toHaveBeenCalledWith(
      "/reports/sales/export?from=2026-08-01&to=2026-08-05&scope=SERVICE",
      {
        timeoutMs: 60_000,
      }
    );
  });
});
