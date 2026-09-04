import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  httpClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

vi.mock("../../shared/http", () => ({ httpClient: mocks.httpClient }));

import { httpSuppliersRepository } from "./httpSuppliersRepository";

const supplier = { id: 7, name: "Casa do Café", cnpj: "12345678000190", phone: "71999990000", cep: "40000000" };

describe("httpSuppliersRepository", () => {
  beforeEach(() => Object.values(mocks.httpClient).forEach((mock) => mock.mockReset()));

  it("lista e consulta o detalhe com os itens vinculados", async () => {
    mocks.httpClient.get.mockResolvedValueOnce({ items: [supplier], page: 0, totalPages: 1 });
    mocks.httpClient.get.mockResolvedValueOnce({ ...supplier, catalogEntries: [{ id: 4, name: "Café", priceCents: 850, stockEnabled: true, stockQuantity: 10, minimumStockQuantity: 2 }] });

    await expect(httpSuppliersRepository.list()).resolves.toEqual([supplier]);
    await expect(httpSuppliersRepository.detail(7)).resolves.toEqual({ ...supplier, catalogEntries: [{ id: 4, name: "Café", price: 8.5, stockEnabled: true, stockQuantity: 10, minimumStockQuantity: 2 }] });
    expect(mocks.httpClient.get).toHaveBeenCalledWith("/suppliers?page=0&size=100&sort=name&direction=ASC");
    expect(mocks.httpClient.get).toHaveBeenCalledWith("/suppliers/7");
  });

  it("carrega fornecedores de todas as páginas quando existem mais de 100 registros", async () => {
    const firstPage = Array.from({ length: 100 }, (_, index) => ({
      ...supplier,
      id: index + 1,
      name: `Fornecedor ${index + 1}`,
    }));
    mocks.httpClient.get
      .mockResolvedValueOnce({ items: firstPage, page: 0, totalPages: 2 })
      .mockResolvedValueOnce({
        items: [{ ...supplier, id: 101, name: "Fornecedor 101" }],
        page: 1,
        totalPages: 2,
      });

    const result = await httpSuppliersRepository.list();

    expect(result).toHaveLength(101);
    expect(mocks.httpClient.get).toHaveBeenNthCalledWith(1, "/suppliers?page=0&size=100&sort=name&direction=ASC");
    expect(mocks.httpClient.get).toHaveBeenNthCalledWith(2, "/suppliers?page=1&size=100&sort=name&direction=ASC");
  });

  it("usa os endpoints de criação, edição e exclusão", async () => {
    mocks.httpClient.post.mockResolvedValue(supplier);
    mocks.httpClient.put.mockResolvedValue(supplier);
    mocks.httpClient.delete.mockResolvedValue(undefined);
    const input = { name: "Casa do Café", cnpj: "12.345.678/0001-90", phone: null, cep: null };

    await httpSuppliersRepository.create(input);
    await httpSuppliersRepository.update(7, input);
    await httpSuppliersRepository.remove(7);

    expect(mocks.httpClient.post).toHaveBeenCalledWith("/suppliers", input);
    expect(mocks.httpClient.put).toHaveBeenCalledWith("/suppliers/7", input);
    expect(mocks.httpClient.delete).toHaveBeenCalledWith("/suppliers/7");
  });
});
