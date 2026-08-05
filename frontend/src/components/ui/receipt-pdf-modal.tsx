import {
  Download,
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
  printHtml,
  onDownload,
  onClose,
}: {
  open: boolean;
  title: string;
  previewUrl: string | null;
  printHtml: string | null;
  onDownload?: () => void;
  onClose: () => void;
}) {
  function handlePrint() {
    if (!printHtml) {
      return;
    }

    const printFrame =
      document.createElement("iframe");

    printFrame.setAttribute(
      "aria-hidden",
      "true"
    );

    Object.assign(printFrame.style, {
      position: "fixed",
      right: "0",
      bottom: "0",
      width: "0",
      height: "0",
      border: "0",
      visibility: "hidden",
    });

    document.body.appendChild(printFrame);

    const printDocument =
      printFrame.contentDocument;

    const printWindow =
      printFrame.contentWindow;

    if (!printDocument || !printWindow) {
      printFrame.remove();
      return;
    }

    printDocument.open();

    printDocument.write(`
      <!doctype html>
      <html lang="pt-BR">
        <head>
          <meta charset="UTF-8" />

          <title>Recibo Vitrine 7</title>

          <style>
            html,
            body {
              width: 80mm;
              height: auto;
              margin: 0;
              padding: 0;
              overflow: visible;
              background: #ffffff;
            }

            *,
            *::before,
            *::after {
              box-sizing: border-box;
            }

            .thermal-receipt {
              width: 80mm;
              height: auto;
              margin: 0;
              padding: 2mm 2mm 3mm;
              overflow: visible;
              color: #000000;
              background: #ffffff;
            }

            .thermal-receipt__content {
              display: block;
              width: max-content;
              max-width: 100%;
              margin: 0 auto;
              padding: 0;
              overflow: visible;
              color: #000000;
              background: transparent;
              font-family:
                ui-monospace,
                SFMono-Regular,
                Menlo,
                Monaco,
                Consolas,
                "Liberation Mono",
                "DejaVu Sans Mono",
                monospace;
              font-size: 8.2px;
              font-weight: 400;
              font-variant-ligatures: none;
              line-height: 1.28;
              letter-spacing: 0;
              white-space: pre;
            }
          </style>
        </head>

        <body>
          ${printHtml}
        </body>
      </html>
    `);

    printDocument.close();

    let removed = false;

    const removePrintFrame = () => {
      if (removed) {
        return;
      }

      removed = true;
      printFrame.remove();
    };

    printWindow.addEventListener(
      "afterprint",
      () => {
        window.setTimeout(
          removePrintFrame,
          300
        );
      },
      {
        once: true,
      }
    );

    window.setTimeout(() => {
      printWindow.focus();
      printWindow.print();

      window.setTimeout(
        removePrintFrame,
        60_000
      );
    }, 150);
  }

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
            onClick={handlePrint}
            disabled={!printHtml}
            className="inline-flex h-9 items-center gap-2 rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-raised)] px-3 text-sm font-medium text-[var(--text-base)] transition hover:border-[var(--border-hover)] hover:bg-[var(--surface-active)] hover:text-[var(--text-base)] disabled:cursor-not-allowed disabled:opacity-40"
          >
            <Printer
              className="h-4 w-4"
              aria-hidden="true"
            />

            Imprimir
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

      {printHtml ? (
        <div
          className="thermal-print-root"
          dangerouslySetInnerHTML={{
            __html: printHtml,
          }}
        />
      ) : null}
    </AnimatedModal>
  );
}
