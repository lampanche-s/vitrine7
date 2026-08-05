import {
  useRef,
  useState,
} from "react";

import type {
  TerminalPaymentMethod,
  TerminalPaymentModalStatus,
} from "./terminal-payment-modal";

type TerminalPaymentSnapshot = {
  open: boolean;
  status: TerminalPaymentModalStatus;
  method: TerminalPaymentMethod;
  amount: number;
  message?: string;
};

type TerminalPaymentOperation<T> = {
  method: TerminalPaymentMethod;
  amount: number;
  operation: () => Promise<T>;
  isApproved: (result: T) => boolean;
  getMessage?: (result: T) => string | undefined;
  onApproved?: (result: T) => void;
  onRejected?: (result: T) => void;
};

const initialSnapshot: TerminalPaymentSnapshot = {
  open: false,
  status: "processing",
  method: "Crédito",
  amount: 0,
};

function getErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : "Não foi possível confirmar o pagamento.";
}

export function getTerminalPaymentStatusFromMessage(
  message: string
): TerminalPaymentModalStatus {
  const normalizedMessage =
    message.toLowerCase();

  if (
    normalizedMessage.includes("tempo limite") ||
    normalizedMessage.includes("timeout") ||
    normalizedMessage.includes("excedeu")
  ) {
    return "timeout";
  }

  if (
    normalizedMessage.includes("recus") ||
    normalizedMessage.includes("declined") ||
    normalizedMessage.includes("negad")
  ) {
    return "declined";
  }

  if (
    normalizedMessage.includes("comunica") ||
    normalizedMessage.includes("servidor") ||
    normalizedMessage.includes("conex")
  ) {
    return "error";
  }

  return "error";
}

export function useTerminalPaymentFlow<T>() {
  const [
    snapshot,
    setSnapshot,
  ] = useState(initialSnapshot);
  const isProcessingRef =
    useRef(false);

  async function start(
    paymentOperation: TerminalPaymentOperation<T>
  ) {
    if (isProcessingRef.current) {
      return null;
    }

    isProcessingRef.current = true;

    setSnapshot({
      open: true,
      status: "processing",
      method: paymentOperation.method,
      amount: paymentOperation.amount,
    });

    try {
      const result =
        await paymentOperation.operation();
      const message =
        paymentOperation.getMessage?.(result);

      if (paymentOperation.isApproved(result)) {
        setSnapshot({
          open: true,
          status: "approved",
          method: paymentOperation.method,
          amount: paymentOperation.amount,
          message,
        });
        paymentOperation.onApproved?.(result);
        return result;
      }

      setSnapshot({
        open: true,
        status: getTerminalPaymentStatusFromMessage(
          message ?? "Pagamento recusado."
        ),
        method: paymentOperation.method,
        amount: paymentOperation.amount,
        message:
          message ??
          "Pagamento recusado pela maquininha.",
      });
      paymentOperation.onRejected?.(result);
      return null;
    } catch (error) {
      const message =
        getErrorMessage(error);

      setSnapshot({
        open: true,
        status: getTerminalPaymentStatusFromMessage(
          message
        ),
        method: paymentOperation.method,
        amount: paymentOperation.amount,
        message,
      });
      return null;
    } finally {
      isProcessingRef.current = false;
    }
  }

  function close() {
    if (isProcessingRef.current) {
      return;
    }

    setSnapshot((current) => ({
      ...current,
      open: false,
    }));
  }

  return {
    paymentModal: snapshot,
    startTerminalPayment: start,
    closeTerminalPayment: close,
  };
}
