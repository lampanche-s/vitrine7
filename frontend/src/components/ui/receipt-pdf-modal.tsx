import {
  Download,
  LoaderCircle,
  Printer,
  X,
} from "lucide-react";

import {
  AnimatedModal,
} from "./animated-modal";

export function ReceiptPdfModal({
  open,
  title,
  previewUrl,
  isPrinting = false,
  onPrint,
  onDownload,
  onClose,
}: {
  open: boolean;
  title: string;
  previewUrl: string | null;
  isPrinting?: boolean;
  onPrint?: () => void;
  onDownload?: () => void;
  onClose: () => void;
}) {
  return (
    <AnimatedModal
      open={open}
      onClose={onClose}
      labelledBy="receipt-pdf-title"
      backdropClassName="z-[360] p-4"
      panelClassName="flex h-[min(760px,calc(100dvh-32px))] w-full max-w-[520px] flex-col overflow-hidden rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-card)] shadow-[var(--shadow-modal)]"
    >
      <div className="flex shrink-0 items-center justify-between gap-4 border-b border-[var(--border-subtle)] px-5 py-4">
        <div className="min-w-0">
          <h2
            id="receipt-pdf-title"
            className="truncate text-base font-semibold uppercase text-[var(--text-base)]"
          >
            Cupom / Comprovante
          </h2>

          <p className="mt-1 truncate text-sm text-[var(--text-muted)]">
            {title}
          </p>
        </div>

        <div className="flex shrink-0 items-center gap-2">
          {onDownload ? (
            <button
              type="button"
              onClick={onDownload}
              className="inline-flex h-9 items-center gap-2 rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-raised)] px-3 text-sm font-medium text-[var(--text-base)] transition hover:border-[var(--border-hover)] hover:bg-[var(--surface-active)] hover:text-[var(--text-base)]"
            >
              <Download
                className="h-4 w-4"
                aria-hidden="true"
              />

              Baixar
            </button>
          ) : null}

          <button
            type="button"
            onClick={onPrint}
            disabled={!onPrint || isPrinting}
            className="inline-flex h-9 items-center gap-2 rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-raised)] px-3 text-sm font-medium text-[var(--text-base)] transition hover:border-[var(--border-hover)] hover:bg-[var(--surface-active)] hover:text-[var(--text-base)] disabled:cursor-not-allowed disabled:opacity-40"
          >
            {isPrinting ? (
              <LoaderCircle
                className="h-4 w-4 animate-spin"
                aria-hidden="true"
              />
            ) : (
              <Printer
                className="h-4 w-4"
                aria-hidden="true"
              />
            )}

            {isPrinting
              ? "Imprimindo..."
              : "Imprimir"}
          </button>

          <button
            type="button"
            onClick={onClose}
            className="grid h-9 w-9 place-items-center rounded-[4px] border border-[var(--border-subtle)] text-[var(--text-muted)] transition hover:bg-[var(--surface-hover)] hover:text-[var(--text-base)]"
            aria-label="Fechar comprovante"
          >
            <X
              className="h-4 w-4"
              aria-hidden="true"
            />
          </button>
        </div>
      </div>

      <div className="min-h-0 flex-1 bg-[var(--surface-panel)] px-4 py-5">
        <div className="flex min-h-full justify-center">
          {previewUrl ? (
            <div className="premium-scroll max-h-[500px] w-[302px] overflow-y-auto rounded-[3px] border border-[var(--border-subtle)] bg-white shadow-[var(--shadow-receipt)]">
              <img
                src={previewUrl}
                alt="Pré-visualização do cupom/comprovante"
                className="block h-auto w-[302px] max-w-none bg-white"
              />
            </div>
          ) : (
            <div className="grid min-h-[360px] w-[302px] place-items-center text-sm text-[var(--text-muted)]">
              Comprovante indisponível.
            </div>
          )}
        </div>
      </div>
    </AnimatedModal>
  );
}
