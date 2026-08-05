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
    const loadPage = vi.fn()
      .mockResolvedValueOnce({
        items: [1, 2],
        page: 0,
        totalPages: 2,
      })
      .mockResolvedValueOnce({
        items: [3],
        page: 1,
        totalPages: 2,
      });

    await expect(
      listAllPages(loadPage)
    ).resolves.toEqual([1, 2, 3]);

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
