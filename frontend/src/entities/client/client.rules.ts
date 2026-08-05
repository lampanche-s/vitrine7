import { getNextNumericId } from "../../shared/lib/identifiers";
import {
  formatBrazilianPhone,
} from "../../shared/formatters/phoneInput";

import type {
  LavaClient,
  LavaClientInput,
} from "./client.types";

export function normalizeLavaClientInput(
  input: LavaClientInput
): LavaClientInput {
  return {
    name: input.name.trim(),
    phone: formatBrazilianPhone(input.phone),
    vehicle: input.vehicle.trim(),
    plate: input.plate.trim().toUpperCase(),
  };
}

export function isLavaClientInputComplete(
  input: LavaClientInput
): boolean {
  return (
    input.name.length > 0 &&
    input.vehicle.length > 0 &&
    input.plate.length > 0
  );
}

export function createLavaClient(
  clients: readonly LavaClient[],
  input: LavaClientInput
): LavaClient[] {
  return [
    {
      id: getNextNumericId(clients),
      ...input,
      lastService: "Ainda sem serviço",
      visits: 1,
      active: true,
    },
    ...clients,
  ];
}

export function updateLavaClient(
  clients: readonly LavaClient[],
  clientId: number,
  input: LavaClientInput
): LavaClient[] {
  return clients.map((client) =>
    client.id === clientId
      ? {
          ...client,
          ...input,
        }
      : client
  );
}

export function toggleLavaClientStatus(
  clients: readonly LavaClient[],
  clientId: number
): LavaClient[] {
  return clients.map((client) =>
    client.id === clientId
      ? {
          ...client,
          active: !client.active,
        }
      : client
  );
}

export function setLavaClientActive(
  clients: readonly LavaClient[],
  clientId: number,
  active: boolean
): LavaClient[] {
  return clients.map((client) =>
    client.id === clientId
      ? {
          ...client,
          active,
        }
      : client
  );
}

export function removeLavaClient(
  clients: readonly LavaClient[],
  clientId: number
): LavaClient[] {
  return clients.filter(
    (client) => client.id !== clientId
  );
}
