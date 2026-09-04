export type {
  Client,
  ClientInput,
  ClientConsumptionHistoryEntry,
  ClientConsumptionHistoryLine,
} from "./client.types";

export {
  isClientInputComplete,
  normalizeClientInput,
} from "./client.rules";
