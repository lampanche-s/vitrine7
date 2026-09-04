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
  formatBrlCurrency,
} from "../../shared/lib/currency";

import {
  markPaymentReversed,
} from "./payment-reversal.api";

import {
  getPaymentReversalValidationMessage,
} from "./payment-reversal.rules";

type PaymentReversalModalProps = {
  open: boolean;
  paymentId: string | null;
  operationLabel: string;
  amount: number;
  onClose: () => void;
  onFinished: () => void;
};

export function PaymentReversalModal({
  open,
  paymentId,
  operationLabel,
  amount,
  onClose,
  onFinished,
}: PaymentReversalModalProps) {
  const [reason, setReason] =
    useState("");

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
        normalizedReason
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
      await markPaymentReversed(
        paymentId,
        normalizedReason
      );

      showToast({
        title: "Venda marcada como estornada",
        description:
          "O histórico e os relatórios foram atualizados.",
        variant: "success",
      });

      resetFields();
      onFinished();
      onClose();
    } catch (error) {
      showErrorToast(error, {
        title:
          "Não foi possível marcar a venda como estornada",
      });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AnimatedModal
      open={open}
      onClose={requestClose}
      labelledBy="payment-reversal-title"
      describedBy="payment-reversal-description"
      closeOnBackdrop={!isSubmitting}
      closeOnEscape={!isSubmitting}
      backdropClassName="z-[360] p-4"
      panelClassName="w-full max-w-[520px] rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
    >
      <form onSubmit={handleSubmit}>
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2
              id="payment-reversal-title"
              className="text-[var(--font-size-section-title)] font-semibold text-[var(--text-base)]"
            >
              Marcar como estornada
            </h2>

            <p
              id="payment-reversal-description"
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
            aria-label="Fechar"
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
              A venda continuará no histórico e deixará de contar nos relatórios.
            </p>
          </div>
        </div>

        <div className="mt-5">
          <TextAreaField
            label="Motivo do estorno"
            value={reason}
            onChange={setReason}
            disabled={isSubmitting}
            placeholder="Ex.: cliente solicitou o cancelamento"
            className="min-h-24"
          />
        </div>

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
            variant="danger"
            disabled={
              isSubmitting ||
              !paymentId
            }
          >
            {isSubmitting
              ? "Salvando..."
              : "Marcar como estornada"}
          </Button>
        </div>
      </form>
    </AnimatedModal>
  );
}
