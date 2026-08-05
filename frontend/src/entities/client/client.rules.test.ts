import {
  describe,
  expect,
  it,
} from "vitest";

import {
  isClientInputComplete,
  normalizeClientInput,
} from "./client.rules";

describe("client.rules", () => {
  it("normaliza nome, telefone, veículo e placa", () => {
    expect(
      normalizeClientInput({
        name: "  Ana   Silva ",
        phone: "(71) 99999-0000",
        vehicle: "  Honda   Civic ",
        plate: "abc-1d23",
      })
    ).toEqual({
      name: "Ana Silva",
      phone: "(71) 99999-0000",
      vehicle: "Honda Civic",
      plate: "ABC1D23",
    });
  });

  it("aceita placa antiga ou Mercosul e rejeita dados incompletos", () => {
    expect(
      isClientInputComplete({
        name: "Ana",
        phone: "",
        vehicle: "Civic",
        plate: "ABC1234",
      })
    ).toBe(true);

    expect(
      isClientInputComplete({
        name: "Ana",
        phone: "(71) 99999-0000",
        vehicle: "Civic",
        plate: "ABC1D23",
      })
    ).toBe(true);

    expect(
      isClientInputComplete({
        name: "Ana",
        phone: "123",
        vehicle: "Civic",
        plate: "INVALIDA",
      })
    ).toBe(false);
  });
});
