import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

vi.mock("../../shared/http", () => ({ httpClient: mocks.httpClient }));

import { httpAdminRepository } from "./httpAdminRepository";

describe("httpAdminRepository", () => {
  beforeEach(() => {
    Object.values(mocks.httpClient).forEach((mock) => mock.mockReset());
  });

  it("carrega usuários de todas as páginas quando existem mais de 100 registros", async () => {
    const firstPage = Array.from({ length: 100 }, (_, index) => ({
      id: index + 1,
      name: `Usuário ${index + 1}`,
      username: `usuario${index + 1}`,
      role: "OPERADOR",
      status: "ATIVO",
    }));
    mocks.httpClient.get
      .mockResolvedValueOnce({ items: firstPage, page: 0, totalPages: 2 })
      .mockResolvedValueOnce({
        items: [{
          id: 101,
          name: "Usuário 101",
          username: "usuario101",
          role: "OPERADOR",
          status: "ATIVO",
        }],
        page: 1,
        totalPages: 2,
      });

    const snapshot = await httpAdminRepository.getSnapshot();

    expect(snapshot.users).toHaveLength(101);
    expect(mocks.httpClient.get).toHaveBeenNthCalledWith(1, "/users?page=0&size=100&sort=name&direction=ASC");
    expect(mocks.httpClient.get).toHaveBeenNthCalledWith(2, "/users?page=1&size=100&sort=name&direction=ASC");
  });
});
