import {
  describe,
  expect,
  it,
} from "vitest";

import {
  canReverseTerminalPayment,
  getPaymentReversalValidationMessage,
} from "./payment-reversal.rules";

describe("regras de estorno na maquininha", () => {
  const approvedCreditPayment = {
    paymentId: "payment-1",
    paymentStatus: "APPROVED",
    method: "Crédito",
  };

  it("permite crédito ou débito aprovado quando o usuário possui permissão", () => {
    expect(
      canReverseTerminalPayment(
        approvedCreditPayment,
        true
      )
    ).toBe(true);

    expect(
      canReverseTerminalPayment(
        {
          ...approvedCreditPayment,
          method: "Débito",
        },
        true
      )
    ).toBe(true);
  });

  it.each([
    "Dinheiro",
    "Pix",
  ])("não permite o método %s", (method) => {
    expect(
      canReverseTerminalPayment(
        {
          ...approvedCreditPayment,
          method,
        },
        true
      )
    ).toBe(false);
  });

  it("não permite operador sem a permissão de estorno", () => {
    expect(
      canReverseTerminalPayment(
        approvedCreditPayment,
        false
      )
    ).toBe(false);
  });

  it.each([
    "REVERSAL_PENDING",
    "REVERSED",
  ])("não permite pagamento no estado %s", (paymentStatus) => {
    expect(
      canReverseTerminalPayment(
        {
          ...approvedCreditPayment,
          paymentStatus,
        },
        true
      )
    ).toBe(false);
  });

  it("exige motivo válido e confirmação da presença do cartão e portador", () => {
    expect(
      getPaymentReversalValidationMessage(
        "  ",
        false
      )
    ).toBe(
      "O motivo deve possuir entre 3 e 255 caracteres."
    );

    expect(
      getPaymentReversalValidationMessage(
        "Cliente solicitou cancelamento",
        false
      )
    ).toBe(
      "Confirme que o cartão e o portador estão presentes."
    );

    expect(
      getPaymentReversalValidationMessage(
        "Cliente solicitou cancelamento",
        true
      )
    ).toBeNull();
  });
});
