import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import {
  Eye,
  X,
} from "lucide-react";

import {
  AnimatedModal,
  EmptyState,
  InfoField,
  PaginationFooter,
  PremiumCard,
  ReceiptPdfModal,
  SearchField,
  SectionTitle,
  useToast,
} from "../../../components/ui";

import type {
  LavaHistoryEntry,
} from "../../../entities/work-order";

import {
  formatBrlCurrency,
} from "../../../shared/lib/currency";
import {
  downloadReceiptPdf,
  type ReceiptDocument,
} from "../../../shared/receipts/receiptDocument";
import {
  loadOfficialReceipt,
} from "../../../shared/receipts/officialReceipt";
import {
  useReceiptViewer,
} from "../../../shared/receipts/useReceiptViewer";

import {
  useAccessControl,
} from "../../access";

import {
  canReverseTerminalPayment,
  TerminalPaymentReversalModal,
} from "../../payment-terminal";

export const LAVA_HISTORY_PAGE_SIZE = 7;

type LavaHistoryPageState = {
  entries: LavaHistoryEntry[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
  first: boolean;
  last: boolean;
};

function formatHistoryTime(value: string): string {
  return new Date(value).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function LavaHistoryPanel({
  onLoadHistory,
}: {
  entries: LavaHistoryEntry[];
  onLoadHistory: (input: {
    page: number;
    size: number;
    search?: string;
  }) => Promise<{
    entries: LavaHistoryEntry[];
    page: number;
    size: number;
    totalPages: number;
    totalElements: number;
    first: boolean;
    last: boolean;
  } | null>;
}) {
  const [searchTerm, setSearchTerm] = useState("");
  const [historyPageState, setHistoryPageState] =
    useState<LavaHistoryPageState | null>(null);
  const {
    can,
  } = useAccessControl();
  const [
    reversalEntry,
    setReversalEntry,
  ] = useState<LavaHistoryEntry | null>(
    null
  );
  const [selectedEntryId, setSelectedEntryId] =
    useState<number | null>(null);
  const latestHistoryRequestId = useRef(0);
  const onLoadHistoryRef = useRef(onLoadHistory);
  const receiptViewer = useReceiptViewer();
  const {
    showErrorToast,
  } = useToast();
  const [currentReceipt, setCurrentReceipt] =
    useState<ReceiptDocument | null>(null);
  const [isLoadingReceipt, setIsLoadingReceipt] =
    useState(false);
  const visibleHistoryEntries = useMemo(
    () => historyPageState?.entries ?? [],
    [historyPageState]
  );

  const selectedEntry = useMemo(
    () =>
      visibleHistoryEntries.find(
        (entry) => entry.id === selectedEntryId
      ) ?? null,
    [
      visibleHistoryEntries,
      selectedEntryId,
    ]
  );
  useEffect(() => {
    onLoadHistoryRef.current = onLoadHistory;
  }, [
    onLoadHistory,
  ]);

  const loadPage = useCallback(async (page: number) => {
    const requestedPage = Math.max(1, page);
    const requestId =
      latestHistoryRequestId.current + 1;
    latestHistoryRequestId.current = requestId;

    const result = await onLoadHistoryRef.current({
      page: requestedPage - 1,
      size: LAVA_HISTORY_PAGE_SIZE,
      search: searchTerm,
    });

    if (latestHistoryRequestId.current !== requestId) {
      return;
    }

    if (!result) {
      return;
    }

    const totalPages = result.totalPages;

    setHistoryPageState({
      entries: result.entries,
      page:
        totalPages === 0
          ? 1
          : Math.min(result.page + 1, totalPages),
      size: result.size,
      totalPages,
      totalElements: result.totalElements,
      first: result.first,
      last: result.last,
    });
  }, [
    searchTerm,
  ]);

  useEffect(() => {
    void loadPage(1);
  }, [
    loadPage,
  ]);

  async function openSelectedReceipt() {
    if (
      !selectedEntry?.checkoutId ||
      isLoadingReceipt
    ) {
      return;
    }

    setIsLoadingReceipt(true);

    try {
      const receipt = await loadOfficialReceipt(
        selectedEntry.checkoutId
      );

      setCurrentReceipt(receipt);
      receiptViewer.openReceipt(receipt);
    } catch (error) {
      showErrorToast(error, {
        title:
          "Não foi possível carregar o comprovante",
      });
    } finally {
      setIsLoadingReceipt(false);
    }
  }

  const canReverseSelectedPayment =
    canReverseTerminalPayment(
      selectedEntry,
      can("payment:reverse")
    );

  return (
    <>
      <SectionTitle title="Histórico" />

      <PremiumCard className="v7-card-fill" contentClassName="v7-card-content">
        <div className="v7-card-header">
          <SectionTitle title="OS concluídas" />
        </div>

        <div className="v7-card-header mt-3 gap-2">
          <SearchField
            value={searchTerm}
            onChange={(value) => {
              setSearchTerm(value);
            }}
            placeholder="Buscar cliente, veículo, placa, serviço ou pagamento..."
          />
        </div>

        <div className="v7-list-scroll premium-scroll mt-3 pr-1">
          {!historyPageState ? (
            <EmptyState message="Carregando histórico..." />
          ) : null}

          {historyPageState &&
          visibleHistoryEntries.length === 0 ? (
            <EmptyState message="Nenhuma OS concluída encontrada." />
          ) : null}

          {visibleHistoryEntries.map((entry) => (
            <div
              key={entry.id}
              className="grid min-h-[76px] grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-4 border-b border-white/[0.06] py-3 last:border-b-0"
            >
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-white">
                  OS {entry.id} · {entry.clientName}
                </p>

                <p className="mt-1 truncate text-sm text-zinc-500">
                  {entry.vehicle} · {entry.plate} · {entry.service}
                </p>

                <p className="mt-1 truncate text-xs text-zinc-600">
                  {entry.time} · {entry.method} · {entry.status}
                </p>
              </div>

              <div className="text-right">
                <p className="text-sm font-semibold text-white">
                  {formatBrlCurrency(entry.amount)}
                </p>

                <p className="mt-1 text-xs text-zinc-600">
                  {entry.document}
                </p>
              </div>

              <button
                type="button"
                onClick={() =>
                  setSelectedEntryId(entry.id)
                }
                className="grid h-9 w-9 place-items-center rounded-[4px] border border-white/[0.08] text-zinc-500 transition hover:border-white/[0.16] hover:bg-white/[0.04] hover:text-white"
                aria-label={`Ver detalhes da OS ${entry.id}`}
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

        {historyPageState && historyPageState.totalPages > 1 ? (
        <div className="v7-card-footer pagination-stable mt-3 border-t border-white/[0.06] pt-3">
          <PaginationFooter
            page={historyPageState.page}
            totalPages={historyPageState.totalPages}
            totalItems={historyPageState.totalElements}
            first={historyPageState.first}
            last={historyPageState.last}
            onPrevious={() =>
              void loadPage(
                historyPageState.page - 1
              )
            }
            onNext={() =>
              void loadPage(
                historyPageState.page + 1
              )
            }
          />
        </div>
        ) : null}
      </PremiumCard>

      <AnimatedModal
        open={Boolean(selectedEntry)}
        onClose={() => setSelectedEntryId(null)}
        labelledBy="lava-history-details-title"
        backdropClassName="z-[320] p-4"
        panelClassName="max-h-[calc(100dvh-32px)] w-full max-w-[560px] overflow-y-auto rounded-[4px] border border-[var(--border-soft)] bg-[var(--surface-card)] p-5 shadow-[0_10px_34px_rgba(0,0,0,0.28)]"
      >
        {selectedEntry ? (
          <div>
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2
                  id="lava-history-details-title"
                  className="text-base font-semibold uppercase text-white"
                >
                  OS {selectedEntry.id}
                </h2>

                <p className="mt-2 text-sm text-zinc-500">
                  {selectedEntry.clientName} · {selectedEntry.time}
                </p>
              </div>

              <button
                type="button"
                onClick={() => setSelectedEntryId(null)}
                className="grid h-9 w-9 shrink-0 place-items-center rounded-[4px] border border-white/[0.08] text-zinc-500 transition hover:bg-white/[0.05] hover:text-white"
                aria-label="Fechar detalhes"
              >
                <X className="h-4 w-4" aria-hidden="true" />
              </button>
            </div>

            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <InfoField label="Cliente" value={selectedEntry.clientName} />
              <InfoField label="Telefone" value={selectedEntry.clientPhone || "Não informado"} />
              <InfoField label="Veículo" value={selectedEntry.vehicle} />
              <InfoField label="Placa" value={selectedEntry.plate} />
              <InfoField label="Serviços" value={selectedEntry.service} />
              <InfoField label="Valor" value={formatBrlCurrency(selectedEntry.amount)} />
              <InfoField label="Pagamento" value={selectedEntry.method} />
              <InfoField label="Documento" value={selectedEntry.document} />
              <InfoField label="Status" value={selectedEntry.status} />

              {selectedEntry.paymentReversedAt ? (
                <InfoField
                  label="Estornado em"
                  value={formatHistoryTime(
                    selectedEntry.paymentReversedAt
                  )}
                />
              ) : null}

              {selectedEntry.paymentReversalReason ? (
                <InfoField
                  label="Motivo do estorno"
                  value={
                    selectedEntry.paymentReversalReason
                  }
                />
              ) : null}
            </div>

            {selectedEntry.checkoutId ||
            canReverseSelectedPayment ? (
              <div className="mt-5 flex flex-wrap justify-end gap-2 border-t border-white/[0.08] pt-4">
                {canReverseSelectedPayment ? (
                  <button
                    type="button"
                    onClick={() => {
                      setReversalEntry(
                        selectedEntry
                      );

                      setSelectedEntryId(null);
                    }}
                    className="btn btn-secondary btn-compact"
                  >
                    Estornar pagamento
                  </button>
                ) : null}

                {selectedEntry.checkoutId ? (
                  <button
                    type="button"
                    onClick={() =>
                      void openSelectedReceipt()
                    }
                    disabled={isLoadingReceipt}
                    className="service-action-button btn btn-secondary btn-compact disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {isLoadingReceipt
                      ? "Carregando..."
                      : "Ver comprovante"}
                  </button>
                ) : null}
              </div>
            ) : null}
          </div>
        ) : null}
      </AnimatedModal>

      <ReceiptPdfModal
        open={receiptViewer.isOpen}
        title={currentReceipt?.title ?? "Comprovante"}
        previewUrl={receiptViewer.previewUrl}
        printHtml={receiptViewer.printHtml}
        onDownload={
          currentReceipt
            ? () => void downloadReceiptPdf(currentReceipt)
            : undefined
        }
        onClose={receiptViewer.closeReceipt}
      />

      <TerminalPaymentReversalModal
        open={Boolean(reversalEntry)}
        paymentId={
          reversalEntry?.paymentId ?? null
        }
        operationLabel={
          reversalEntry
            ? `OS ${reversalEntry.id}`
            : "Pagamento"
        }
        amount={
          reversalEntry?.amount ?? 0
        }
        onClose={() =>
          setReversalEntry(null)
        }
        onFinished={() => {
          void loadPage(
            historyPageState?.page ?? 1
          );
        }}
      />
    </>
  );
}
