type ReversiblePayment = {
  paymentId?: string | null;
  paymentStatus?: string | null;
};

export function canMarkPaymentReversed(
  payment: ReversiblePayment | null | undefined,
  hasReversalPermission: boolean
): boolean {
  return Boolean(
    payment?.paymentId &&
      payment.paymentStatus === "APPROVED" &&
      hasReversalPermission
  );
}

export function getPaymentReversalValidationMessage(
  reason: string
): string | null {
  const normalizedReason = reason.trim();

  if (
    normalizedReason.length < 3 ||
    normalizedReason.length > 255
  ) {
    return "O motivo deve possuir entre 3 e 255 caracteres.";
  }

  return null;
}
