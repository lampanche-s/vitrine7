import {
  describe,
  expect,
  it,
} from "vitest";

import type {
  LavaDomainState,
} from "./lava-domain.types";

import {
  lavaDomainReducer,
} from "./lava-domain.reducer";

const baseState: LavaDomainState = {
  services: [
    {
      id: 1,
      name: "Servico original",
      category: "Lavagem",
      price: "R$ 30,00",
      smallCarPrice: "R$ 30,00",
      mediumCarPrice: "R$ 40,00",
      duration: "30 min",
      active: true,
    },
  ],
  clients: [
    {
      id: 1,
      name: "Cliente original",
      phone: "",
      vehicle: "Honda Civic",
      plate: "QXZ8A21",
      lastService: "Ainda sem serviço",
      visits: 0,
      active: true,
    },
  ],
  progressVehicles: [],
  financeEntries: [],
  historyEntries: [],
};

describe("lavaDomainReducer clients", () => {
  it("não duplica cliente criado quando a mesma resposta é aplicada novamente", () => {
    const createdClient = {
      id: 2,
      name: "Cliente HTTP",
      phone: "",
      vehicle: "Jeep Compass",
      plate: "RTA4C90",
      lastService: "Ainda sem serviço",
      visits: 0,
      active: true,
    };

    const once = lavaDomainReducer(baseState, {
      type: "client/created",
      payload: createdClient,
    });

    const twice = lavaDomainReducer(once, {
      type: "client/created",
      payload: createdClient,
    });

    expect(
      twice.clients.filter(
        (client) => client.id === createdClient.id
      )
    ).toHaveLength(1);
    expect(twice.clients[0]).toEqual(createdClient);
  });
});

describe("lavaDomainReducer services", () => {
  it("não duplica serviço criado quando a mesma resposta é aplicada novamente", () => {
    const createdService = {
      id: 2,
      name: "Servico HTTP",
      category: "Lavagem",
      price: "R$ 35,00",
      smallCarPrice: "R$ 35,00",
      mediumCarPrice: "R$ 45,00",
      duration: "40 min",
      active: true,
    };

    const once = lavaDomainReducer(baseState, {
      type: "service/created",
      payload: createdService,
    });

    const twice = lavaDomainReducer(once, {
      type: "service/created",
      payload: createdService,
    });

    expect(
      twice.services.filter(
        (service) => service.id === createdService.id
      )
    ).toHaveLength(1);
    expect(twice.services[0]).toEqual(createdService);
  });
});

describe("lavaDomainReducer work orders", () => {
  it("não duplica OS criada quando a mesma resposta é aplicada novamente", () => {
    const createdWorkOrder = {
      id: 3,
      financeEntryId: 3,
      clientId: 1,
      clientName: "Cliente HTTP",
      clientPhone: "",
      serviceId: 1,
      vehicle: "Honda Civic",
      plate: "QXZ8A21",
      service: "Lavagem completa",
      vehicleSize: "Pequeno" as const,
      notes: "",
      stage: "waiting" as const,
      elapsed: "Em aberto",
      serviceDurationMinutes: null,
    };

    const once = lavaDomainReducer(baseState, {
      type: "work-order/created",
      payload: {
        workOrder: createdWorkOrder,
      },
    });

    const twice = lavaDomainReducer(once, {
      type: "work-order/created",
      payload: {
        workOrder: createdWorkOrder,
      },
    });

    expect(
      twice.progressVehicles.filter(
        (workOrder) =>
          workOrder.id === createdWorkOrder.id
      )
    ).toHaveLength(1);
    expect(twice.progressVehicles[0]).toEqual(
      createdWorkOrder
    );
    expect(twice.financeEntries).toEqual([]);
  });

  it("mantém apenas OS ativas e remove concluídas após atualização", () => {
    const activeWorkOrder = {
      id: 4,
      financeEntryId: 4,
      clientId: 1,
      clientName: "Cliente ativo",
      clientPhone: "",
      serviceId: 1,
      vehicle: "Honda Civic",
      plate: "QXZ8A21",
      service: "Lavagem completa",
      vehicleSize: "Pequeno" as const,
      notes: "",
      stage: "waiting" as const,
      elapsed: "Em aberto",
      operationalStatus: "PAYMENT_PENDING" as const,
      serviceDurationMinutes: null,
    };

    const completedWorkOrder = {
      ...activeWorkOrder,
      operationalStatus: "COMPLETED" as const,
      stage: "done" as const,
      elapsed: "COMPLETED",
    };

    const loaded = lavaDomainReducer(baseState, {
      type: "state/replaced",
      payload: {
        ...baseState,
        progressVehicles: [
          activeWorkOrder,
          completedWorkOrder,
          activeWorkOrder,
        ],
      },
    });

    expect(loaded.progressVehicles).toEqual([
      activeWorkOrder,
    ]);

    const afterCompletion = lavaDomainReducer(loaded, {
      type: "work-order/updated",
      payload: completedWorkOrder,
    });

    expect(afterCompletion.progressVehicles).toEqual([]);
  });
});
