type ReversiblePayment = {
  paymentId?: string | null;
  paymentStatus?: string | null;
  method: string;
};

const REVERSIBLE_PAYMENT_METHODS = new Set([
  "Crédito",
  "Débito",
  "Cartão",
]);

export function canReverseTerminalPayment(
  payment: ReversiblePayment | null | undefined,
  hasReversalPermission: boolean
): boolean {
  return Boolean(
    payment?.paymentId &&
      payment.paymentStatus === "APPROVED" &&
      REVERSIBLE_PAYMENT_METHODS.has(
        payment.method
      ) &&
      hasReversalPermission
  );
}

export function getPaymentReversalValidationMessage(
  reason: string,
  cardholderPresent: boolean
): string | null {
  const normalizedReason = reason.trim();

  if (
    normalizedReason.length < 3 ||
    normalizedReason.length > 255
  ) {
    return "O motivo deve possuir entre 3 e 255 caracteres.";
  }

  if (!cardholderPresent) {
    return "Confirme que o cartão e o portador estão presentes.";
  }

  return null;
}
