import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";

const httpClient = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  patch: vi.fn(),
  delete: vi.fn(),
};

vi.mock("../../shared/http", () => ({
  httpClient,
}));

function tabResponse(
  status: "OPEN" | "PAYMENT_PENDING" = "OPEN"
) {
  return {
    id: 31,
    name: "Mesa 3",
    status,
    checkoutId:
      status === "PAYMENT_PENDING"
        ? "checkout-tab-1"
        : null,
    checkoutStatus:
      status === "PAYMENT_PENDING"
        ? "READY_FOR_PAYMENT"
        : null,
    totalCents: 1200,
    createdAt: "2026-07-17T19:00:00Z",
    lines: [
      {
        id: 1,
        catalogEntryId: 5,
        entryType: "ITEM",
        itemName: "Espeto",
        unitPriceCents: 1200,
        quantity: 1,
        lineTotalCents: 1200,
      },
    ],
  };
}

describe("httpBarRepository", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
  });

  it("carrega comandas e o catálogo canônico no snapshot", async () => {
    httpClient.get.mockImplementation((path: string) => {
      if (path.startsWith("/catalog?")) {
        return Promise.resolve({
          items: [
            {
              id: 9,
              name: "Lavagem expressa",
              type: "SERVICE",
              priceCents: 2500,
              stockEnabled: false,
              stockQuantity: null,
              minimumStockQuantity: null,
            },
          ],
          page: 0,
          totalPages: 1,
        });
      }

      return Promise.resolve({
        items: [],
        page: 0,
        totalPages: 0,
      });
    });

    const { httpBarRepository } =
      await import("./httpBarRepository");
    const snapshot =
      await httpBarRepository.getSnapshot();

    expect(snapshot).toEqual({
      commands: [],
      catalogEntries: [
        {
          id: 9,
          name: "Lavagem expressa",
          type: "SERVICE",
          price: 25,
          stockEnabled: false,
          stockQuantity: null,
          minimumStockQuantity: null,
        },
      ],
      historyEntries: [],
    });
  });

  it("solicita somente a impressão da pré-nota da comanda aberta", async () => {
    const { httpBarRepository } =
      await import("./httpBarRepository");

    await httpBarRepository.printPrePaymentNote(31);

    expect(httpClient.post).toHaveBeenCalledTimes(1);
    expect(httpClient.post).toHaveBeenCalledWith(
      "/bar/tabs/31/prepayment-print-jobs"
    );
    expect(httpClient.get).not.toHaveBeenCalled();
    expect(httpClient.put).not.toHaveBeenCalled();
    expect(httpClient.patch).not.toHaveBeenCalled();
    expect(httpClient.delete).not.toHaveBeenCalled();
  });

  it("envia as três solicitações de impressão operacional sem conteúdo da linha", async () => {
    const { httpBarRepository } =
      await import("./httpBarRepository");

    await httpBarRepository.printItems(31);
    await httpBarRepository.printServices(31);
    await httpBarRepository.printLine(31, 77);

    expect(httpClient.post).toHaveBeenNthCalledWith(
      1,
      "/bar/tabs/31/print/items"
    );
    expect(httpClient.post).toHaveBeenNthCalledWith(
      2,
      "/bar/tabs/31/print/services"
    );
    expect(httpClient.post).toHaveBeenNthCalledWith(
      3,
      "/bar/tabs/31/lines/77/print"
    );
  });

  it("carrega o catálogo de todas as páginas quando existem mais de 100 registros", async () => {
    const catalogEntries = Array.from({ length: 100 }, (_, index) => ({
      id: index + 1,
      name: `Produto ${index + 1}`,
      type: "ITEM" as const,
      priceCents: 1000,
      stockEnabled: false,
      stockQuantity: null,
      minimumStockQuantity: null,
      supplierId: null,
    }));
    httpClient.get.mockImplementation((path: string) => {
      if (path === "/catalog?page=0&size=100") {
        return Promise.resolve({ items: catalogEntries, page: 0, totalPages: 2 });
      }
      if (path === "/catalog?page=1&size=100") {
        return Promise.resolve({
          items: [{ ...catalogEntries[0], id: 101, name: "Produto 101" }],
          page: 1,
          totalPages: 2,
        });
      }
      return Promise.resolve({ items: [], page: 0, totalPages: 1 });
    });

    const { httpBarRepository } = await import("./httpBarRepository");
    const snapshot = await httpBarRepository.getSnapshot();

    expect(snapshot.catalogEntries).toHaveLength(101);
    expect(httpClient.get).toHaveBeenCalledWith("/catalog?page=1&size=100");
  });

  it("carrega comandas abertas de todas as páginas quando existem mais de 100 registros", async () => {
    const openTabs = Array.from({ length: 100 }, (_, index) => ({
      ...tabResponse(),
      id: index + 1,
      name: `Mesa ${index + 1}`,
    }));
    httpClient.get.mockImplementation((path: string) => {
      if (path === "/bar/tabs?page=0&size=100&status=OPEN") {
        return Promise.resolve({ items: openTabs, page: 0, totalPages: 2 });
      }
      if (path === "/bar/tabs?page=1&size=100&status=OPEN") {
        return Promise.resolve({
          items: [{ ...tabResponse(), id: 101, name: "Mesa 101" }],
          page: 1,
          totalPages: 2,
        });
      }
      return Promise.resolve({ items: [], page: 0, totalPages: 1 });
    });

    const { httpBarRepository } = await import("./httpBarRepository");
    const snapshot = await httpBarRepository.getSnapshot();

    expect(snapshot.commands).toHaveLength(101);
    expect(httpClient.get).toHaveBeenCalledWith(
      "/bar/tabs?page=1&size=100&status=OPEN"
    );
  });

  it("cria, edita e remove uma entrada canônica", async () => {
    httpClient.post.mockResolvedValueOnce({
      id: 9,
      name: "Espeto bovino",
      type: "ITEM",
      priceCents: 2490,
      stockEnabled: true,
      stockQuantity: 20,
      minimumStockQuantity: 5,
    });
    httpClient.put.mockResolvedValueOnce({
      id: 9,
      name: "Espeto bovino especial",
      type: "ITEM",
      priceCents: 2690,
      stockEnabled: true,
      stockQuantity: 18,
      minimumStockQuantity: 5,
    });

    const { httpBarRepository } =
      await import("./httpBarRepository");

    await httpBarRepository.createCatalogEntry({
      name: "Espeto bovino",
      type: "ITEM",
      price: 24.9,
      stockEnabled: true,
      stockQuantity: 20,
      minimumStockQuantity: 5,
    });
    httpClient.post.mockResolvedValueOnce(undefined);
    const verified = await httpBarRepository.verifyCatalogPassword("segredo");
    const updated =
      await httpBarRepository.updateCatalogEntry(
        9,
        {
          name: "Espeto bovino especial",
          type: "ITEM",
          price: 26.9,
          stockEnabled: true,
          stockQuantity: 18,
          minimumStockQuantity: 5,
        },
        "segredo"
      );
    await httpBarRepository.removeCatalogEntry(9, "segredo");

    expect(httpClient.post).toHaveBeenCalledWith(
      "/catalog",
      {
        name: "Espeto bovino",
        type: "ITEM",
        priceCents: 2490,
        stockEnabled: true,
        stockQuantity: 20,
        minimumStockQuantity: 5,
      }
    );
    expect(updated.price).toBe(26.9);
    expect(verified).toBe(true);
    expect(httpClient.post).toHaveBeenCalledWith(
      "/catalog/access/verify",
      { password: "segredo" }
    );
    expect(httpClient.put).toHaveBeenCalledWith(
      "/catalog/9",
      expect.any(Object),
      { headers: { "X-Catalog-Password": "segredo" } }
    );
    expect(httpClient.delete).toHaveBeenCalledWith(
      "/catalog/9",
      { headers: { "X-Catalog-Password": "segredo" } }
    );
  });

  it("envia estoque nulo quando o controle está desligado", async () => {
    httpClient.post.mockResolvedValueOnce({
      id: 10,
      name: "Produto livre",
      type: "ITEM",
      priceCents: 1500,
      stockEnabled: false,
      stockQuantity: null,
      minimumStockQuantity: null,
    });

    const { httpBarRepository } =
      await import("./httpBarRepository");

    const created =
      await httpBarRepository.createCatalogEntry({
        name: "Produto livre",
        type: "ITEM",
        price: 15,
        stockEnabled: false,
        stockQuantity: null,
        minimumStockQuantity: null,
      });

    expect(httpClient.post).toHaveBeenCalledWith(
      "/catalog",
      {
        name: "Produto livre",
        type: "ITEM",
        priceCents: 1500,
        stockEnabled: false,
        stockQuantity: null,
        minimumStockQuantity: null,
      }
    );
    expect(created.stockEnabled).toBe(false);
    expect(created.stockQuantity).toBeNull();
  });

  it("adiciona entrada do catálogo à comanda", async () => {
    httpClient.put.mockResolvedValueOnce(tabResponse());
    const { httpBarRepository } =
      await import("./httpBarRepository");

    const command =
      await httpBarRepository.addCommandItem(
        31,
        {
          catalogItemId: 5,
          quantity: 2,
        }
      );

    expect(httpClient.put).toHaveBeenCalledWith(
      "/bar/tabs/31/catalog/5",
      { quantity: 2 }
    );
    expect(command.items[0]).toMatchObject({
      key: "5",
      catalogItemId: 5,
    });
  });

  it("encaminha veículo e placa na inclusão de serviço", async () => {
    httpClient.put.mockResolvedValueOnce(tabResponse());
    const { httpBarRepository } = await import("./httpBarRepository");

    await httpBarRepository.addCommandItem(31, {
      catalogItemId: 5,
      vehicleName: "Onix prata",
      vehiclePlate: "ABC1D23",
    });

    expect(httpClient.put).toHaveBeenCalledWith("/bar/tabs/31/catalog/5", {
      quantity: 1,
      vehicleName: "Onix prata",
      vehiclePlate: "ABC1D23",
    });
  });

  it("altera quantidade e preço personalizado", async () => {
    httpClient.put.mockResolvedValueOnce(tabResponse());
    const { httpBarRepository } =
      await import("./httpBarRepository");

    await httpBarRepository.updateCommandItemQuantity(
      31,
      "5",
      3,
      13.5
    );

    expect(httpClient.put).toHaveBeenCalledWith(
      "/bar/tabs/31/catalog/5",
      {
        quantity: 3,
        unitPriceCents: 1350,
      }
    );
  });

  it("finaliza o checkout e devolve histórico da comanda", async () => {
    httpClient.get
      .mockResolvedValueOnce(
        tabResponse("PAYMENT_PENDING")
      )
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce({
        ...tabResponse("PAYMENT_PENDING"),
        status: "CLOSED",
        checkoutStatus: "FINALIZED",
      })
      .mockResolvedValueOnce({
        items: [
          {
            id: 5,
            name: "Espeto",
            type: "ITEM",
            priceCents: 1200,
            stockEnabled: true,
            stockQuantity: 9,
            minimumStockQuantity: 3,
          },
        ],
        page: 0,
        totalPages: 1,
      });
    httpClient.post.mockResolvedValueOnce({
      checkout: {
        id: "checkout-tab-1",
        status: "FINALIZED",
        totalCents: 1200,
        finalizedAt: "2026-07-17T19:40:00Z",
      },
      payment: {
        status: "APPROVED",
      },
    });

    const { httpBarRepository } =
      await import("./httpBarRepository");
    const result =
      await httpBarRepository.closeCommand({
        commandId: 31,
        payment: "Pix",
        document: "Recibo geral",
        time: "19:40",
      });

    expect(httpClient.post).toHaveBeenCalledWith(
      "/checkouts/checkout-tab-1/payments/pix",
      { amountCents: 1200 },
      expect.objectContaining({
        headers: expect.objectContaining({
          "Idempotency-Key": expect.any(String),
        }),
      })
    );
    expect(result.historyEntry).toMatchObject({
      amount: 12,
      method: "Pix",
    });
    expect(result.catalogEntries?.[0]).toMatchObject({
      id: 5,
      stockEnabled: true,
      stockQuantity: 9,
      minimumStockQuantity: 3,
    });
  });

  it("lista somente histórico concluído", async () => {
    httpClient.get.mockResolvedValueOnce({
      items: [
        {
          operationId: 21,
          checkoutId: "checkout-21",
          displayName: "Mesa 3",
          operationalStatus: "CLOSED",
          checkoutStatus: "FINALIZED",
          totalCents: 2500,
          paymentMethod: "PIX",
          paymentStatus: "APPROVED",
          paymentId: "payment-21",
          paymentReversedAt: null,
          paymentReversalReason: null,
          cashReceivedCents: null,
          cashChangeCents: null,
          lineCount: 1,
          totalUnits: 2,
          finishedAt: "2026-07-17T19:30:00Z",
        },
        {
          operationId: 22,
          checkoutId: "checkout-22",
          displayName: "Mesa 4",
          operationalStatus: "PAYMENT_PENDING",
          checkoutStatus: "PAYMENT_FAILED",
          totalCents: 1200,
          paymentMethod: "DEBIT",
          paymentStatus: "DECLINED",
          paymentId: null,
          paymentReversedAt: null,
          paymentReversalReason: null,
          cashReceivedCents: null,
          cashChangeCents: null,
          lineCount: 1,
          totalUnits: 1,
          finishedAt: "2026-07-17T19:35:00Z",
        },
      ],
      page: 0,
      totalPages: 1,
      totalElements: 2,
    });

    const { httpBarRepository } =
      await import("./httpBarRepository");
    const result =
      await httpBarRepository.listHistory({
        page: 0,
        size: 20,
      });

    expect(result.entries).toHaveLength(1);
    expect(result.entries[0]).toMatchObject({
      id: 21,
      origin: "Comanda Mesa 3",
      amount: 25,
    });
  });
});
