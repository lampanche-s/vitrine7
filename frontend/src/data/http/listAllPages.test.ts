import {
  describe,
  expect,
  it,
  vi,
} from "vitest";

import {
  listAllPages,
} from "./listAllPages";

describe("listAllPages", () => {
  it("carrega todas as páginas na ordem", async () => {
    const firstPage = Array.from(
      { length: 100 },
      (_, index) => index + 1
    );
    const loadPage = vi.fn()
      .mockResolvedValueOnce({
        items: firstPage,
        page: 0,
        totalPages: 2,
      })
      .mockResolvedValueOnce({
        items: [101],
        page: 1,
        totalPages: 2,
      });

    const result = await listAllPages(loadPage);

    expect(result).toHaveLength(101);
    expect(result[0]).toBe(1);
    expect(result[100]).toBe(101);

    expect(loadPage).toHaveBeenNthCalledWith(
      1,
      0
    );

    expect(loadPage).toHaveBeenNthCalledWith(
      2,
      1
    );
  });

  it("trata resposta sem metadados como página única", async () => {
    const loadPage = vi.fn()
      .mockResolvedValue({
        items: [1],
      });

    await expect(
      listAllPages(loadPage)
    ).resolves.toEqual([1]);

    expect(loadPage).toHaveBeenCalledTimes(1);
  });
});
