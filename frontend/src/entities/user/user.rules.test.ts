import {
  describe,
  expect,
  it,
} from "vitest";

import {
  PASSWORD_MIN_LENGTH,
  PASSWORD_POLICY_MESSAGE,
  isPasswordAccepted,
  normalizeSystemUserInput,
} from "./user.rules";

describe("user rules", () => {
  it("remove apenas espaços externos do username", () => {
    const input = normalizeSystemUserInput({
      name: " Operador ",
      username: " João @ Caixa #1 ",
      password: " 123456 ",
      role: "Operador",
    });

    expect(input.name).toBe("Operador");
    expect(input.username).toBe("João @ Caixa #1");
    expect(input.password).toBe(" 123456 ");
  });

  it("mantém senha mínima de 6 caracteres", () => {
    expect("abc123").toHaveLength(PASSWORD_MIN_LENGTH);
    expect("12345".length).toBeLessThan(PASSWORD_MIN_LENGTH);
    expect(PASSWORD_POLICY_MESSAGE).toBe(
      "A senha deve ter pelo menos 6 caracteres."
    );
  });

  it.each([
    "abcdef",
    "123456",
    "!!!!!!",
    "aaaaaa",
    "senha simples",
    "áááááá",
  ])("aceita senha apenas pelo mínimo de 6 caracteres: %s", (password) => {
    expect(isPasswordAccepted(password)).toBe(true);
  });

  it("recusa senha com 5 caracteres", () => {
    expect(isPasswordAccepted("12345")).toBe(false);
  });
});
