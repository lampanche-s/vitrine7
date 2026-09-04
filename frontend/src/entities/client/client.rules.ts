import {
  formatBrazilianPhone,
} from "../../shared/formatters/phoneInput";

import type {
  ClientInput,
} from "./client.types";

export function normalizeClientInput(
  input: ClientInput
): ClientInput {
  return {
    name: input.name.trim().replace(/\s+/g, " "),
    phone: formatBrazilianPhone(input.phone),
    vehicle: input.vehicle.trim().replace(/\s+/g, " "),
    plate: input.plate
      .replace(/[^A-Za-z0-9]/g, "")
      .toUpperCase(),
  };
}

export function isClientInputComplete(
  input: ClientInput
): boolean {
  return (
    input.name.length > 0 &&
    input.vehicle.length > 0 &&
    /^[A-Z]{3}(?:\d{4}|\d[A-Z]\d{2})$/.test(input.plate) &&
    (
      input.phone.length === 0 ||
      [10, 11].includes(input.phone.replace(/\D/g, "").length)
    )
  );
}
