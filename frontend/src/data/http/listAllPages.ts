export type HttpPageResponse<T> = {
  items: T[];
  page: number;
  totalPages: number;
};

export async function listAllPages<T>(
  loadPage: (
    page: number
  ) => Promise<HttpPageResponse<T>>
): Promise<T[]> {
  const items: T[] = [];
  let currentPage = 0;

  while (true) {
    const response =
      await loadPage(currentPage);

    items.push(...response.items);

    const responsePage =
      Number.isInteger(response.page)
        ? response.page
        : currentPage;

    const totalPages =
      Number.isInteger(response.totalPages)
        ? response.totalPages
        : 1;

    const nextPage =
      responsePage + 1;

    if (nextPage >= totalPages) {
      return items;
    }

    currentPage = nextPage;
  }
}
