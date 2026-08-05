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
        },
      ],
      historyEntries: [],
    });
  });

  it("cria, edita e remove uma entrada canônica", async () => {
    httpClient.post.mockResolvedValueOnce({
      id: 9,
      name: "Espeto bovino",
      type: "ITEM",
      priceCents: 2490,
    });
    httpClient.put.mockResolvedValueOnce({
      id: 9,
      name: "Espeto bovino especial",
      type: "ITEM",
      priceCents: 2690,
    });

    const { httpBarRepository } =
      await import("./httpBarRepository");

    await httpBarRepository.createCatalogEntry({
      name: "Espeto bovino",
      type: "ITEM",
      price: 24.9,
    });
    const updated =
      await httpBarRepository.updateCatalogEntry(
        9,
        {
          name: "Espeto bovino especial",
          type: "ITEM",
          price: 26.9,
        }
      );
    await httpBarRepository.removeCatalogEntry(9);

    expect(httpClient.post).toHaveBeenCalledWith(
      "/catalog",
      {
        name: "Espeto bovino",
        type: "ITEM",
        priceCents: 2490,
      }
    );
    expect(updated.price).toBe(26.9);
    expect(httpClient.delete).toHaveBeenCalledWith(
      "/catalog/9"
    );
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
    httpClient.get.mockResolvedValueOnce(
      tabResponse("PAYMENT_PENDING")
    );
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
      undefined,
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
