import type {
  Client,
  ClientInput,
  ClientConsumptionHistoryEntry,
} from "../../entities/client";

export interface ClientsRepository {
  list(): Promise<Client[]>;

  consumptionHistory(
    clientId: number
  ): Promise<ClientConsumptionHistoryEntry[]>;

  create(
    input: ClientInput
  ): Promise<Client>;

  update(
    clientId: number,
    input: ClientInput
  ): Promise<Client>;

  setActive(
    clientId: number,
    active: boolean
  ): Promise<Client>;

  remove(
    clientId: number
  ): Promise<void>;
}
