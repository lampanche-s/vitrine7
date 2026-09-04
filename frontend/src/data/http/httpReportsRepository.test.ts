import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  download: vi.fn(),
}));

vi.mock("../../shared/http", () => ({
  httpClient: {
    get: mocks.get,
    post: mocks.post,
    download: mocks.download,
  },
}));

import {
  httpReportsRepository,
} from "./httpReportsRepository";

describe("httpReportsRepository", () => {
  beforeEach(() => {
    mocks.get.mockReset();
    mocks.post.mockReset();
    mocks.download.mockReset();
    mocks.get.mockResolvedValue({});
    mocks.post.mockResolvedValue(undefined);
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

  it.each(["ALL", "ITEM", "SERVICE"] as const)(
    "consulta o escopo %s com o período selecionado",
    async (scope) => {
      await httpReportsRepository.summary({
        from: "2026-08-01",
        to: "2026-08-25",
        scope,
      });

      expect(mocks.get).toHaveBeenCalledWith(
        `/reports/sales/summary?from=2026-08-01&to=2026-08-25&scope=${scope}`
      );
    }
  );

  it("consulta todo o período sem enviar datas", async () => {
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
        headers: undefined,
      }
    );
  });

  it("valida a senha protegida somente no corpo da requisição", async () => {
    await httpReportsRepository.verifyProtectedReportPassword("segredo");

    expect(mocks.post).toHaveBeenCalledWith(
      "/reports/access/verify",
      { password: "segredo" }
    );
  });

  it("envia a senha protegida em header no resumo e no export", async () => {
    await httpReportsRepository.summary({
      from: "2026-08-01",
      to: "2026-08-31",
      scope: "ALL",
      reportPassword: "segredo",
    });
    await httpReportsRepository.export({
      from: "2026-08-01",
      to: "2026-08-31",
      scope: "ITEM",
      reportPassword: "segredo",
    });

    expect(mocks.get).toHaveBeenNthCalledWith(
      1,
      "/reports/sales/summary?from=2026-08-01&to=2026-08-31&scope=ALL",
      { headers: { "X-Report-Password": "segredo" } }
    );
    expect(mocks.get).toHaveBeenNthCalledWith(
      2,
      "/reports/sales/export?from=2026-08-01&to=2026-08-31&scope=ITEM",
      {
        timeoutMs: 60_000,
        headers: { "X-Report-Password": "segredo" },
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
