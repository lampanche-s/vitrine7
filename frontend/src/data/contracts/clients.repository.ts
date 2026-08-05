import type {
  Client,
  ClientInput,
} from "../../entities/client";

export interface ClientsRepository {
  list(): Promise<Client[]>;

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
