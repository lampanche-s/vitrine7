import { describe, expect, it } from "vitest";
import { isSupplierInputComplete, normalizeSupplierInput } from "./supplier.rules";

describe("supplier.rules", () => {
  it("normaliza os campos opcionais e exige somente nome", () => {
    expect(normalizeSupplierInput({ name: "  Casa   do  Café ", cnpj: "", phone: null, cep: "  " })).toEqual({ name: "Casa do Café", cnpj: null, phone: null, cep: null });
    expect(isSupplierInputComplete({ name: "Fornecedor", cnpj: null, phone: null, cep: null })).toBe(true);
    expect(isSupplierInputComplete({ name: "  ", cnpj: null, phone: null, cep: null })).toBe(false);
  });
});
