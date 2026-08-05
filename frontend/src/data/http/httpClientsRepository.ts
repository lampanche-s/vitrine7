import type {
  Client,
  ClientInput,
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
