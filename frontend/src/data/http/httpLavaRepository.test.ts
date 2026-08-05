import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";

import {
  HttpError,
} from "../../shared/http/http-error";

import {
  formatBrlCurrency,
} from "../../shared/lib/currency";

const httpClient = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  patch: vi.fn(),
  delete: vi.fn(),
};

vi.mock("../../shared/http", () => ({
  HttpError,
  httpClient,
}));

const backendClient = {
  id: 7,
  name: "Marcos Almeida",
  phone: "71988241092",
  vehicleName: "Honda Civic",
  plate: "QXZ8A21",
  visitsCount: 12,
  lastServiceLabel: "Lavagem premium",
  active: true,
};

const backendService = {
  id: 11,
  name: "Lavagem completa",
  category: "Lavagem",
  smallVehiclePriceCents: 3500,
  mediumVehiclePriceCents: 4500,
  durationMinutes: 40,
  durationLabel: "40 min",
  active: true,
};

const backendWorkOrder = {
  id: 21,
  registeredClientId: 7,
  customerNameSnapshot: "Marcos Almeida",
  customerPhoneDigitsSnapshot: "71988241092",
  vehicleNameSnapshot: "Honda Civic",
  vehiclePlateSnapshot: "QXZ8A21",
  vehicleSize: "MEDIUM",
  status: "OPEN",
  checkoutSessionId: null,
  checkoutStatus: null,
  subtotalCents: 4500,
  discountCents: 0,
  totalCents: 4500,
  documentType: null,
  prepared: false,
  paidAt: null,
  cancelledAt: null,
  services: [
    {
      id: 31,
      serviceId: 11,
      serviceNameSnapshot: "Lavagem completa",
      priceCents: 4500,
      createdAt: "2026-07-18T10:00:00Z",
      updatedAt: "2026-07-18T10:00:00Z",
    },
  ],
  createdAt: "2026-07-18T10:00:00Z",
  updatedAt: "2026-07-18T10:05:00Z",
};

describe("httpLavaRepository clients", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.resetAllMocks();
  });

  it("lista clientes reais paginados e mapeia o DTO do backend", async () => {
    httpClient.get
      .mockResolvedValueOnce({
        items: [],
        page: 0,
        totalPages: 0,
      })
      .mockResolvedValueOnce({
        items: [
          backendClient,
        ],
        page: 0,
        totalPages: 2,
      })
      .mockResolvedValueOnce({
        items: [
          {
            ...backendClient,
            id: 8,
            name: "Camila Rocha",
            phone: null,
            visitsCount: null,
            lastServiceLabel: null,
            active: false,
          },
          {
            ...backendClient,
            id: 7,
          },
        ],
        page: 1,
        totalPages: 2,
      })
      .mockResolvedValueOnce({
        items: [],
        page: 0,
        totalPages: 0,
      })
      .mockResolvedValueOnce({
        items: [],
        page: 0,
        totalPages: 0,
      })
      .mockResolvedValueOnce({
        items: [],
        page: 0,
        totalPages: 0,
      })
      .mockResolvedValueOnce([]);

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    const snapshot =
      await httpLavaRepository.getSnapshot();

    expect(httpClient.get).toHaveBeenCalledWith(
      "/lava/services?page=0&size=100&sort=name&direction=ASC"
    );
    expect(httpClient.get).toHaveBeenCalledWith(
      "/lava/clients?page=0&size=100&sort=name&direction=ASC"
    );
    expect(httpClient.get).toHaveBeenCalledWith(
      "/lava/clients?page=1&size=100&sort=name&direction=ASC"
    );
    expect(httpClient.get).toHaveBeenCalledWith(
      "/lava/work-orders?page=0&size=100&status=OPEN"
    );
    expect(httpClient.get).toHaveBeenCalledWith(
      "/lava/work-orders?page=0&size=100&status=PAYMENT_PENDING"
    );
    expect(httpClient.get).toHaveBeenCalledWith(
      "/lava/work-orders?page=0&size=100&status=PAID"
    );
    expect(snapshot.clients).toHaveLength(2);
    expect(snapshot.clients[0]).toMatchObject({
      id: 7,
      name: "Marcos Almeida",
      phone: "(71) 98824-1092",
      vehicle: "Honda Civic",
      plate: "QXZ8A21",
      visits: 12,
      lastService: "Lavagem premium",
      active: true,
    });
    expect(snapshot.clients[1]).toMatchObject({
      id: 8,
      phone: "",
      visits: 0,
      lastService: "Ainda sem serviço",
      active: false,
    });
    expect(snapshot.workOrders).toEqual([]);
    expect(snapshot.financeEntries).toEqual([]);
  });

  it("cria, edita, altera status e exclui cliente usando os endpoints reais", async () => {
    httpClient.post.mockResolvedValueOnce(backendClient);
    httpClient.put.mockResolvedValueOnce({
      ...backendClient,
      name: "Marcos Silva",
    });
    httpClient.patch.mockResolvedValueOnce({
      ...backendClient,
      active: false,
    });
    httpClient.delete.mockResolvedValueOnce(undefined);

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.createClient({
        name: "Marcos Almeida",
        phone: "(71) 98824-1092",
        vehicle: "Honda Civic",
        plate: "QXZ-8A21",
      })
    ).resolves.toMatchObject({
      id: 7,
    });

    await expect(
      httpLavaRepository.updateClient(7, {
        name: "Marcos Silva",
        phone: "(71) 98824-1092",
        vehicle: "Honda Civic",
        plate: "QXZ-8A21",
      })
    ).resolves.toMatchObject({
      name: "Marcos Silva",
    });

    await expect(
      httpLavaRepository.setClientActive(7, false)
    ).resolves.toMatchObject({
      active: false,
    });

    await expect(
      httpLavaRepository.removeClient(7)
    ).resolves.toBeUndefined();

    expect(httpClient.post).toHaveBeenCalledWith(
      "/lava/clients",
      {
        name: "Marcos Almeida",
        phone: "(71) 98824-1092",
        vehicleName: "Honda Civic",
        plate: "QXZ-8A21",
      }
    );
    expect(httpClient.put).toHaveBeenCalledWith(
      "/lava/clients/7",
      {
        name: "Marcos Silva",
        phone: "(71) 98824-1092",
        vehicleName: "Honda Civic",
        plate: "QXZ-8A21",
      }
    );
    expect(httpClient.patch).toHaveBeenCalledWith(
      "/lava/clients/7/active",
      {
        active: false,
      }
    );
    expect(httpClient.delete).toHaveBeenCalledWith(
      "/lava/clients/7"
    );
  });

  it("expõe mensagens específicas de validação retornadas pelo backend", async () => {
    httpClient.post.mockRejectedValueOnce(
      new HttpError(
        "Existem campos inválidos na requisição.",
        400,
        {
          message:
            "Existem campos inválidos na requisição.",
          fieldErrors: [
            {
              field: "name",
              message: "Informe o nome do cliente.",
            },
            {
              field: "plate",
              message: "A placa informada é muito longa.",
            },
          ],
        }
      )
    );

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.createClient({
        name: "",
        phone: "",
        vehicle: "Honda Civic",
        plate: "QXZ-8A211",
      })
    ).rejects.toThrow(
      "Informe o nome do cliente. A placa informada é muito longa."
    );
  });

  it("não acessa localStorage do mock ao carregar clientes HTTP", async () => {
    const getItem = vi.fn();
    vi.stubGlobal("window", {
      localStorage: {
        getItem,
      },
    });

    httpClient.get.mockResolvedValueOnce({
      items: [],
      page: 0,
      totalPages: 0,
    });

    httpClient.get.mockResolvedValueOnce({
      items: [],
      page: 0,
      totalPages: 0,
    });

    httpClient.get.mockResolvedValueOnce({
      items: [],
      page: 0,
      totalPages: 0,
    });

    httpClient.get.mockResolvedValueOnce({
      items: [],
      page: 0,
      totalPages: 0,
    });

    httpClient.get.mockResolvedValueOnce({
      items: [],
      page: 0,
      totalPages: 0,
    });

    httpClient.get.mockResolvedValueOnce([]);

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await httpLavaRepository.getSnapshot();

    expect(getItem).not.toHaveBeenCalled();

    vi.unstubAllGlobals();
  });
});

describe("httpLavaRepository services", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.resetAllMocks();
  });

  it("lista serviços reais paginados e mapeia o DTO do backend", async () => {
    httpClient.get
      .mockResolvedValueOnce({
        items: [
          backendService,
        ],
        page: 0,
        totalPages: 2,
      })
      .mockResolvedValueOnce({
        items: [
          {
            ...backendService,
            id: 12,
            name: "Cristalizacao",
            smallVehiclePriceCents: 12050,
            mediumVehiclePriceCents: 15075,
            durationMinutes: 90,
            durationLabel: "1 h 30 min",
            active: false,
          },
          {
            ...backendService,
            id: 11,
          },
        ],
        page: 1,
        totalPages: 2,
      })
      .mockResolvedValueOnce({
        items: [],
        page: 0,
        totalPages: 0,
      })
      .mockResolvedValueOnce({
        items: [],
        page: 0,
        totalPages: 0,
      })
      .mockResolvedValueOnce({
        items: [],
        page: 0,
        totalPages: 0,
      })
      .mockResolvedValueOnce({
        items: [],
        page: 0,
        totalPages: 0,
      })
      .mockResolvedValueOnce([]);

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    const snapshot =
      await httpLavaRepository.getSnapshot();

    expect(httpClient.get).toHaveBeenCalledWith(
      "/lava/services?page=0&size=100&sort=name&direction=ASC"
    );
    expect(httpClient.get).toHaveBeenCalledWith(
      "/lava/services?page=1&size=100&sort=name&direction=ASC"
    );
    expect(snapshot.services).toHaveLength(2);
    expect(snapshot.services[0]).toMatchObject({
      id: 11,
      name: "Lavagem completa",
      category: "Lavagem",
      price: formatBrlCurrency(35),
      smallCarPrice: formatBrlCurrency(35),
      mediumCarPrice: formatBrlCurrency(45),
      duration: "40 min",
      active: true,
    });
    expect(snapshot.services[1]).toMatchObject({
      id: 12,
      smallCarPrice: formatBrlCurrency(120.5),
      mediumCarPrice: formatBrlCurrency(150.75),
      duration: "1 h 30 min",
      active: false,
    });
    expect(snapshot.clients).toEqual([]);
    expect(snapshot.workOrders).toEqual([]);
    expect(snapshot.financeEntries).toEqual([]);
  });

  it("cria, edita, altera status e exclui serviço usando os endpoints reais", async () => {
    httpClient.post.mockResolvedValueOnce(backendService);
    httpClient.put.mockResolvedValueOnce({
      ...backendService,
      name: "Lavagem premium",
      durationMinutes: 75,
      durationLabel: "1 h 15 min",
    });
    httpClient.patch.mockResolvedValueOnce({
      ...backendService,
      active: false,
    });
    httpClient.delete.mockResolvedValueOnce(undefined);

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.createService({
        name: "Lavagem completa",
        category: "Lavagem",
        smallCarPrice: "R$ 35,00",
        mediumCarPrice: "R$ 45,00",
        duration: "40 min",
      })
    ).resolves.toMatchObject({
      id: 11,
      price: formatBrlCurrency(35),
      active: true,
    });

    await expect(
      httpLavaRepository.updateService(11, {
        name: "Lavagem premium",
        category: "Lavagem",
        smallCarPrice: "R$ 35,00",
        mediumCarPrice: "R$ 45,00",
        duration: "1 h 15 min",
      })
    ).resolves.toMatchObject({
      name: "Lavagem premium",
      duration: "1 h 15 min",
    });

    await expect(
      httpLavaRepository.setServiceActive(11, false)
    ).resolves.toMatchObject({
      active: false,
    });

    await expect(
      httpLavaRepository.removeService(11)
    ).resolves.toBeUndefined();

    expect(httpClient.post).toHaveBeenCalledWith(
      "/lava/services",
      {
        name: "Lavagem completa",
        category: "Lavagem",
        smallVehiclePriceCents: 3500,
        mediumVehiclePriceCents: 4500,
        durationMinutes: 40,
      }
    );
    expect(httpClient.put).toHaveBeenCalledWith(
      "/lava/services/11",
      {
        name: "Lavagem premium",
        category: "Lavagem",
        smallVehiclePriceCents: 3500,
        mediumVehiclePriceCents: 4500,
        durationMinutes: 75,
      }
    );
    expect(httpClient.patch).toHaveBeenCalledWith(
      "/lava/services/11/active",
      {
        active: false,
      }
    );
    expect(httpClient.delete).toHaveBeenCalledWith(
      "/lava/services/11"
    );
  });

  it("expõe mensagens específicas de validação retornadas pelo backend para serviços", async () => {
    httpClient.post.mockRejectedValueOnce(
      new HttpError(
        "Existem campos inválidos na requisição.",
        400,
        {
          message:
            "Existem campos inválidos na requisição.",
          fieldErrors: [
            {
              field: "name",
              message: "Informe o nome do serviço.",
            },
            {
              field: "durationMinutes",
              message: "A duração deve ser de pelo menos 1 minuto.",
            },
          ],
        }
      )
    );

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.createService({
        name: " ",
        category: "Lavagem",
        smallCarPrice: "R$ 35,00",
        mediumCarPrice: "R$ 45,00",
        duration: "40 min",
      })
    ).rejects.toThrow(
      "Informe o nome do serviço. A duração deve ser de pelo menos 1 minuto."
    );
  });

  it("valida duração antes de chamar o backend", async () => {
    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.createService({
        name: "Lavagem completa",
        category: "Lavagem",
        smallCarPrice: "R$ 35,00",
        mediumCarPrice: "R$ 45,00",
        duration: "tempo curto",
      })
    ).rejects.toThrow(
      "Informe uma duração válida para o serviço."
    );

    expect(httpClient.post).not.toHaveBeenCalled();
  });
});

describe("httpLavaRepository work orders", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.resetAllMocks();
  });

  it("lista OS abertas reais e mapeia serviço, porte e preço persistidos", async () => {
    httpClient.get
      .mockResolvedValueOnce({
        items: [],
        page: 0,
        totalPages: 0,
      })
      .mockResolvedValueOnce({
        items: [],
        page: 0,
        totalPages: 0,
      })
      .mockResolvedValueOnce({
        items: [
          backendWorkOrder,
        ],
        page: 0,
        totalPages: 1,
      })
      .mockResolvedValueOnce({
        items: [],
        page: 0,
        totalPages: 0,
      })
      .mockResolvedValueOnce({
        items: [],
        page: 0,
        totalPages: 0,
      })
      .mockResolvedValueOnce([]);

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    const snapshot =
      await httpLavaRepository.getSnapshot();

    expect(httpClient.get).toHaveBeenCalledWith(
      "/lava/work-orders?page=0&size=100&status=OPEN"
    );
    expect(httpClient.get).toHaveBeenCalledWith(
      "/lava/work-orders?page=0&size=100&status=PAYMENT_PENDING"
    );
    expect(httpClient.get).toHaveBeenCalledWith(
      "/lava/work-orders?page=0&size=100&status=PAID"
    );
    expect(snapshot.workOrders).toEqual([
      expect.objectContaining({
        id: 21,
        clientId: 7,
        clientName: "Marcos Almeida",
        clientPhone: "(71) 98824-1092",
        serviceId: 11,
        service: "Lavagem completa",
        vehicle: "Honda Civic",
        plate: "QXZ8A21",
        vehicleSize: "Médio",
        stage: "waiting",
        operationalStatus: "OPEN",
        createdAt: "2026-07-18T10:00:00Z",
      }),
    ]);
    expect(snapshot.financeEntries).toEqual([]);
  });

  it("cria OS para cliente cadastrado com Idempotency-Key e serviço", async () => {
    const randomUUID = vi.fn(
      () => "00000000-0000-4000-8000-000000000001"
    );
    vi.stubGlobal("crypto", {
      randomUUID,
    });

    httpClient.post.mockResolvedValueOnce(backendWorkOrder);

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.openWorkOrder({
        clientId: 7,
        clientName: "Ignorado pelo backend",
        clientPhone: "(71) 98824-1092",
        serviceId: 11,
        vehicle: "Honda Civic",
        plate: "QXZ8A21",
        service: "Lavagem completa",
        amount: 45,
        vehicleSize: "Médio",
        paymentMethod: "Pendente",
        fiscalDocument: "Recibo geral",
      })
    ).resolves.toMatchObject({
      workOrder: {
        id: 21,
        serviceId: 11,
        vehicleSize: "Médio",
      },
    });

    expect(httpClient.post).toHaveBeenCalledWith(
      "/lava/work-orders",
      {
        clientId: 7,
        serviceId: 11,
        vehicleSize: "MEDIUM",
      },
      {
        headers: {
          "Idempotency-Key":
            "00000000-0000-4000-8000-000000000001",
        },
      }
    );
    expect(httpClient.put).not.toHaveBeenCalled();
    expect(httpClient.get).not.toHaveBeenCalled();

    vi.unstubAllGlobals();
  });

  it("cria OS avulsa com dados do cliente, veículo e placa", async () => {
    const randomUUID = vi.fn(
      () => "00000000-0000-4000-8000-000000000002"
    );
    vi.stubGlobal("crypto", {
      randomUUID,
    });

    const guestWorkOrder = {
      ...backendWorkOrder,
      registeredClientId: null,
      customerNameSnapshot: "Ana Souza",
      customerPhoneDigitsSnapshot: "71999990000",
      vehicleNameSnapshot: "Toyota Corolla",
      vehiclePlateSnapshot: "ABC1D23",
      vehicleSize: "SMALL",
      subtotalCents: 3500,
      totalCents: 3500,
      services: [
        {
          ...backendWorkOrder.services[0],
          priceCents: 3500,
        },
      ],
    };

    httpClient.post.mockResolvedValueOnce(guestWorkOrder);

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.openWorkOrder({
        clientId: null,
        clientName: "Ana Souza",
        clientPhone: "(71) 99999-0000",
        serviceId: 11,
        vehicle: "Toyota Corolla",
        plate: "ABC-1D23",
        service: "Lavagem completa",
        amount: 35,
        vehicleSize: "Pequeno",
        paymentMethod: "Pendente",
        fiscalDocument: "Recibo geral",
      })
    ).resolves.toMatchObject({
      workOrder: {
        clientId: null,
        clientName: "Ana Souza",
        clientPhone: "(71) 99999-0000",
        vehicle: "Toyota Corolla",
        plate: "ABC1D23",
        vehicleSize: "Pequeno",
      },
    });

    expect(httpClient.post).toHaveBeenCalledWith(
      "/lava/work-orders",
      {
        clientId: null,
        customerName: "Ana Souza",
        phone: "(71) 99999-0000",
        vehicleName: "Toyota Corolla",
        plate: "ABC-1D23",
        serviceId: 11,
        vehicleSize: "SMALL",
      },
      {
        headers: {
          "Idempotency-Key":
            "00000000-0000-4000-8000-000000000002",
        },
      }
    );
    expect(httpClient.put).not.toHaveBeenCalled();
    expect(httpClient.get).not.toHaveBeenCalled();

    vi.unstubAllGlobals();
  });

  it("consulta uma OS por ID e reabre o estado mapeado do backend", async () => {
    httpClient.get.mockResolvedValueOnce(backendWorkOrder);

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.getWorkOrder(21)
    ).resolves.toMatchObject({
      id: 21,
      serviceId: 11,
      service: "Lavagem completa",
      stage: "waiting",
      elapsed: "Em aberto",
    });

    expect(httpClient.get).toHaveBeenCalledWith(
      "/lava/work-orders/21"
    );
  });

  it("mantém mensagem compreensível e status 401 sem autenticação", async () => {
    httpClient.get.mockRejectedValueOnce(
      new HttpError(
        "Faça login para continuar.",
        401,
        {
          message: "Faça login para continuar.",
        }
      )
    );

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.getWorkOrder(21)
    ).rejects.toMatchObject({
      message: "Faça login para continuar.",
      status: 401,
    });
  });

  it("prepara checkout e aprova pagamento de credito no SIMULATOR", async () => {
    const randomUUID = vi
      .fn()
      .mockReturnValueOnce(
        "00000000-0000-4000-8000-000000000010"
      )
      .mockReturnValueOnce(
        "00000000-0000-4000-8000-000000000011"
      );
    vi.stubGlobal("crypto", {
      randomUUID,
    });

    const preparedWorkOrder = {
      ...backendWorkOrder,
      status: "PAYMENT_PENDING",
      checkoutSessionId:
        "11111111-1111-4111-8111-111111111111",
      checkoutStatus: "READY_FOR_PAYMENT",
      documentType: "GENERAL_RECEIPT",
      prepared: true,
    };
    const paidWorkOrder = {
      ...preparedWorkOrder,
      status: "PAID",
      checkoutStatus: "FINALIZED",
      paidAt: "2026-07-18T10:10:00Z",
    };
    httpClient.get
      .mockResolvedValueOnce(backendWorkOrder)
      .mockResolvedValueOnce(paidWorkOrder);
    httpClient.post
      .mockResolvedValueOnce(preparedWorkOrder)
      .mockResolvedValueOnce({
        checkout: {
          id: preparedWorkOrder.checkoutSessionId,
          status: "FINALIZED",
          totalCents: 4500,
          paidAt: "2026-07-18T10:10:00Z",
          finalizedAt: "2026-07-18T10:10:00Z",
        },
        payment: {
          status: "APPROVED",
          amountCents: 4500,
          cashReceivedCents: null,
          cashChangeCents: null,
        },
        terminalTransaction: {
          status: "APPROVED",
          responseMessage: "Transacao aprovada.",
          errorMessage: null,
          providerStatus: "APPROVED",
          providerFailureMessage: null,
        },
      });

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.payWorkOrder({
        workOrderId: 21,
        method: "Crédito",
        mode: "terminal",
        document: "Recibo geral",
      })
    ).resolves.toMatchObject({
      approved: true,
      message: "Pagamento aprovado.",
      workOrder: {
        id: 21,
        checkoutStatus: "FINALIZED",
        paymentStatus: "APPROVED",
        paidAt: "2026-07-18T10:10:00Z",
        operationalStatus: "PAID",
        stage: "waiting",
      },
    });

    expect(httpClient.post).toHaveBeenNthCalledWith(
      1,
      "/lava/work-orders/21/prepare",
      {
        documentType: "GENERAL_RECEIPT",
        cpf: null,
        discountCents: 0,
      },
      {
        headers: {
          "Idempotency-Key":
            "00000000-0000-4000-8000-000000000010",
        },
      }
    );
    expect(httpClient.post).toHaveBeenNthCalledWith(
      2,
      "/checkouts/11111111-1111-4111-8111-111111111111/payments/terminal",
      {
        method: "CREDIT_CARD",
      },
      {
        headers: {
          "Idempotency-Key":
            "00000000-0000-4000-8000-000000000011",
        },
        timeoutMs: 135_000,
      }
    );
    expect(httpClient.post).toHaveBeenCalledTimes(2);
    expect(httpClient.get).toHaveBeenLastCalledWith(
      "/lava/work-orders/21"
    );

    vi.unstubAllGlobals();
  });

  it("preserva a OS e permite nova tentativa segura após recusa", async () => {
    const randomUUID = vi.fn(
      () => "00000000-0000-4000-8000-000000000012"
    );
    vi.stubGlobal("crypto", {
      randomUUID,
    });

    const preparedWorkOrder = {
      ...backendWorkOrder,
      status: "PAYMENT_PENDING",
      checkoutSessionId:
        "11111111-1111-4111-8111-111111111111",
      checkoutStatus: "READY_FOR_PAYMENT",
      documentType: "GENERAL_RECEIPT",
      prepared: true,
    };
    const declinedWorkOrder = {
      ...preparedWorkOrder,
      checkoutStatus: "PAYMENT_FAILED",
    };

    httpClient.get
      .mockResolvedValueOnce(preparedWorkOrder)
      .mockResolvedValueOnce(declinedWorkOrder);
    httpClient.post.mockResolvedValueOnce({
      checkout: {
        id: preparedWorkOrder.checkoutSessionId,
        status: "PAYMENT_FAILED",
        totalCents: 4500,
        paidAt: null,
        finalizedAt: null,
      },
      payment: {
        status: "DECLINED",
        amountCents: 4500,
        cashReceivedCents: null,
        cashChangeCents: null,
      },
      terminalTransaction: {
        status: "DECLINED",
        responseMessage:
          "Transacao recusada pelo terminal simulado.",
        errorMessage: null,
        providerStatus: "DECLINED",
        providerFailureMessage: null,
      },
    });

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.payWorkOrder({
        workOrderId: 21,
        method: "Débito",
        mode: "terminal",
        document: "Recibo geral",
      })
    ).resolves.toMatchObject({
      approved: false,
      message:
        "Transacao recusada pelo terminal simulado.",
      workOrder: {
        id: 21,
        checkoutStatus: "PAYMENT_FAILED",
        paymentStatus: "DECLINED",
        service: "Lavagem completa",
      },
    });

    expect(httpClient.post).toHaveBeenCalledTimes(1);
    expect(httpClient.post).toHaveBeenCalledWith(
      "/checkouts/11111111-1111-4111-8111-111111111111/payments/terminal",
      {
        method: "DEBIT_CARD",
      },
      expect.objectContaining({
        headers: expect.objectContaining({
          "Idempotency-Key": expect.any(String),
        }),
      })
    );
    expect(httpClient.post).not.toHaveBeenCalledWith(
      "/lava/work-orders/21/complete"
    );

    vi.unstubAllGlobals();
  });

  it("propaga erro claro do backend sem finalizar checkout indevidamente", async () => {
    const preparedWorkOrder = {
      ...backendWorkOrder,
      status: "PAYMENT_PENDING",
      checkoutSessionId:
        "11111111-1111-4111-8111-111111111111",
      checkoutStatus: "READY_FOR_PAYMENT",
      prepared: true,
    };

    httpClient.get.mockResolvedValueOnce(preparedWorkOrder);
    httpClient.post.mockRejectedValueOnce(
      new HttpError(
        "Nao foi possivel concluir a comunicacao com a maquininha.",
        422,
        {
          message:
            "Nao foi possivel concluir a comunicacao com a maquininha.",
        }
      )
    );

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.payWorkOrder({
        workOrderId: 21,
        method: "Crédito",
        mode: "terminal",
        document: "Recibo geral",
      })
    ).rejects.toThrow(
      "Nao foi possivel concluir a comunicacao com a maquininha."
    );

    expect(httpClient.post).not.toHaveBeenCalledWith(
      "/lava/work-orders/21/complete"
    );
  });

  it("aprova dinheiro com valor recebido e troco do backend", async () => {
    const preparedWorkOrder = {
      ...backendWorkOrder,
      status: "PAYMENT_PENDING",
      checkoutSessionId:
        "11111111-1111-4111-8111-111111111111",
      checkoutStatus: "READY_FOR_PAYMENT",
      prepared: true,
    };
    const paidWorkOrder = {
      ...preparedWorkOrder,
      status: "PAID",
      checkoutStatus: "FINALIZED",
      paidAt: "2026-07-18T10:12:00Z",
    };
    httpClient.get
      .mockResolvedValueOnce(preparedWorkOrder)
      .mockResolvedValueOnce(paidWorkOrder);
    httpClient.post
      .mockResolvedValueOnce({
        checkout: {
          id: preparedWorkOrder.checkoutSessionId,
          status: "FINALIZED",
          totalCents: 4500,
          paidAt: "2026-07-18T10:12:00Z",
          finalizedAt: "2026-07-18T10:12:00Z",
        },
        payment: {
          status: "APPROVED",
          amountCents: 4500,
          cashReceivedCents: 5000,
          cashChangeCents: 500,
        },
      });

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.payWorkOrder({
        workOrderId: 21,
        method: "Dinheiro",
        mode: "cash",
        document: "Recibo geral",
        cashReceived: 50,
      })
    ).resolves.toMatchObject({
      approved: true,
      workOrder: {
        cashReceived: 50,
        cashChange: 5,
        operationalStatus: "PAID",
        stage: "waiting",
      },
    });

    expect(httpClient.post).toHaveBeenNthCalledWith(
      1,
      "/checkouts/11111111-1111-4111-8111-111111111111/payments/cash",
      {
        cashReceivedCents: 5000,
      },
      expect.any(Object)
    );
    expect(httpClient.post).toHaveBeenCalledTimes(1);
  });

  it("conclui OS paga somente quando solicitado pela interface", async () => {
    const completedWorkOrder = {
      ...backendWorkOrder,
      status: "COMPLETED",
      checkoutStatus: "FINALIZED",
      paidAt: "2026-07-18T10:12:00Z",
    };

    httpClient.post.mockResolvedValueOnce(
      completedWorkOrder
    );
    httpClient.get.mockResolvedValueOnce(
      completedWorkOrder
    );

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.changeWorkOrderStage(
        21,
        "done"
      )
    ).resolves.toMatchObject({
      id: 21,
      operationalStatus: "COMPLETED",
      stage: "done",
    });

    expect(httpClient.post).toHaveBeenCalledWith(
      "/lava/work-orders/21/complete"
    );
    expect(httpClient.get).toHaveBeenCalledWith(
      "/lava/work-orders/21"
    );
  });

  it("usa endpoint manual e preserva bloqueio da configuração atual do backend", async () => {
    const preparedWorkOrder = {
      ...backendWorkOrder,
      status: "PAYMENT_PENDING",
      checkoutSessionId:
        "11111111-1111-4111-8111-111111111111",
      checkoutStatus: "READY_FOR_PAYMENT",
      prepared: true,
    };

    httpClient.get.mockResolvedValueOnce(preparedWorkOrder);
    httpClient.post.mockRejectedValueOnce(
      new HttpError(
        "A confirmacao manual de pagamento esta desabilitada.",
        422,
        {
          message:
            "A confirmacao manual de pagamento esta desabilitada.",
        }
      )
    );

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.payWorkOrder({
        workOrderId: 21,
        method: "Crédito",
        mode: "manual",
        document: "Recibo geral",
        manualReason: "Comprovante conferido",
      })
    ).rejects.toThrow(
      "A confirmacao manual de pagamento esta desabilitada."
    );

    expect(httpClient.post).toHaveBeenCalledWith(
      "/checkouts/11111111-1111-4111-8111-111111111111/payments/manual",
      {
        method: "CREDIT_CARD",
        reason: "Comprovante conferido",
      },
      expect.any(Object)
    );
  });

  it("paga OS com Pix simples sem acionar terminal, manual de cartão ou dinheiro", async () => {
    const preparedWorkOrder = {
      ...backendWorkOrder,
      status: "PAYMENT_PENDING",
      checkoutSessionId:
        "11111111-1111-4111-8111-111111111111",
      checkoutStatus: "READY_FOR_PAYMENT",
      prepared: true,
    };
    const paidWorkOrder = {
      ...preparedWorkOrder,
      status: "PAID",
      checkoutStatus: "FINALIZED",
      paymentStatus: "APPROVED",
      paidAt: "2026-07-18T10:12:00Z",
    };

    httpClient.get
      .mockResolvedValueOnce(preparedWorkOrder)
      .mockResolvedValueOnce(paidWorkOrder);
    httpClient.post.mockResolvedValueOnce({
      checkout: {
        id: "11111111-1111-4111-8111-111111111111",
        status: "FINALIZED",
        totalCents: 4500,
        paidAt: "2026-07-18T10:12:00Z",
        finalizedAt: "2026-07-18T10:12:00Z",
      },
      payment: {
        status: "APPROVED",
        amountCents: 4500,
        cashReceivedCents: null,
        cashChangeCents: null,
      },
    });

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.payWorkOrder({
        workOrderId: 21,
        method: "Pix",
        mode: "manual",
        document: "Recibo geral",
        manualReason: "Pix confirmado manualmente.",
      })
    ).resolves.toMatchObject({
      approved: true,
      message: "Pagamento Pix aprovado.",
      workOrder: {
        paymentStatus: "APPROVED",
      },
    });

    expect(httpClient.post).toHaveBeenCalledWith(
      "/checkouts/11111111-1111-4111-8111-111111111111/payments/pix",
      undefined,
      expect.any(Object)
    );
    expect(
      httpClient.post.mock.calls.some(
        ([url]) =>
          String(url).includes("/payments/terminal")
      )
    ).toBe(false);
    expect(
      httpClient.post.mock.calls.some(
        ([url]) =>
          String(url).includes("/payments/manual")
      )
    ).toBe(false);
    expect(
      httpClient.post.mock.calls.some(
        ([url]) =>
          String(url).includes("/payments/cash")
      )
    ).toBe(false);
  });

  it("bloqueia nova cobrança quando a OS já está paga", async () => {
    httpClient.get.mockResolvedValueOnce({
      ...backendWorkOrder,
      status: "PAID",
      checkoutSessionId:
        "11111111-1111-4111-8111-111111111111",
      checkoutStatus: "FINALIZED",
      prepared: true,
      paidAt: "2026-07-18T10:12:00Z",
    });

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.payWorkOrder({
        workOrderId: 21,
        method: "Débito",
        mode: "terminal",
        document: "Recibo geral",
      })
    ).rejects.toThrow(
      "Esta ordem de serviço já possui pagamento aprovado."
    );

    expect(httpClient.post).not.toHaveBeenCalled();
  });
});

describe("httpLavaRepository history", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.resetAllMocks();
  });

  it("lista histórico completo real, filtra somente OS concluída aprovada e carrega detalhes", async () => {
    const completedWorkOrder = {
      ...backendWorkOrder,
      status: "COMPLETED",
      checkoutSessionId:
        "11111111-1111-4111-8111-111111111111",
      checkoutStatus: "FINALIZED",
      documentType: "GENERAL_RECEIPT",
      prepared: true,
      paidAt: "2026-07-18T10:10:00Z",
      updatedAt: "2026-07-18T10:12:00Z",
    };

    httpClient.get
      .mockResolvedValueOnce({
        items: [
          {
            workOrderId: 21,
            checkoutId:
              "11111111-1111-4111-8111-111111111111",
            customerSnapshot: "Marcos Almeida",
            phoneSnapshot: "71988241092",
            vehicleSnapshot: "Honda Civic",
            plateSnapshot: "QXZ8A21",
            vehicleSize: "MEDIUM",
            status: "COMPLETED",
            subtotalCents: 4500,
            discountCents: 0,
            totalCents: 4500,
            paymentMethod: "PIX",
            paymentStatus: "APPROVED",
            paymentId:
              "22222222-2222-4222-8222-222222222222",
            serviceCount: 1,
            createdAt: "2026-07-18T10:00:00Z",
            paidAt: "2026-07-18T10:10:00Z",
            completedAt: "2026-07-18T10:12:00Z",
            cancelledAt: null,
            createdByUserId: 1,
            detailPath:
              "/api/v1/lava/work-orders/21",
          },
          {
            workOrderId: 22,
            checkoutId: null,
            customerSnapshot: "Cliente aberto",
            phoneSnapshot: null,
            vehicleSnapshot: "Jeep Compass",
            plateSnapshot: "ABC1D23",
            vehicleSize: "SMALL",
            status: "OPEN",
            subtotalCents: 3500,
            discountCents: 0,
            totalCents: 3500,
            paymentMethod: null,
            paymentStatus: null,
            paymentId: null,
            serviceCount: 1,
            createdAt: "2026-07-18T10:00:00Z",
            paidAt: null,
            completedAt: null,
            cancelledAt: null,
            createdByUserId: 1,
            detailPath:
              "/api/v1/lava/work-orders/22",
          },
          {
            workOrderId: 23,
            checkoutId: null,
            customerSnapshot: "Cliente recusado",
            phoneSnapshot: null,
            vehicleSnapshot: "Fiat Toro",
            plateSnapshot: "DEF4G56",
            vehicleSize: "MEDIUM",
            status: "COMPLETED",
            subtotalCents: 4500,
            discountCents: 0,
            totalCents: 4500,
            paymentMethod: "DEBIT",
            paymentStatus: "DECLINED",
            paymentId: null,
            serviceCount: 1,
            createdAt: "2026-07-18T10:00:00Z",
            paidAt: null,
            completedAt: "2026-07-18T10:12:00Z",
            cancelledAt: null,
            createdByUserId: 1,
            detailPath:
              "/api/v1/lava/work-orders/23",
          },
        ],
        page: 0,
        size: 7,
        totalPages: 1,
        totalElements: 3,
        first: true,
        last: true,
      })
      .mockResolvedValueOnce(completedWorkOrder);

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    const result =
      await httpLavaRepository.listHistory({
        page: 0,
        size: 7,
      });

    expect(httpClient.get).toHaveBeenNthCalledWith(
      1,
      "/lava/history?page=0&size=7&status=COMPLETED"
    );
    expect(httpClient.get).toHaveBeenNthCalledWith(
      2,
      "/lava/work-orders/21"
    );
    expect(result.entries).toEqual([
      expect.objectContaining({
        id: 21,
        clientName: "Marcos Almeida",
        clientPhone: "(71) 98824-1092",
        vehicle: "Honda Civic",
        plate: "QXZ8A21",
        service: "Lavagem completa",
        amount: 45,
        method: "Pix",
        document: "Recibo geral",
        status: "Concluída",
        paymentStatus: "APPROVED",
      }),
    ]);
    expect(result.totalPages).toBe(1);
    expect(result).toMatchObject({
      page: 0,
      size: 7,
      totalElements: 3,
      first: true,
      last: true,
    });
  });

  it("mantém OS concluída retornada pelo histórico mesmo quando o detalhe secundário ainda vem pago", async () => {
    const paidDetailSnapshot = {
      ...backendWorkOrder,
      status: "PAID",
      checkoutStatus: "FINALIZED",
      documentType: "GENERAL_RECEIPT",
      prepared: true,
      paidAt: "2026-07-18T10:10:00Z",
      updatedAt: "2026-07-18T10:10:00Z",
    };

    httpClient.get
      .mockResolvedValueOnce({
        items: [
          {
            workOrderId: 21,
            checkoutId:
              "11111111-1111-4111-8111-111111111111",
            customerSnapshot: "Marcos Almeida",
            phoneSnapshot: "71988241092",
            vehicleSnapshot: "Honda Civic",
            plateSnapshot: "QXZ8A21",
            vehicleSize: "MEDIUM",
            status: "COMPLETED",
            subtotalCents: 4500,
            discountCents: 0,
            totalCents: 4500,
            paymentMethod: "PIX",
            paymentStatus: "APPROVED",
            paymentId:
              "22222222-2222-4222-8222-222222222222",
            serviceCount: 1,
            createdAt: "2026-07-18T10:00:00Z",
            paidAt: "2026-07-18T10:10:00Z",
            completedAt: "2026-07-18T10:12:00Z",
            cancelledAt: null,
            createdByUserId: 1,
            detailPath:
              "/api/v1/lava/work-orders/21",
          },
        ],
        page: 0,
        size: 7,
        totalPages: 1,
        totalElements: 1,
        first: true,
        last: true,
      })
      .mockResolvedValueOnce(paidDetailSnapshot);

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    const result =
      await httpLavaRepository.listHistory({
        page: 0,
        size: 7,
      });

    expect(result.entries).toEqual([
      expect.objectContaining({
        id: 21,
        status: "Concluída",
        paymentStatus: "APPROVED",
        completedAt: "2026-07-18T10:12:00Z",
      }),
    ]);
    expect(httpClient.get).toHaveBeenNthCalledWith(
      1,
      "/lava/history?page=0&size=7&status=COMPLETED"
    );
    expect(httpClient.get).toHaveBeenNthCalledWith(
      2,
      "/lava/work-orders/21"
    );
  });
});

describe("httpLavaRepository finance", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.resetAllMocks();
  });

  it("carrega resumo e transações do financeiro filtrando somente Lava Jato", async () => {
    httpClient.get
      .mockResolvedValueOnce({
        period: {
          from: "2026-07-18",
          to: "2026-07-18",
          timeZone: "America/Bahia",
        },
        totalRevenueCents: 4500,
        paymentCount: 1,
        averageTicketCents: 4500,
        firstApprovedAt: "2026-07-18T10:10:00Z",
        lastApprovedAt: "2026-07-18T10:10:00Z",
        byModule: [
          {
            key: "LAVA",
            revenueCents: 4500,
            paymentCount: 1,
            revenueBasisPoints: 10000,
          },
        ],
        byOperationType: [
          {
            key: "LAVA_WORK_ORDER",
            revenueCents: 4500,
            paymentCount: 1,
            revenueBasisPoints: 10000,
          },
        ],
        byPaymentMethod: [
          {
            key: "PIX",
            revenueCents: 4500,
            paymentCount: 1,
            revenueBasisPoints: 10000,
          },
        ],
      })
      .mockResolvedValueOnce({
        items: [
          {
            paymentId:
              "22222222-2222-4222-8222-222222222222",
            checkoutId:
              "11111111-1111-4111-8111-111111111111",
            operationType: "LAVA_WORK_ORDER",
            module: "LAVA",
            method: "PIX",
            processingMode: "TERMINAL",
            status: "APPROVED",
            amountCents: 4500,
            approvedAt: "2026-07-18T10:10:00Z",
            responsibleUserId: 1,
            responsibleUserName: "Administrador",
            operationId: 21,
            displayName: "Marcos Almeida",
            description:
              "Honda Civic QXZ8A21 | 1 servicos",
            detailPath:
              "/api/v1/lava/work-orders/21",
          },
          {
            paymentId:
              "33333333-3333-4333-8333-333333333333",
            checkoutId:
              "44444444-4444-4444-8444-444444444444",
            operationType: "BAR_COMMAND",
            module: "BAR",
            method: "CASH",
            processingMode: "CASH",
            status: "APPROVED",
            amountCents: 2000,
            approvedAt: "2026-07-18T10:12:00Z",
            responsibleUserId: 1,
            responsibleUserName: "Administrador",
            operationId: 9,
            displayName: "Mesa 1",
            description: "1 linhas da comanda",
            detailPath: "/api/v1/bar/tabs/9",
          },
          {
            paymentId:
              "55555555-5555-4555-8555-555555555555",
            checkoutId:
              "66666666-6666-4666-8666-666666666666",
            operationType: "LAVA_WORK_ORDER",
            module: "LAVA",
            method: "DEBIT",
            processingMode: "TERMINAL",
            status: "DECLINED",
            amountCents: 4500,
            approvedAt: "2026-07-18T10:13:00Z",
            responsibleUserId: 1,
            responsibleUserName: "Administrador",
            operationId: 22,
            displayName: "Cliente recusado",
            description: "Fiat Toro DEF4G56 | 1 servicos",
            detailPath:
              "/api/v1/lava/work-orders/22",
          },
        ],
        page: 0,
        totalPages: 1,
        totalElements: 3,
      });

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    const result =
      await httpLavaRepository.listFinance({
        from: "2026-07-18",
        to: "2026-07-18",
        page: 0,
        size: 6,
        search: "Marcos",
      });

    expect(httpClient.get).toHaveBeenNthCalledWith(
      1,
      "/finance/summary?from=2026-07-18&to=2026-07-18&module=LAVA&operationType=LAVA_WORK_ORDER&search=Marcos"
    );
    expect(httpClient.get).toHaveBeenNthCalledWith(
      2,
      "/finance/transactions?from=2026-07-18&to=2026-07-18&module=LAVA&operationType=LAVA_WORK_ORDER&page=0&size=6&search=Marcos"
    );
    expect(result.summary).toMatchObject({
      totalRevenueCents: 4500,
      paymentCount: 1,
      byPaymentMethod: [
        expect.objectContaining({
          key: "PIX",
          revenueCents: 4500,
        }),
      ],
    });
    expect(result.transactions).toEqual([
      expect.objectContaining({
        paymentId:
          "22222222-2222-4222-8222-222222222222",
        module: "LAVA",
        operationType: "LAVA_WORK_ORDER",
        status: "APPROVED",
      }),
    ]);
  });

  it("mantém mensagem clara para falta de permissão no financeiro", async () => {
    httpClient.get.mockRejectedValueOnce(
      new HttpError("Forbidden", 403, {
        message: "Forbidden",
      })
    );

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.listFinance({
        from: "2026-07-18",
        to: "2026-07-18",
        page: 0,
        size: 6,
      })
    ).rejects.toMatchObject({
      status: 403,
      message:
        "Sua sessão não possui permissão para acessar o financeiro do Lava Jato.",
    });
  });

  it("mantém mensagem clara para falta de autenticação no financeiro", async () => {
    httpClient.get.mockRejectedValueOnce(
      new HttpError("Unauthorized", 401, {
        message: "Unauthorized",
      })
    );

    const {
      httpLavaRepository,
    } = await import("./httpLavaRepository");

    await expect(
      httpLavaRepository.listFinance({
        from: "2026-07-18",
        to: "2026-07-18",
        page: 0,
        size: 6,
      })
    ).rejects.toMatchObject({
      status: 401,
      message:
        "Faça login para acessar o financeiro do Lava Jato.",
    });
  });
});
