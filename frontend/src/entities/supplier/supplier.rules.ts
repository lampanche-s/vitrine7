import type { SupplierInput } from "./supplier.types";

export function normalizeSupplierInput(input: SupplierInput): SupplierInput {
  return {
    name: input.name.trim().replace(/\s+/g, " "),
    cnpj: input.cnpj?.trim() || null,
    phone: input.phone?.trim() || null,
    cep: input.cep?.trim() || null,
  };
}

export function isSupplierInputComplete(input: SupplierInput): boolean {
  return input.name.trim().length > 0;
}
