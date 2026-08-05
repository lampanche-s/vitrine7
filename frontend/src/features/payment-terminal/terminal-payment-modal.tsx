import {
  CheckCircle2,
  LoaderCircle,
  X,
  XCircle,
} from "lucide-react";

import {
  AnimatedModal,
  Button,
} from "../../components/ui";

import {
  formatBrlCurrency,
} from "../../shared/lib/currency";

export type TerminalPaymentModalStatus =
  | "processing"
  | "approved"
  | "declined"
  | "error"
  | "timeout";

export type TerminalPaymentMethod =
  | "Crédito"
  | "Débito"
  | "Cartão"
  | string;

type TerminalPaymentModalProps = {
  open: boolean;
  status: TerminalPaymentModalStatus;
  method: TerminalPaymentMethod;
  amount: number;
  message?: string;
  onClose: () => void;
  onContinue?: () => void;
};

function getTitle(status: TerminalPaymentModalStatus) {
  if (status === "processing") {
    return "Aguardando pagamento";
  }

  if (status === "approved") {
    return "Pagamento concluído e registrado";
  }

  if (status === "declined") {
    return "Pagamento recusado";
  }

  if (status === "timeout") {
    return "Tempo limite excedido";
  }

  return "Erro de comunicação";
}

function getDescription(
  status: TerminalPaymentModalStatus,
  message?: string
) {
  if (status === "processing") {
    return "Finalize o pagamento na maquininha.";
  }

  if (status === "approved") {
    return "A confirmação real do terminal foi recebida.";
  }

  return (
    message ??
    "Não foi possível confirmar o pagamento no terminal."
  );
}

export function TerminalPaymentModal({
  open,
  status,
  method,
  amount,
  message,
  onClose,
  onContinue,
}: TerminalPaymentModalProps) {
  const isProcessing =
    status === "processing";
  const isApproved =
    status === "approved";

  return (
    <AnimatedModal
      open={open}
      onClose={
        isProcessing
          ? () => undefined
          : onClose
      }
      labelledBy="terminal-payment-modal-title"
      describedBy="terminal-payment-modal-description"
      closeOnBackdrop={false}
      closeOnEscape={!isProcessing}
      backdropClassName="z-[420] p-4"
      panelClassName="w-full max-w-[440px] overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
    >
      <div className="text-center">
        <div
          className={[
            "mx-auto grid h-16 w-16 place-items-center rounded-[var(--panel-radius)] border",
            isProcessing
              ? "border-[var(--color-accent-border)] bg-[var(--color-accent-soft)] text-[var(--color-accent)]"
              : isApproved
                ? "border-[var(--color-success-border)] bg-[var(--color-success-soft)] text-[var(--color-success)]"
                : "border-[var(--color-danger-border)] bg-[var(--color-danger-soft)] text-[var(--color-danger)]",
          ].join(" ")}
        >
          {isProcessing ? (
            <LoaderCircle
              className="h-7 w-7 animate-spin"
              aria-hidden="true"
            />
          ) : isApproved ? (
            <CheckCircle2
              className="h-7 w-7"
              aria-hidden="true"
            />
          ) : (
            <XCircle
              className="h-7 w-7"
              aria-hidden="true"
            />
          )}
        </div>

        <h2
          id="terminal-payment-modal-title"
          className="mt-5 text-[var(--font-size-page-title)] font-semibold text-[var(--text-base)]"
        >
          {getTitle(status)}
        </h2>

        <p
          id="terminal-payment-modal-description"
          className="mx-auto mt-2 max-w-[320px] text-sm leading-6 text-[var(--text-muted)]"
        >
          {getDescription(status, message)}
        </p>

        <div className="mt-5 grid gap-2 rounded-[var(--panel-radius)] border border-[var(--border-subtle)] bg-[var(--surface-control)] p-3 text-left">
          <div className="flex items-center justify-between gap-4">
            <span className="text-xs font-medium uppercase text-[var(--text-subtle)]">
              Método
            </span>

            <span className="text-sm font-semibold text-[var(--text-base)]">
              {method}
            </span>
          </div>

          <div className="flex items-center justify-between gap-4">
            <span className="text-xs font-medium uppercase text-[var(--text-subtle)]">
              Valor
            </span>

            <span className="text-sm font-semibold text-[var(--text-base)]">
              {formatBrlCurrency(amount)}
            </span>
          </div>
        </div>
      </div>

      {!isProcessing ? (
        <div className="mt-5 flex justify-end border-t border-[var(--border-subtle)] pt-5">
          {isApproved ? (
            <Button
              variant="primary"
              onClick={onContinue ?? onClose}
            >
              Continuar
            </Button>
          ) : (
            <Button
              variant="secondary"
              leadingIcon={<X />}
              onClick={onClose}
            >
              Fechar
            </Button>
          )}
        </div>
      ) : null}
    </AnimatedModal>
  );
}
