import {
  httpClient,
} from "../../shared/http";

export type PaymentReversalResponse = {
  id: string;
  status: "REVERSED" | string;
  reversedAt: string | null;
  reversedByUserId: number | null;
  reversalReason: string | null;
};

export async function markPaymentReversed(
  paymentId: string,
  reason: string
) {
  return httpClient.post<PaymentReversalResponse>(
    `/payments/${paymentId}/reversal`,
    {
      reason: reason.trim(),
    }
  );
}
