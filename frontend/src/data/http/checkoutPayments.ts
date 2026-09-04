import {
  httpClient,
} from "../../shared/http";

export type BackendCheckoutPaymentMethod =
  | "CASH"
  | "PIX"
  | "CREDIT_CARD"
  | "DEBIT_CARD";

export type CheckoutPaymentProcessing =
  | "cash"
  | "pix"
  | "terminal"
  | "manual-card";

const PAYMENT_ATTEMPT_STORAGE_PREFIX =
  "vitrine7.payment-attempt:";
const TERMINAL_PAYMENT_HTTP_TIMEOUT_MS =
  135_000;

const memoryPaymentAttempts =
  new Map<string, string>();

export type CheckoutResponse = {
  id: string;
  status: string;
  totalCents: number;
  paidAt?: string | null;
  finalizedAt: string | null;
};

export type PaymentConfirmationResponse = {
  checkout: CheckoutResponse;
  payment: {
    status: "APPROVED" | "DECLINED" | "PROCESSING" | string;
    amountCents?: number;
    cashReceivedCents?: number | null;
    cashChangeCents?: number | null;
  };
};

export type TerminalPaymentConfirmationResponse =
  PaymentConfirmationResponse & {
    terminalTransaction: {
      status:
        | "APPROVED"
        | "DECLINED"
        | "ERROR"
        | "SENT"
        | string;
      responseMessage: string | null;
      errorMessage: string | null;
      providerStatus?: string | null;
      providerFailureMessage?: string | null;
    };
  };

export function createIdempotencyKey(): string {
  if (
    typeof crypto !== "undefined" &&
    "randomUUID" in crypto
  ) {
    return crypto.randomUUID();
  }

  throw new Error(
    "Não foi possível gerar a chave de idempotência."
  );
}

export const headersForIdempotency = (
  idempotencyKey: string
) => ({
  headers: {
    "Idempotency-Key": idempotencyKey,
  },
});

type PaymentAttemptInput = {
  checkoutId: string;
  processing: CheckoutPaymentProcessing;
  method: BackendCheckoutPaymentMethod;
  amountCents?: number;
  cashReceivedCents?: number;
  reason?: string;
};

function getPaymentAttemptStorage(): Storage | null {
  if (typeof window === "undefined") {
    return null;
  }

  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
}

function getPaymentAttemptStorageKey(
  input: PaymentAttemptInput
): string {
  const descriptor = [
    input.checkoutId,
    input.processing,
    input.method,
    input.amountCents ?? "",
    input.cashReceivedCents ?? "",
    input.reason?.trim() ?? "",
  ].join("|");

  return (
    PAYMENT_ATTEMPT_STORAGE_PREFIX +
    encodeURIComponent(descriptor)
  );
}

function getOrCreatePaymentAttemptKey(
  input: PaymentAttemptInput
): {
  idempotencyKey: string;
  storageKey: string;
} {
  const storageKey =
    getPaymentAttemptStorageKey(input);

  const storage =
    getPaymentAttemptStorage();

  const existingKey =
    storage?.getItem(storageKey) ??
    memoryPaymentAttempts.get(storageKey);

  if (existingKey) {
    return {
      idempotencyKey: existingKey,
      storageKey,
    };
  }

  const idempotencyKey =
    createIdempotencyKey();

  memoryPaymentAttempts.set(
    storageKey,
    idempotencyKey
  );

  try {
    storage?.setItem(
      storageKey,
      idempotencyKey
    );
  } catch {
    // O fallback em memoria continua valido.
  }

  return {
    idempotencyKey,
    storageKey,
  };
}

function clearPaymentAttempt(storageKey: string) {
  memoryPaymentAttempts.delete(storageKey);

  try {
    getPaymentAttemptStorage()?.removeItem(
      storageKey
    );
  } catch {
    // A limpeza em memoria ja foi feita.
  }
}

function isDefinitivePaymentStatus(
  status: string
): boolean {
  return (
    status === "APPROVED" ||
    status === "DECLINED"
  );
}

function getHttpErrorStatus(
  error: unknown
): number | null {
  if (
    typeof error !== "object" ||
    error === null ||
    !("status" in error) ||
    typeof error.status !== "number"
  ) {
    return null;
  }

  return error.status;
}

function shouldKeepPaymentAttempt(
  error: unknown
): boolean {
  const status =
    getHttpErrorStatus(error);

  if (status === null) {
    return true;
  }

  return (
    status === 0 ||
    status === 408 ||
    status >= 500
  );
}

export async function cancelCheckout(
  checkoutId: string,
  reason: string
): Promise<CheckoutResponse> {
  return httpClient.post<CheckoutResponse>(
    `/checkouts/${checkoutId}/cancel`,
    {
      reason,
    }
  );
}

function validateCardMethod(
  method: BackendCheckoutPaymentMethod
) {
  if (
    method !== "CREDIT_CARD" &&
    method !== "DEBIT_CARD"
  ) {
    throw new Error(
      "A confirmação manual aceita apenas crédito ou débito."
    );
  }
}

export async function runCheckoutPayment(input: {
  checkoutId: string;
  processing: "cash";
  method: "CASH";
  amountCents?: number;
  cashReceivedCents: number;
}): Promise<PaymentConfirmationResponse>;

export async function runCheckoutPayment(input: {
  checkoutId: string;
  processing: "pix";
  method: "PIX";
  amountCents?: number;
}): Promise<PaymentConfirmationResponse>;

export async function runCheckoutPayment(input: {
  checkoutId: string;
  processing: "manual-card";
  method: BackendCheckoutPaymentMethod;
  amountCents?: number;
  reason: string;
}): Promise<PaymentConfirmationResponse>;

export async function runCheckoutPayment(input: {
  checkoutId: string;
  processing: "terminal";
  method: BackendCheckoutPaymentMethod;
  amountCents?: number;
}): Promise<TerminalPaymentConfirmationResponse>;

export async function runCheckoutPayment(input: {
  checkoutId: string;
  processing: CheckoutPaymentProcessing;
  method: BackendCheckoutPaymentMethod;
  amountCents?: number;
  cashReceivedCents?: number;
  reason?: string;
}): Promise<
  | PaymentConfirmationResponse
  | TerminalPaymentConfirmationResponse
> {
  const {
    idempotencyKey,
    storageKey,
  } = getOrCreatePaymentAttemptKey(input);

  const idempotencyOptions =
    headersForIdempotency(idempotencyKey);

  try {
    const result = await (async () => {
      if (input.processing === "cash") {
        return httpClient.post<PaymentConfirmationResponse>(
          `/checkouts/${input.checkoutId}/payments/cash`,
          {
            cashReceivedCents: input.cashReceivedCents,
            ...(input.amountCents === undefined ? {} : { amountCents: input.amountCents }),
          },
          idempotencyOptions
        );
      }

      if (input.processing === "pix") {
        return httpClient.post<PaymentConfirmationResponse>(
          `/checkouts/${input.checkoutId}/payments/pix`,
          input.amountCents === undefined
            ? undefined
            : { amountCents: input.amountCents },
          idempotencyOptions
        );
      }

      if (input.processing === "manual-card") {
        validateCardMethod(input.method);

        return httpClient.post<PaymentConfirmationResponse>(
          `/checkouts/${input.checkoutId}/payments/manual`,
          {
            method: input.method,
            reason: input.reason,
            ...(input.amountCents === undefined ? {} : { amountCents: input.amountCents }),
          },
          idempotencyOptions
        );
      }

      validateCardMethod(input.method);

      return httpClient.post<TerminalPaymentConfirmationResponse>(
        `/checkouts/${input.checkoutId}/payments/terminal`,
        {
          method: input.method,
          ...(input.amountCents === undefined ? {} : { amountCents: input.amountCents }),
        },
        {
          ...idempotencyOptions,
          timeoutMs:
            TERMINAL_PAYMENT_HTTP_TIMEOUT_MS,
        }
      );
    })();

    if (
      isDefinitivePaymentStatus(
        result.payment.status
      )
    ) {
      clearPaymentAttempt(storageKey);
    }

    return result;
  } catch (error) {
    if (!shouldKeepPaymentAttempt(error)) {
      clearPaymentAttempt(storageKey);
    }

    throw error;
  }
}
