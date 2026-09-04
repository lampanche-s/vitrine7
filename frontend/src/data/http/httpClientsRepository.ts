import type {
  Client,
  ClientInput,
  ClientConsumptionHistoryEntry,
} from "../../entities/client";

import type {
  ClientsRepository,
} from "../contracts/clients.repository";

import {
  formatBrazilianPhone,
} from "../../shared/formatters/phoneInput";

import {
  httpClient,
} from "../../shared/http";

import {
  listAllPages,
  type HttpPageResponse,
} from "./listAllPages";

type ClientResponse = {
  id: number;
  name: string;
  phone: string | null;
  vehicleName: string;
  plate: string;
  active: boolean;
};


type ClientConsumptionHistoryResponse = {
  operationId: number;
  completedAt: string;
  totalCents: number;
  paymentStatus: string;
  lines: {
    entryType: "ITEM" | "SERVICE";
    itemName: string;
    quantity: number;
    unitPriceCents: number;
    lineTotalCents: number;
  }[];
};

function mapConsumptionHistory(
  entry: ClientConsumptionHistoryResponse
): ClientConsumptionHistoryEntry {
  return {
    operationId: entry.operationId,
    completedAt: entry.completedAt,
    total: entry.totalCents / 100,
    paymentStatus: entry.paymentStatus,
    lines: entry.lines.map((line) => ({
      entryType: line.entryType,
      itemName: line.itemName,
      quantity: line.quantity,
      unitPrice: line.unitPriceCents / 100,
      total: line.lineTotalCents / 100,
    })),
  };
}

function mapClient(
  client: ClientResponse
): Client {
  return {
    id: client.id,
    name: client.name,
    phone: formatBrazilianPhone(client.phone ?? ""),
    vehicle: client.vehicleName,
    plate: client.plate,
    active: client.active,
  };
}

function requestBody(input: ClientInput) {
  return {
    name: input.name,
    phone: input.phone || null,
    vehicleName: input.vehicle,
    plate: input.plate,
  };
}

export const httpClientsRepository: ClientsRepository = {
  async list() {
    const clients = await listAllPages<ClientResponse>(
      (page) =>
        httpClient.get<HttpPageResponse<ClientResponse>>(
          `/clients?page=${page}&size=100&sort=name&direction=ASC`
        )
    );

    return clients.map(mapClient);
  },

  async consumptionHistory(clientId) {
    const history = await httpClient.get<ClientConsumptionHistoryResponse[]>(
      `/clients/${clientId}/consumption-history`
    );

    return history.map(mapConsumptionHistory);
  },

  async create(input) {
    return mapClient(
      await httpClient.post<ClientResponse>(
        "/clients",
        requestBody(input)
      )
    );
  },

  async update(clientId, input) {
    return mapClient(
      await httpClient.put<ClientResponse>(
        `/clients/${clientId}`,
        requestBody(input)
      )
    );
  },

  async setActive(clientId, active) {
    return mapClient(
      await httpClient.patch<ClientResponse>(
        `/clients/${clientId}/active`,
        { active }
      )
    );
  },

  async remove(clientId) {
    await httpClient.delete<void>(
      `/clients/${clientId}`
    );
  },
};
