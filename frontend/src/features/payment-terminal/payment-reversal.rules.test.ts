import {
  describe,
  expect,
  it,
} from "vitest";

import {
  canMarkPaymentReversed,
  getPaymentReversalValidationMessage,
} from "./payment-reversal.rules";

describe("regras de marcação de estorno", () => {
  const approvedPayment = {
    paymentId: "payment-1",
    paymentStatus: "APPROVED",
  };

  it("permite pagamento aprovado quando o usuário possui permissão", () => {
    expect(
      canMarkPaymentReversed(
        approvedPayment,
        true
      )
    ).toBe(true);
  });

  it("não permite usuário sem permissão", () => {
    expect(
      canMarkPaymentReversed(
        approvedPayment,
        false
      )
    ).toBe(false);
  });

  it("não permite pagamento já estornado", () => {
    expect(
      canMarkPaymentReversed(
        {
          ...approvedPayment,
          paymentStatus: "REVERSED",
        },
        true
      )
    ).toBe(false);
  });

  it("exige motivo válido", () => {
    expect(
      getPaymentReversalValidationMessage(
        "  "
      )
    ).toBe(
      "O motivo deve possuir entre 3 e 255 caracteres."
    );

    expect(
      getPaymentReversalValidationMessage(
        "Cliente solicitou cancelamento"
      )
    ).toBeNull();
  });
});
