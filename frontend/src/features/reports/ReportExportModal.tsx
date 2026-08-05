import {
  Download,
  X,
} from "lucide-react";

import {
  AnimatedModal,
  Button,
  DropdownSelect,
  TextField,
} from "../../components/ui";

import {
  reportPeriodOptions,
  type ReportPeriodPreset,
  type ReportScope,
} from "./report-period";

type ReportExportModalProps = {
  open: boolean;
  scope: ReportScope | null;
  period: ReportPeriodPreset;
  customFrom: string;
  customTo: string;
  error: string;
  isExporting: boolean;

  onPeriodChange: (
    period: ReportPeriodPreset
  ) => void;

  onCustomFromChange: (
    value: string
  ) => void;

  onCustomToChange: (
    value: string
  ) => void;

  onClose: () => void;
  onConfirm: () => void;
};

export function ReportExportModal({
  open,
  scope,
  period,
  customFrom,
  customTo,
  error,
  isExporting,
  onPeriodChange,
  onCustomFromChange,
  onCustomToChange,
  onClose,
  onConfirm,
}: ReportExportModalProps) {
  const reportName =
    scope === "SERVICE"
      ? "Lava Jato"
      : "Espeto Bar";

  return (
    <AnimatedModal
      open={open}
      onClose={onClose}
      labelledBy="report-export-title"
      describedBy="report-export-description"
      closeOnBackdrop={!isExporting}
      closeOnEscape={!isExporting}
      panelClassName="w-full max-w-[520px] overflow-visible rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <h2
            id="report-export-title"
            className="text-[var(--font-size-section-title)] font-semibold text-[var(--text-base)]"
          >
            Exportar relatório —{" "}
            {reportName}
          </h2>

          <p
            id="report-export-description"
            className="mt-1 text-sm text-[var(--text-muted)]"
          >
            Escolha o período incluído
            no arquivo XLSX.
          </p>
        </div>

        <Button
          size="icon"
          variant="ghost"
          disabled={isExporting}
          onClick={onClose}
          leadingIcon={<X />}
          aria-label="Fechar modal"
          title="Fechar"
        />
      </div>

      <div className="mt-5 grid gap-4">
        <DropdownSelect
          label="Período"
          value={period}
          placeholder="Selecione o período"
          listMaxHeight={280}
          options={reportPeriodOptions.map((option) => ({
            value: option.value,
            label: option.label,
            description: option.description,
          }))}
          onChange={(value) =>
            onPeriodChange(value as ReportPeriodPreset)
          }
        />

        {period === "custom" ? (
          <div className="grid gap-3 sm:grid-cols-2">
            <TextField
              label="Data inicial"
              type="date"
              value={customFrom}
              onChange={
                onCustomFromChange
              }
            />

            <TextField
              label="Data final"
              type="date"
              value={customTo}
              onChange={
                onCustomToChange
              }
            />
          </div>
        ) : null}

        {error ? (
          <div
            className="rounded-[var(--control-radius)] border border-[var(--color-danger-border)] bg-[var(--color-danger-soft)] px-3 py-2 text-sm text-[var(--color-danger)]"
            role="alert"
          >
            {error}
          </div>
        ) : null}
      </div>

      <div className="mt-5 flex flex-col-reverse gap-2 border-t border-[var(--border-subtle)] pt-5 sm:flex-row sm:justify-end">
        <Button
          variant="secondary"
          disabled={isExporting}
          onClick={onClose}
        >
          Cancelar
        </Button>

        <Button
          variant="primary"
          disabled={isExporting}
          leadingIcon={<Download />}
          onClick={onConfirm}
        >
          {isExporting
            ? "Exportando..."
            : "Exportar relatório"}
        </Button>
      </div>
    </AnimatedModal>
  );
}
