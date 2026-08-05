import {
  useState,
  type FormEvent,
} from "react";

import {
  AlertTriangle,
  X,
} from "lucide-react";

import {
  AnimatedModal,
  Button,
  TextAreaField,
  useToast,
} from "../../components/ui";

import {
  HttpError,
} from "../../shared/http";

import {
  formatBrlCurrency,
} from "../../shared/lib/currency";

import {
  reverseTerminalPayment,
} from "./payment-reversal.api";

import {
  getPaymentReversalValidationMessage,
} from "./payment-reversal.rules";

type TerminalPaymentReversalModalProps = {
  open: boolean;
  paymentId: string | null;
  operationLabel: string;
  amount: number;
  onClose: () => void;
  onFinished: () => void;
};

function getErrorCode(
  error: unknown
): string | null {
  if (
    !(error instanceof HttpError) ||
    typeof error.payload !== "object" ||
    error.payload === null ||
    !("code" in error.payload) ||
    typeof error.payload.code !== "string"
  ) {
    return null;
  }

  return error.payload.code;
}

function isAmbiguousResult(
  error: unknown
) {
  if (!(error instanceof HttpError)) {
    return false;
  }

  return (
    getErrorCode(error) ===
      "PAYMENT_REVERSAL_PENDING_CONFIRMATION" ||
    error.status === 408 ||
    error.status === 0
  );
}

export function TerminalPaymentReversalModal({
  open,
  paymentId,
  operationLabel,
  amount,
  onClose,
  onFinished,
}: TerminalPaymentReversalModalProps) {
  const [reason, setReason] =
    useState("");

  const [
    cardholderPresent,
    setCardholderPresent,
  ] = useState(false);

  const [isSubmitting, setIsSubmitting] =
    useState(false);

  const [validationMessage, setValidationMessage] =
    useState("");

  const {
    showToast,
    showErrorToast,
  } = useToast();

  function resetFields() {
    setReason("");
    setCardholderPresent(false);
    setValidationMessage("");
  }

  function requestClose() {
    if (isSubmitting) {
      return;
    }

    resetFields();
    onClose();
  }

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    const normalizedReason =
      reason.trim();

    const nextValidationMessage =
      getPaymentReversalValidationMessage(
        normalizedReason,
        cardholderPresent
      );

    if (nextValidationMessage) {
      setValidationMessage(
        nextValidationMessage
      );

      return;
    }

    if (!paymentId || isSubmitting) {
      return;
    }

    setValidationMessage("");
    setIsSubmitting(true);

    try {
      await reverseTerminalPayment(
        paymentId,
        normalizedReason
      );

      showToast({
        title: "Pagamento estornado",
        description:
          "O estorno foi aprovado e o histórico foi atualizado.",
        variant: "success",
      });

      resetFields();
      onFinished();
      onClose();
    } catch (error) {
      if (isAmbiguousResult(error)) {
        showToast({
          title:
            "Estorno aguardando confirmação",
          description:
            "Verifique a maquininha e a conta PagBank. Não solicite outro estorno enquanto o resultado estiver pendente.",
          variant: "warning",
          duration: 7000,
        });

        resetFields();
        onFinished();
        onClose();

        return;
      }

      showErrorToast(error, {
        title:
          "Não foi possível concluir o estorno",
      });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AnimatedModal
      open={open}
      onClose={requestClose}
      labelledBy="terminal-reversal-title"
      describedBy="terminal-reversal-description"
      closeOnBackdrop={!isSubmitting}
      closeOnEscape={!isSubmitting}
      backdropClassName="z-[360] p-4"
      panelClassName="w-full max-w-[520px] rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
    >
      <form onSubmit={handleSubmit}>
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2
              id="terminal-reversal-title"
              className="text-[var(--font-size-section-title)] font-semibold text-[var(--text-base)]"
            >
              Estornar pagamento
            </h2>

            <p
              id="terminal-reversal-description"
              className="mt-2 text-sm leading-6 text-[var(--text-muted)]"
            >
              {operationLabel} ·{" "}
              {formatBrlCurrency(amount)}
            </p>
          </div>

          <Button
            size="icon"
            variant="ghost"
            disabled={isSubmitting}
            onClick={requestClose}
            leadingIcon={<X />}
            aria-label="Fechar estorno"
            title="Fechar"
          />
        </div>

        <div className="mt-5 rounded-[var(--panel-radius)] border border-[var(--color-accent-border)] bg-[var(--color-accent-soft)] p-3">
          <div className="flex gap-3">
            <AlertTriangle
              className="mt-0.5 h-4 w-4 shrink-0 text-[var(--color-accent)]"
              aria-hidden="true"
            />

            <p className="text-sm leading-6 text-[var(--text-muted)]">
              O estorno será executado na maquininha e
              reverterá o pagamento. A venda original
              continuará registrada no histórico.
            </p>
          </div>
        </div>

        <div className="mt-5">
          <TextAreaField
            label="Motivo do estorno"
            value={reason}
            onChange={setReason}
            disabled={isSubmitting}
            placeholder="Ex.: cliente solicitou o cancelamento da compra"
            className="min-h-24"
          />
        </div>

        <label className="mt-4 flex cursor-pointer items-start gap-3 rounded-[var(--panel-radius)] border border-[var(--border-subtle)] bg-[var(--surface-control)] p-3">
          <input
            type="checkbox"
            checked={cardholderPresent}
            disabled={isSubmitting}
            onChange={(event) =>
              setCardholderPresent(event.target.checked)
            }
            className="mt-0.5 h-4 w-4"
            style={{
              accentColor: "var(--color-accent)",
            }}
          />

          <span className="text-sm leading-5 text-[var(--text-muted)]">
            Confirmo que o cartão e o portador estão presentes
            para concluir o estorno na maquininha.
          </span>
        </label>

        {validationMessage ? (
          <div
            role="alert"
            className="mt-3 rounded-[var(--control-radius)] border border-[var(--color-danger-border)] bg-[var(--color-danger-soft)] px-3 py-2 text-sm text-[var(--color-danger)]"
          >
            {validationMessage}
          </div>
        ) : null}

        <div className="mt-5 flex justify-end gap-2 border-t border-[var(--border-subtle)] pt-5">
          <Button
            variant="secondary"
            disabled={isSubmitting}
            onClick={requestClose}
          >
            Voltar
          </Button>

          <Button
            type="submit"
            variant="primary"
            disabled={
              isSubmitting ||
              !paymentId
            }
          >
            {isSubmitting
              ? "Aguardando maquininha..."
              : "Solicitar estorno"}
          </Button>
        </div>
      </form>
    </AnimatedModal>
  );
}
