import {
  describe,
  expect,
  it,
} from "vitest";

import {
  getTerminalPaymentStatusFromMessage,
} from "./useTerminalPaymentFlow";

describe("getTerminalPaymentStatusFromMessage", () => {
  it("classifica recusas, timeout e erros de comunicação para o modal", () => {
    expect(
      getTerminalPaymentStatusFromMessage(
        "Transacao recusada pelo terminal simulado."
      )
    ).toBe("declined");

    expect(
      getTerminalPaymentStatusFromMessage(
        "A requisição excedeu o tempo limite."
      )
    ).toBe("timeout");

    expect(
      getTerminalPaymentStatusFromMessage(
        "Nao foi possivel concluir a comunicacao com a maquininha."
      )
    ).toBe("error");
  });
});
