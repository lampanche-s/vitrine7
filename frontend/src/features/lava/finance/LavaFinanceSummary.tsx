import {
  useMemo,
  useState,
} from "react";
import {
  Check,
  Eye,
  X,
} from "lucide-react";
import {
  AnimatedModal,
  DropdownField,
  EmptyState,
  InfoField,
  PaginationFooter,
  PremiumCard,
  ReceiptPdfModal,
  SectionTitle,
  SearchField,
} from "../../../components/ui";
import type {
  LavaFinanceEntry,
  LavaPaymentMethod,
  SettleLavaFinanceEntryInput,
} from "../../../entities/payment";
import {
  formatBrlCurrency,
} from "../../../shared/lib/currency";
import {
  getCurrentShortTime,
} from "../../../shared/lib/date-time";
import {
  createReceiptPrintHtml,
  createReceiptPreviewImage,
  downloadReceiptPdf,
  type ReceiptDocument,
} from "../../../shared/receipts/receiptDocument";

const LAVA_FINANCE_PAGE_SIZE = 7;
const GENERAL_RECEIPT_DOCUMENT = "Recibo geral";

export function LavaFinanceSummary({
  entries,
  onSettle,
}: {
  entries: LavaFinanceEntry[];
  onSettle: (
    input: SettleLavaFinanceEntryInput
  ) => Promise<boolean>;
}) {
  const [searchTerm, setSearchTerm] = useState("");
  const [financePage, setFinancePage] = useState(1);
  const [selectedEntryId, setSelectedEntryId] = useState<number | null>(null);
  const [receiptPrintHtml, setReceiptPrintHtml] = useState<string | null>(null);
  const [receiptPreviewUrl, setReceiptPreviewUrl] = useState<string | null>(null);
  const [isReceiptModalOpen, setIsReceiptModalOpen] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<LavaPaymentMethod>("Dinheiro");

  const selectedEntry = useMemo(
    () => entries.find((entry) => entry.id === selectedEntryId),
    [
      entries,
      selectedEntryId,
    ]
  );

  const financialReceipt: ReceiptDocument | null =
    selectedEntry
      ? {
          title: selectedEntry.vehicle,
          subtitle: "Comprovante financeiro da ordem de serviço",
          code: selectedEntry.plate,
          amount: formatBrlCurrency(selectedEntry.amount),
          payment: selectedEntry.method,
          issuedAt: selectedEntry.time,
          items: [
            {
              quantity: 1,
              name: selectedEntry.service,
              unitPrice: selectedEntry.amount,
              total: selectedEntry.amount,
            },
          ],
          lines: [
            {
              label: "Placa",
              value: selectedEntry.plate,
            },
            {
              label: "Serviço",
              value: selectedEntry.service,
            },
          ],
        }
      : null;

  const filteredEntries = useMemo(() => {
    const normalizedSearch = searchTerm.trim().toLowerCase();

    if (normalizedSearch.length === 0) {
      return entries;
    }

    return entries.filter((entry) =>
      entry.vehicle.toLowerCase().includes(normalizedSearch) ||
      entry.plate.toLowerCase().includes(normalizedSearch) ||
      entry.service.toLowerCase().includes(normalizedSearch) ||
      entry.method.toLowerCase().includes(normalizedSearch) ||
      entry.document.toLowerCase().includes(normalizedSearch)
    );
  }, [
    entries,
    searchTerm,
  ]);

  const financeTotalPages = Math.max(
    1,
    Math.ceil(filteredEntries.length / LAVA_FINANCE_PAGE_SIZE)
  );

  const safeFinancePage = Math.min(financePage, financeTotalPages);

  const visibleFinanceEntries = useMemo(
    () =>
      filteredEntries.slice(
        (safeFinancePage - 1) * LAVA_FINANCE_PAGE_SIZE,
        safeFinancePage * LAVA_FINANCE_PAGE_SIZE
      ),
    [
      filteredEntries,
      safeFinancePage,
    ]
  );

  function closeDetailsModal() {
    setSelectedEntryId(null);
    setPaymentMethod("Dinheiro");
  }

  function openReceiptModal(receipt: ReceiptDocument) {
    setReceiptPrintHtml(createReceiptPrintHtml(receipt));
    setReceiptPreviewUrl(createReceiptPreviewImage(receipt));
    setIsReceiptModalOpen(true);
  }

  function closeReceiptModal() {
    setIsReceiptModalOpen(false);

    window.setTimeout(() => {
      setReceiptPreviewUrl(null);
      setReceiptPrintHtml(null);
    }, 160);
  }

  async function confirmPayment() {
    if (!selectedEntry || selectedEntry.status !== "pending") {
      return;
    }

    const settled = await onSettle({
      entryId: selectedEntry.id,
      method: paymentMethod,
      document: GENERAL_RECEIPT_DOCUMENT,
      time: getCurrentShortTime(),
      paidAt: new Date().toISOString(),
    });

    if (!settled) {
      return;
    }

    closeDetailsModal();
  }

  return (
    <>
      <SectionTitle title="Histórico" />

      <PremiumCard className="v7-card-fill" contentClassName="v7-card-content">
        <div className="v7-card-header">
          <SectionTitle title="Histórico financeiro" />
        </div>

        <div className="v7-card-header mt-3">
          <SearchField
            value={searchTerm}
            onChange={(value) => {
              setSearchTerm(value);
              setFinancePage(1);
            }}
            placeholder="Buscar veículo, placa, serviço ou pagamento..."
          />
        </div>

        <div className="v7-list-scroll premium-scroll mt-3 pr-1">
          {visibleFinanceEntries.length === 0 && (
            <EmptyState
              message="Nenhuma movimentação encontrada."
            />
          )}

          {visibleFinanceEntries.map((entry) => (
            <div
              key={entry.id}
              className="grid min-h-[76px] grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-4 border-b border-white/[0.06] py-3 last:border-b-0"
            >
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-white">
                  {entry.vehicle}
                </p>

                <p className="mt-1 truncate text-sm text-zinc-500">
                  {entry.plate} · {entry.service}
                </p>

                <p className="mt-1 truncate text-xs text-zinc-600">
                  {entry.method} ·{" "}
                  {entry.document} · {entry.time}
                </p>
              </div>

              <div className="text-right">
                <p className="text-sm font-semibold text-white">
                  {formatBrlCurrency(entry.amount)}
                </p>

                <p className="mt-1 text-xs text-zinc-600">
                  {entry.status === "paid" ? "Recebido" : "Em aberto"}
                </p>
              </div>

              <button
                type="button"
                onClick={() => {
                  setSelectedEntryId(entry.id);
                }}
                className="grid h-9 w-9 place-items-center rounded-[4px] border border-white/[0.08] text-zinc-500 transition hover:border-white/[0.16] hover:bg-white/[0.04] hover:text-white"
                aria-label={`Ver detalhes de ${entry.vehicle}`}
                title="Ver detalhes"
              >
                <Eye
                  className="h-4 w-4"
                  aria-hidden="true"
                />
              </button>
            </div>
          ))}
        </div>

        <div className="v7-card-footer pagination-stable mt-3 border-t border-white/[0.06] pt-3">
          <PaginationFooter
            page={safeFinancePage}
            totalPages={financeTotalPages}
            onPrevious={() =>
              setFinancePage((current) =>
                Math.max(1, current - 1)
              )
            }
            onNext={() =>
              setFinancePage((current) =>
                Math.min(financeTotalPages, current + 1)
              )
            }
          />
        </div>
      </PremiumCard>

      <AnimatedModal
        open={Boolean(selectedEntry)}
        onClose={closeDetailsModal}
        labelledBy="financial-details-title"
        backdropClassName="z-[320] p-4"
        panelClassName="max-h-[calc(100dvh-32px)] w-full max-w-[560px] overflow-y-auto rounded-[4px] border border-[var(--border-soft)] bg-[var(--surface-card)] p-5 shadow-[0_10px_34px_rgba(0,0,0,0.28)]"
      >
        {selectedEntry ? (
          <div>
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2
                  id="financial-details-title"
                  className="text-base font-semibold uppercase text-white"
                >
                  {selectedEntry.vehicle}
                </h2>

                <p className="mt-2 text-sm text-zinc-500">
                  Detalhes financeiros da ordem de serviço.
                </p>
              </div>

              <button
                type="button"
                onClick={closeDetailsModal}
                className="grid h-9 w-9 shrink-0 place-items-center rounded-[4px] border border-white/[0.08] text-zinc-500 transition hover:bg-white/[0.05] hover:text-white"
                aria-label="Fechar detalhes"
              >
                <X className="h-4 w-4" aria-hidden="true" />
              </button>
            </div>

            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <InfoField label="Placa" value={selectedEntry.plate} />

              <InfoField label="Serviço" value={selectedEntry.service} />

              <InfoField
                label="Valor"
                value={formatBrlCurrency(selectedEntry.amount)}
              />

              <InfoField
                label="Status"
                value={selectedEntry.status === "paid" ? "Recebido" : "Em aberto"}
              />

              <InfoField label="Pagamento" value={selectedEntry.method} />

              <InfoField label="Documento" value={selectedEntry.document} />
            </div>

            {selectedEntry.status === "pending" && (
              <div className="mt-5 grid gap-4 border-t border-white/[0.08] pt-5">
                <div className="grid gap-4 sm:grid-cols-2">
                  <DropdownField
                    label="Pagamento"
                    value={paymentMethod}
                    options={["Dinheiro", "Crédito", "Débito"]}
                    onChange={(value) =>
                      setPaymentMethod(value as LavaPaymentMethod)
                    }
                  />
                </div>

                <button
                  type="button"
                  className="service-action-button btn btn-primary btn-full"
                  onClick={confirmPayment}
                >
                  <Check className="h-3.5 w-3.5" aria-hidden="true" />
                  Confirmar pagamento
                </button>
              </div>
            )}

            {selectedEntry.status === "paid" && (
              <div className="mt-5 rounded-[4px] border border-white/[0.08] bg-white/[0.025] px-3 py-3 text-sm text-zinc-400">
                O pagamento desta ordem de serviço está concluído.
              </div>
            )}

            {financialReceipt ? (
              <div className="mt-5 flex flex-col-reverse gap-2 border-t border-white/[0.08] pt-4 sm:flex-row sm:justify-end">
                <button
                  type="button"
                  onClick={() => openReceiptModal(financialReceipt)}
                  className="service-action-button btn btn-secondary btn-compact"
                >
                  Ver comprovante
                </button>

                <button
                  type="button"
                  onClick={() => void downloadReceiptPdf(financialReceipt)}
                  className="service-action-button btn btn-primary btn-compact"
                >
                  Baixar
                </button>
              </div>
            ) : null}
          </div>
        ) : null}
      </AnimatedModal>

      <ReceiptPdfModal
        open={isReceiptModalOpen}
        title={financialReceipt?.title ?? "Comprovante"}
        previewUrl={receiptPreviewUrl}
        printHtml={receiptPrintHtml}
        onDownload={
          financialReceipt
            ? () => void downloadReceiptPdf(financialReceipt)
            : undefined
        }
        onClose={closeReceiptModal}
      />
    </>
  );
}

