import {
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
  Button,
  ContentStack,
  EmptyState,
  InfoField,
  PaginationFooter,
  PremiumCard,
  ReceiptPdfModal,
  SearchField,
  useToast,
} from "../../../components/ui";

import type {
  BarSaleHistoryEntry,
} from "../../../entities/sale-history";

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
  useReceiptPrinter,
} from "../../../shared/receipts/useReceiptPrinter";

import {
  useAccessControl,
} from "../../access";

import {
  canMarkPaymentReversed,
  PaymentReversalModal,
} from "../../payment-terminal";

const BAR_HISTORY_PAGE_SIZE = 7;

type BarHistoryPageState = {
  entries: BarSaleHistoryEntry[];
  page: number;
  totalPages: number;
  totalElements: number;
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

export function BarSalesHistory({
  onLoadHistory,
}: {
  onLoadHistory: (input: {
    page: number;
    size: number;
    search?: string;
  }) => Promise<{
    entries: BarSaleHistoryEntry[];
    page: number;
    totalPages: number;
    totalElements: number;
  } | null>;
}) {
  const [searchTerm, setSearchTerm] = useState("");
  const [requestedPage, setRequestedPage] = useState(1);
  const [historyPageState, setHistoryPageState] =
    useState<BarHistoryPageState | null>(null);
  const {
    can,
  } = useAccessControl();
  const [
    reversalEntry,
    setReversalEntry,
  ] = useState<BarSaleHistoryEntry | null>(
    null
  );
  const [
    historyReloadKey,
    setHistoryReloadKey,
  ] = useState(0);
  const [selectedEntryId, setSelectedEntryId] = useState<number | null>(null);
  const latestHistoryRequestId = useRef(0);
  const onLoadHistoryRef = useRef(onLoadHistory);
  const receiptViewer = useReceiptViewer();
  const receiptPrinter = useReceiptPrinter();
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
      ),
    [
      visibleHistoryEntries,
      selectedEntryId,
    ]
  );

  const safeHistoryPage =
    historyPageState?.page ?? 1;
  const historyTotalPages =
    historyPageState?.totalPages ?? 0;
  useEffect(() => {
    onLoadHistoryRef.current = onLoadHistory;
  }, [
    onLoadHistory,
  ]);

  useEffect(() => {
    const requestId =
      latestHistoryRequestId.current + 1;
    latestHistoryRequestId.current = requestId;

    void onLoadHistoryRef.current({
      page: requestedPage - 1,
      size: BAR_HISTORY_PAGE_SIZE,
      search: searchTerm,
    }).then((result) => {
        if (
          !result ||
          latestHistoryRequestId.current !== requestId
        ) {
          return;
        }

        setHistoryPageState({
          entries: result.entries,
          page:
            result.totalPages === 0
              ? 1
              : Math.min(
                  result.page + 1,
                  result.totalPages
                ),
          totalPages: result.totalPages,
          totalElements: result.totalElements,
        });
      });
  }, [
    requestedPage,
    searchTerm,
    historyReloadKey,
  ]);

  function closeDetailsModal() {
    setSelectedEntryId(null);
  }

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
    canMarkPaymentReversed(
      selectedEntry,
      can("payment:reverse")
    );

  return (
    <ContentStack>
      <PremiumCard>
        <div className="flex flex-col">
          <div className="v7-card-header">
            <SearchField
              value={searchTerm}
              onChange={(value) => {
                setSearchTerm(value);
                setRequestedPage(1);
              }}
              placeholder="Buscar comanda, pagamento ou documento..."
            />
          </div>

          <div className="bar-history-list mt-3">
            {!historyPageState ? (
              <EmptyState
                message="Carregando histórico..."
              />
            ) : null}

            {historyPageState &&
              visibleHistoryEntries.length === 0 && (
              <EmptyState
                message="Nenhum registro encontrado."
              />
            )}

            {visibleHistoryEntries.map((entry) => (
              <div
                key={entry.id}
                className="bar-history-row grid min-h-[68px] grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-4 border-b border-[var(--border-subtle)] py-3 last:border-b-0"
              >
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-[var(--text-base)]">
                    {entry.origin}
                  </p>

                  <p className="mt-1 truncate text-sm text-[var(--text-muted)]">
                    {entry.description}
                  </p>

                  <p className="mt-1 truncate text-xs text-[var(--text-subtle)]">
                    {entry.method} · {entry.document}
                  </p>
                </div>

                <div className="text-right">
                  <p className="text-sm font-semibold text-[var(--text-base)]">
                    {formatBrlCurrency(entry.amount)}
                  </p>

                  <p className="mt-1 text-xs text-[var(--text-subtle)]">
                    {entry.time}
                  </p>
                </div>

                <Button
                  size="icon"
                  variant="ghost"
                  onClick={() => setSelectedEntryId(entry.id)}
                  leadingIcon={<Eye />}
                  aria-label={`Ver detalhes de ${entry.origin}`}
                  title="Ver detalhes"
                />
              </div>
            ))}
          </div>

          {historyPageState && historyTotalPages > 1 ? (
          <div className="v7-card-footer pagination-stable mt-3 border-t border-[var(--border-subtle)] pt-3">
            <PaginationFooter
              page={safeHistoryPage}
              totalPages={historyTotalPages}
              totalItems={historyPageState.totalElements}
              onPrevious={() =>
                setRequestedPage((current) =>
                  Math.max(1, current - 1)
                )
              }
              onNext={() =>
                setRequestedPage((current) =>
                  Math.min(historyTotalPages, current + 1)
                )
              }
            />
          </div>
          ) : null}
        </div>
      </PremiumCard>

      <AnimatedModal
        open={Boolean(selectedEntry)}
        onClose={closeDetailsModal}
        labelledBy="bar-sale-details-title"
        backdropClassName="z-[320] p-4"
        panelClassName="max-h-[calc(100dvh-32px)] w-full max-w-[560px] overflow-y-auto rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
      >
        {selectedEntry ? (
          <div>
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2
                  id="bar-sale-details-title"
                  className="text-[var(--font-size-section-title)] font-semibold text-[var(--text-base)]"
                >
                  {selectedEntry.origin}
                </h2>

                <p className="mt-2 text-sm text-[var(--text-muted)]">
                  Detalhes da venda concluída.
                </p>
              </div>

              <Button
                size="icon"
                variant="ghost"
                onClick={closeDetailsModal}
                leadingIcon={<X />}
                aria-label="Fechar detalhes"
                title="Fechar"
              />
            </div>

            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <InfoField
                label="Tipo"
                value="Comanda"
              />

              <InfoField
                label="Valor"
                value={formatBrlCurrency(selectedEntry.amount)}
              />

              <InfoField label="Pagamento" value={selectedEntry.method} />

              <InfoField label="Documento" value={selectedEntry.document} />

              <InfoField
                label="Status"
                value={selectedEntry.status ?? "Concluída"}
              />

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

              <InfoField label="Horário" value={selectedEntry.time} />
            </div>

            <div className="mt-4 rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] p-3">
              <p className="text-[10px] font-medium uppercase tracking-normal text-[var(--text-subtle)]">
                Itens
              </p>

              <p className="mt-2 text-sm leading-6 text-[var(--text-muted)]">
                {selectedEntry.description}
              </p>
            </div>

            {selectedEntry.checkoutId ||
            canReverseSelectedPayment ? (
              <div className="mt-5 flex flex-wrap justify-end gap-2 border-t border-[var(--border-subtle)] pt-4">
                {canReverseSelectedPayment ? (
                  <Button
                    size="compact"
                    variant="danger"
                    onClick={() => {
                      setReversalEntry(selectedEntry);
                      closeDetailsModal();
                    }}
                  >
                    Marcar como estornada
                  </Button>
                ) : null}

                {selectedEntry.checkoutId ? (
                  <Button
                    size="compact"
                    variant="secondary"
                    disabled={isLoadingReceipt}
                    onClick={() => void openSelectedReceipt()}
                  >
                    {isLoadingReceipt
                      ? "Carregando..."
                      : "Ver comprovante"}
                  </Button>
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
        isPrinting={receiptPrinter.isPrinting}
        onPrint={
          currentReceipt?.checkoutId
            ? () => void receiptPrinter.printReceipt(currentReceipt.checkoutId as string)
            : undefined
        }
        onDownload={
          currentReceipt
            ? () => void downloadReceiptPdf(currentReceipt)
            : undefined
        }
        onClose={receiptViewer.closeReceipt}
      />

      <PaymentReversalModal
        open={Boolean(reversalEntry)}
        paymentId={
          reversalEntry?.paymentId ?? null
        }
        operationLabel={
          reversalEntry?.origin ??
          "Pagamento"
        }
        amount={
          reversalEntry?.amount ?? 0
        }
        onClose={() =>
          setReversalEntry(null)
        }
        onFinished={() => {
          setHistoryReloadKey(
            (current) => current + 1
          );
        }}
      />
    </ContentStack>
  );
}

