import {
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  ContentStack,
  PremiumCard,
  SectionTitle,
} from "../../../components/ui";

import type {
  BarSaleHistoryEntry,
} from "../../../entities/sale-history";

import type {
  BarHistoryPageRequest,
  BarHistoryPageResult,
  LavaHistoryPageRequest,
  LavaHistoryPageResult,
} from "../../../data/contracts";

import {
  formatBrlCurrency,
} from "../../../shared/lib/currency";

import {
  UnifiedReportExportActions,
} from "../../reports/UnifiedReportExportActions";

import {
  getLatestRows,
} from "./barReports.selectors";

type BarReportsProps = {
  onLoadHistory: (
    input: BarHistoryPageRequest
  ) => Promise<BarHistoryPageResult | null>;
  onLoadLavaHistory: (
    input: LavaHistoryPageRequest
  ) => Promise<LavaHistoryPageResult | null>;
};

type ReportListRow = {
  label: string;
  value: string;
};

const BAR_REPORT_PAGE_SIZE = 100;

function formatQuantity(value: number) {
  return String(value).padStart(2, "0");
}

function getPaymentRows(
  entries: BarSaleHistoryEntry[]
): ReportListRow[] {
  const totals = entries.reduce<
    Record<string, number>
  >((accumulator, entry) => {
    accumulator[entry.method] =
      (accumulator[entry.method] ?? 0) +
      entry.amount;
    return accumulator;
  }, {});

  return Object.entries(totals)
    .sort(
      ([, firstAmount], [, secondAmount]) =>
        secondAmount - firstAmount
    )
    .map(([label, amount]) => ({
      label,
      value: formatBrlCurrency(amount),
    }));
}

function ReportRows({
  rows,
}: {
  rows: ReportListRow[];
}) {
  if (rows.length === 0) {
    return (
      <p className="py-3 text-sm text-[var(--text-subtle)]">
        Nenhuma venda concluída.
      </p>
    );
  }

  return (
    <div>
      {rows.map((row) => (
        <div
          key={row.label}
          className="flex items-center justify-between gap-3 border-b border-[var(--border-subtle)] py-2.5 last:border-b-0"
        >
          <p className="truncate text-sm font-medium text-[var(--text-base)]">
            {row.label}
          </p>
          <p className="shrink-0 text-sm text-[var(--text-muted)]">
            {row.value}
          </p>
        </div>
      ))}
    </div>
  );
}

export function BarReports({
  onLoadHistory,
  onLoadLavaHistory,
}: BarReportsProps) {
  const [entries, setEntries] =
    useState<BarSaleHistoryEntry[]>([]);
  const [historyLoadFailed, setHistoryLoadFailed] =
    useState(false);

  useEffect(() => {
    let isCurrent = true;

    async function loadReportHistory() {
      setHistoryLoadFailed(false);
      const firstPage = await onLoadHistory({
        page: 0,
        size: BAR_REPORT_PAGE_SIZE,
      });

      if (!isCurrent) {
        return;
      }

      if (!firstPage) {
        setEntries([]);
        setHistoryLoadFailed(true);
        return;
      }

      const allEntries = [...firstPage.entries];

      for (
        let page = 1;
        page < firstPage.totalPages;
        page += 1
      ) {
        const currentPage = await onLoadHistory({
          page,
          size: BAR_REPORT_PAGE_SIZE,
        });

        if (!isCurrent) {
          return;
        }

        if (!currentPage) {
          setEntries([]);
          setHistoryLoadFailed(true);
          return;
        }

        allEntries.push(...currentPage.entries);
      }

      setEntries(allEntries);
    }

    void loadReportHistory();
    return () => {
      isCurrent = false;
    };
  }, [onLoadHistory]);

  const reportData = useMemo(() => {
    const totalReceived = entries.reduce(
      (total, entry) => total + entry.amount,
      0
    );
    const averageTicket = entries.length > 0
      ? totalReceived / entries.length
      : 0;

    return {
      totalReceived,
      averageTicket,
      paymentRows: getPaymentRows(entries),
      latestRows: getLatestRows(entries),
    };
  }, [entries]);

  return (
    <ContentStack>
      <UnifiedReportExportActions
        onLoadBarHistory={onLoadHistory}
        onLoadLavaHistory={onLoadLavaHistory}
      />

      {historyLoadFailed ? (
        <p className="text-sm text-[var(--color-danger)]">
          Não foi possível carregar os dados completos do relatório.
        </p>
      ) : null}

      <PremiumCard>
        <div className="grid gap-3 xl:grid-cols-3">
          <div className="border-r border-[var(--border-subtle)] pr-4 last:border-r-0">
            <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
              Total recebido
            </p>
            <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
              {formatBrlCurrency(reportData.totalReceived)}
            </p>
          </div>
          <div className="border-r border-[var(--border-subtle)] pr-4 last:border-r-0">
            <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
              Comandas concluídas
            </p>
            <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
              {formatQuantity(entries.length)}
            </p>
          </div>
          <div className="pr-4">
            <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
              Ticket médio
            </p>
            <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
              {formatBrlCurrency(reportData.averageTicket)}
            </p>
          </div>
        </div>
      </PremiumCard>

      <div className="grid min-h-0 gap-3 xl:grid-cols-2">
        <PremiumCard className="v7-card-fill overflow-hidden">
          <div className="v7-card-content">
            <div className="v7-card-header">
              <SectionTitle compact title="Formas de pagamento" />
            </div>
            <div className="v7-list-scroll premium-scroll pr-1">
              <ReportRows rows={reportData.paymentRows} />
            </div>
          </div>
        </PremiumCard>

        <PremiumCard className="v7-card-fill overflow-hidden">
          <div className="v7-card-content">
            <div className="v7-card-header flex items-center justify-between gap-4">
              <SectionTitle compact title="Últimos registros" />
              <p className="text-sm text-[var(--text-subtle)]">
                {reportData.latestRows.length} registro(s)
              </p>
            </div>
            <div className="v7-list-scroll premium-scroll pr-1">
              {reportData.latestRows.length === 0 ? (
                <p className="py-3 text-sm text-[var(--text-subtle)]">
                  Nenhuma venda concluída.
                </p>
              ) : null}

              {reportData.latestRows.map((entry) => (
                <div
                  key={entry.id}
                  className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-3 border-b border-[var(--border-subtle)] py-3 last:border-b-0"
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-[var(--text-base)]">
                      {entry.origin}
                    </p>
                    <p className="mt-1 truncate text-xs text-[var(--text-subtle)]">
                      {entry.method} · {entry.document}
                    </p>
                  </div>
                  <p className="text-sm font-medium text-[var(--text-muted)]">
                    {formatBrlCurrency(entry.amount)}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </PremiumCard>
      </div>
    </ContentStack>
  );
}
