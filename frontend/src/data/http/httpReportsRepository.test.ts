import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  download: vi.fn(),
}));

vi.mock("../../shared/http", () => ({
  httpClient: {
    get: mocks.get,
    download: mocks.download,
  },
}));

import {
  httpReportsRepository,
} from "./httpReportsRepository";

describe("httpReportsRepository", () => {
  beforeEach(() => {
    mocks.get.mockReset();
    mocks.download.mockReset();
    mocks.get.mockResolvedValue({});
    mocks.download.mockResolvedValue({
      blob: new Blob(),
      fileName: "vitrine7-backup.backup",
    });
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

  it("baixa o backup completo do sistema", async () => {
    await httpReportsRepository.downloadSystemBackup();

    expect(mocks.download).toHaveBeenCalledWith(
      "/system/backup",
      {
        method: "POST",
        timeoutMs: 300_000,
      }
    );
  });

});
