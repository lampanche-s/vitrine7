import {
  httpClient,
} from "../../shared/http";

const TERMINAL_OPERATION_TIMEOUT_MS =
  135_000;

export type TerminalPaymentReversalResponse = {
  commandId: string;

  payment: {
    id: string;
    status:
      | "APPROVED"
      | "REVERSAL_PENDING"
      | "REVERSED"
      | string;
    reversedAt: string | null;
    reversalReason: string | null;
  };

  terminalTransaction: {
    id: string;
    paymentId: string;
    status: string;
    providerReference: string | null;
  };
};

export async function reverseTerminalPayment(
  paymentId: string,
  reason: string
) {
  return httpClient.post<
    TerminalPaymentReversalResponse
  >(
    `/payments/${paymentId}/terminal-reversal`,
    {
      reason: reason.trim(),
      cardholderPresent: true,
    },
    {
      timeoutMs:
        TERMINAL_OPERATION_TIMEOUT_MS,
    }
  );
}
