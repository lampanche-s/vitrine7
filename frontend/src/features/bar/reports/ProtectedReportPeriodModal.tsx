import type { FormEvent } from "react";

import {
  AnimatedModal,
  Button,
  TextField,
} from "../../../components/ui";

type ProtectedReportPeriodModalProps = {
  open: boolean;
  password: string;
  error: string;
  onPasswordChange: (password: string) => void;
  onCancel: () => void;
  onConfirm: () => void;
};

export function ProtectedReportPeriodModal({
  open,
  password,
  error,
  onPasswordChange,
  onCancel,
  onConfirm,
}: ProtectedReportPeriodModalProps) {
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onConfirm();
  }

  return (
    <AnimatedModal
      open={open}
      onClose={onCancel}
      labelledBy="protected-report-period-title"
      describedBy="protected-report-period-description"
      panelClassName="w-full max-w-[440px] overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
    >
      <form onSubmit={handleSubmit}>
        <h2
          id="protected-report-period-title"
          className="text-[var(--font-size-section-title)] font-semibold text-[var(--text-base)]"
        >
          Acesso aos relatórios
        </h2>

        <p
          id="protected-report-period-description"
          className="mt-1 text-sm text-[var(--text-muted)]"
        >
          Este período exige senha de acesso.
        </p>

        <div className="mt-5 grid gap-2">
          <TextField
            label="Senha"
            type="password"
            value={password}
            autoFocus
            onChange={onPasswordChange}
          />

          {error ? (
            <p
              className="text-sm text-[var(--color-danger)]"
              role="alert"
            >
              {error}
            </p>
          ) : null}
        </div>

        <div className="mt-5 flex justify-end gap-2 border-t border-[var(--border-subtle)] pt-5">
          <Button variant="secondary" onClick={onCancel}>
            Cancelar
          </Button>
          <Button type="submit" variant="primary">
            Confirmar
          </Button>
        </div>
      </form>
    </AnimatedModal>
  );
}
