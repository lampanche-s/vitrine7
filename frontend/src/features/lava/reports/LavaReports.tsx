import {
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  Download,
} from "lucide-react";

import {
  ContentStack,
  EmptyState,
  PremiumCard,
  SectionTitle,
} from "../../../components/ui";

import type {
  LavaFinanceBreakdown,
} from "../../../data/contracts";

import type {
  LavaHistoryEntry,
} from "../../../entities/work-order";

import {
  formatBrlCurrency,
} from "../../../shared/lib/currency";

import {
  exportReportWorkbook,
  type ReportTable,
} from "../../../shared/exporters/reportWorkbookExporter";

import {
  useLavaDomain,
} from "../state";

const LAVA_REPORT_HISTORY_LIMIT = 6;
const LAVA_REPORT_HISTORY_PAGE_SIZE = 100;

function centsToBrl(cents: number) {
  return formatBrlCurrency(cents / 100);
}

function paymentMethodLabel(method: string) {
  if (method === "PIX") {
    return "Pix";
  }

  if (method === "CREDIT") {
    return "Crédito";
  }

  if (method === "DEBIT") {
    return "Débito";
  }

  if (method === "CASH") {
    return "Dinheiro";
  }

  return method;
}

function historyMethodKey(method: LavaHistoryEntry["method"]) {
  if (method === "Pix") {
    return "PIX";
  }

  if (method === "Crédito" || method === "Cartão") {
    return "CREDIT";
  }

  if (method === "Débito") {
    return "DEBIT";
  }

  return "CASH";
}

function operationCode(workOrderId: number) {
  return `OS-${String(workOrderId).padStart(3, "0")}`;
}

function aggregateServiceRows(
  entries: LavaHistoryEntry[]
) {
  const serviceCounts = new Map<string, number>();

  entries.forEach((entry) => {
    const services =
      entry.receiptItems && entry.receiptItems.length > 0
        ? entry.receiptItems.map((item) => item.name)
        : [entry.service];

    services.forEach((service) => {
      const label = service.trim();

      if (!label) {
        return;
      }

      serviceCounts.set(
        label,
        (serviceCounts.get(label) ?? 0) + 1
      );
    });
  });

  return Array.from(serviceCounts.entries())
    .map(([label, count]) => ({
      label,
      value: String(count).padStart(2, "0"),
      count,
    }))
    .sort(
      (first, second) =>
        second.count - first.count ||
        first.label.localeCompare(second.label)
    );
}

function createReportDataFromHistory(
  entries: LavaHistoryEntry[]
) {
  const totalRevenueCents = entries.reduce(
    (total, entry) =>
      total + Math.round(entry.amount * 100),
    0
  );
  const methods = new Map<
    string,
    { revenueCents: number; paymentCount: number }
  >();

  entries.forEach((entry) => {
    const key = historyMethodKey(entry.method);
    const current = methods.get(key) ?? {
      revenueCents: 0,
      paymentCount: 0,
    };

    current.revenueCents += Math.round(
      entry.amount * 100
    );
    current.paymentCount += 1;
    methods.set(key, current);
  });

  return {
    totalRevenueCents,
    operationCount: entries.length,
    averageTicketCents:
      entries.length === 0
        ? 0
        : Math.floor(totalRevenueCents / entries.length),
    byPaymentMethod: Array.from(methods.entries()).map(
      ([key, value]): LavaFinanceBreakdown => ({
        key,
        revenueCents: value.revenueCents,
        paymentCount: value.paymentCount,
        revenueBasisPoints:
          totalRevenueCents === 0
            ? 0
            : Math.floor(
                (value.revenueCents * 10_000) /
                  totalRevenueCents
              ),
      })
    ),
  };
}

export function LavaReports() {
  const [isExporting, setIsExporting] = useState(false);
  const {
    listHistory,
  } = useLavaDomain();
  const [historyData, setHistoryData] = useState<
    LavaHistoryEntry[] | null
  >(null);
  const [isLoadingHistory, setIsLoadingHistory] =
    useState(true);

  useEffect(() => {
    let isMounted = true;

    async function loadHistory() {
      const firstPage = await listHistory({
        page: 0,
        size: LAVA_REPORT_HISTORY_PAGE_SIZE,
      });

      if (firstPage === null) {
        return null;
      }

      const entries = [...firstPage.entries];
      let nextPage = firstPage.page + 1;

      while (nextPage < firstPage.totalPages) {
        const page = await listHistory({
          page: nextPage,
          size: LAVA_REPORT_HISTORY_PAGE_SIZE,
        });

        if (page === null) {
          return entries;
        }

        entries.push(...page.entries);
        nextPage = page.page + 1;
      }

      return entries;
    }

    void loadHistory()
      .then((historyResult) => {
        if (!isMounted) {
          return;
        }

        setHistoryData(historyResult ?? []);
      })
      .finally(() => {
        if (isMounted) {
          setIsLoadingHistory(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [listHistory]);

  const reportData = useMemo(
    () => createReportDataFromHistory(historyData ?? []),
    [historyData]
  );

  const financialRows = useMemo(() => {
    if (historyData === null) {
      return [
        "Recebido",
        "Operações",
        "Ticket médio",
        "Métodos",
      ].map((label) => ({
        label,
        value: isLoadingHistory
          ? "..."
          : label === "Recebido" ||
              label === "Ticket médio"
            ? "R$ 0,00"
            : "0",
      }));
    }

    return [
      {
        label: "Recebido",
        value: centsToBrl(
          reportData.totalRevenueCents
        ),
      },
      {
        label: "Operações",
        value: String(
          reportData.operationCount
        ).padStart(2, "0"),
      },
      {
        label: "Ticket médio",
        value: centsToBrl(
          reportData.averageTicketCents
        ),
      },
      {
        label: "Métodos",
        value: String(
          reportData.byPaymentMethod.length
        ).padStart(2, "0"),
      },
    ];
  }, [
    historyData,
    isLoadingHistory,
    reportData,
  ]);

  const paymentRows = useMemo(() => {
    return reportData.byPaymentMethod.map(
      (item) => ({
        label: paymentMethodLabel(item.key),
        value: centsToBrl(item.revenueCents),
      })
    );
  }, [reportData]);

  const receivedTotal =
    historyData === null
      ? null
      : reportData.totalRevenueCents;
  const serviceRows = useMemo(
    () => aggregateServiceRows(historyData ?? []),
    [historyData]
  );
  const latestRows = useMemo(
    () =>
      (historyData ?? []).slice(
        0,
        LAVA_REPORT_HISTORY_LIMIT
      ),
    [historyData]
  );

  async function handleExportLavaReport() {
    if (isExporting) {
      return;
    }

    setIsExporting(true);

    try {
      const generatedAt = new Date();

      const tables: ReportTable[] = [
        {
          sheetName: "Financeiro",
          title: "Financeiro do Lava Jato",
          columns: [
            { key: "order", header: "OS", width: 14 },
            { key: "vehicle", header: "Veículo", width: 24 },
            { key: "plate", header: "Placa", width: 14 },
            { key: "service", header: "Serviço", width: 28 },
            { key: "payment", header: "Pagamento", width: 18 },
            { key: "document", header: "Documento", width: 18 },
            { key: "completedAt", header: "Conclusão", width: 22 },
            {
              key: "amount",
              header: "Valor",
              width: 16,
              type: "currency",
            },
          ],
          rows: [
            ...(historyData ?? []).map((entry) => ({
              order: operationCode(entry.id),
              vehicle: entry.vehicle,
              plate: entry.plate,
              service: entry.service,
              payment: entry.method,
              document: entry.document,
              completedAt: entry.completedAt,
              amount: entry.amount,
            })),
          ],
        },
        {
          sheetName: "Operacional",
          title: "Serviços realizados",
          columns: [
            { key: "vehicle", header: "Veículo", width: 24 },
            { key: "plate", header: "Placa", width: 14 },
            { key: "service", header: "Serviço", width: 28 },
            { key: "completedAt", header: "Conclusão", width: 22 },
          ],
          rows: (historyData ?? []).map((entry) => ({
            vehicle: entry.vehicle,
            plate: entry.plate,
            service: entry.service,
            completedAt: entry.completedAt,
          })),
        },
      ];

      await exportReportWorkbook({
        fileName: "relatorio-lava-jato",
        title: "Relatório Lava Jato",
        subtitle: "Dados financeiros e operacionais",
        generatedAt,
        metrics: [
          {
            label: "Total recebido",
            value:
              receivedTotal === null
                ? "R$ 0,00"
                : centsToBrl(receivedTotal),
          },
          {
            label: "Operações",
            value: String(
              reportData.operationCount
            ).padStart(2, "0"),
          },
          {
            label: "Métodos",
            value: String(
              reportData.byPaymentMethod.length
            ).padStart(2, "0"),
          },
          {
            label: "Ticket médio",
            value: centsToBrl(
              reportData.averageTicketCents
            ),
          },
        ],
        tables,
      });
    } finally {
      setIsExporting(false);
    }
  }

  return (
    <ContentStack>
      <div className="v7-card-header flex items-center justify-between gap-4">
        <SectionTitle title="Relatórios do Lava Jato" />

        <button
          type="button"
          onClick={() => void handleExportLavaReport()}
          disabled={isExporting}
          className="service-action-button btn btn-primary btn-compact disabled:cursor-not-allowed disabled:opacity-50"
        >
          <Download
            className="h-4 w-4"
            aria-hidden="true"
          />
          {isExporting ? "Exportando..." : "Exportar XLSX"}
        </button>
      </div>

      <div className="grid min-h-0 flex-1 gap-3 xl:grid-rows-[auto_1fr]">
        <PremiumCard className="shrink-0">
          <div className="grid gap-3 xl:grid-cols-4">
            {financialRows.map((item) => (
              <div
                key={item.label}
                className="border-r border-white/[0.08] pr-4 last:border-r-0"
              >
                <p className="text-[11px] font-medium uppercase text-zinc-600">
                  {item.label}
                </p>

                <p className="mt-1 text-xl font-semibold text-white">
                  {item.value}
                </p>
              </div>
            ))}
          </div>
        </PremiumCard>

        <div className="grid min-h-0 gap-3 xl:grid-cols-[0.82fr_0.82fr_1.18fr]">
          <PremiumCard className="v7-card-fill overflow-hidden">
            <div className="v7-card-content">
              <div className="v7-card-header">
                <SectionTitle title="Financeiro" />
              </div>

              <div className="v7-list-scroll premium-scroll mt-3 pr-1">
                <p className="text-[11px] font-medium uppercase text-zinc-600">
                  Formas de pagamento
                </p>

                <div className="mt-2">
                  {isLoadingHistory ? (
                    <p className="py-3 text-sm text-zinc-500">
                      Carregando dados...
                    </p>
                  ) : null}

                  {!isLoadingHistory &&
                    paymentRows.length === 0 && (
                    <EmptyState message="Nenhum pagamento aprovado no período." />
                  )}

                  {!isLoadingHistory && paymentRows.map((item) => (
                    <div
                      key={item.label}
                      className="flex items-center justify-between border-b border-white/[0.06] py-2.5 last:border-b-0"
                    >
                      <p className="text-sm font-medium text-white">
                        {item.label}
                      </p>

                      <p className="text-sm text-zinc-500">
                        {item.value}
                      </p>
                    </div>
                  ))}
                </div>

                <div className="mt-4 border-t border-white/[0.08] pt-3">
                  <p className="text-[11px] font-medium uppercase text-zinc-600">
                    Resumo financeiro
                  </p>

                  <div className="mt-2">
                    <div className="flex items-center justify-between border-b border-white/[0.06] py-2.5">
                      <p className="text-sm font-medium text-white">
                        Recebido
                      </p>

                      <p className="text-sm text-zinc-500">
                        {receivedTotal === null
                          ? "R$ 0,00"
                          : centsToBrl(receivedTotal)}
                      </p>
                    </div>

                    <div className="flex items-center justify-between py-2.5">
                      <p className="text-sm font-medium text-white">
                        Operações
                      </p>

                      <p className="text-sm text-zinc-500">
                        {historyData === null
                          ? 0
                          : reportData.operationCount}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </PremiumCard>

          <PremiumCard className="v7-card-fill overflow-hidden">
            <div className="v7-card-content">
              <div className="v7-card-header">
                <SectionTitle title="Serviços" />
              </div>

              <div className="v7-list-scroll premium-scroll mt-3 pr-1">
                <p className="text-[11px] font-medium uppercase text-zinc-600">
                  Serviços realizados
                </p>

                <div className="mt-2">
                  {isLoadingHistory ? (
                    <p className="py-3 text-sm text-zinc-500">
                      Carregando dados...
                    </p>
                  ) : null}

                  {!isLoadingHistory &&
                    serviceRows.length === 0 && (
                    <EmptyState message="Nenhuma OS concluída no período." />
                  )}

                  {!isLoadingHistory && serviceRows.map((item) => (
                    <div
                      key={item.label}
                      className="flex items-center justify-between border-b border-white/[0.06] py-2.5 last:border-b-0"
                    >
                      <p className="truncate text-sm font-medium text-white">
                        {item.label}
                      </p>

                      <p className="text-sm text-zinc-500">
                        {item.value}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </PremiumCard>

          <PremiumCard className="v7-card-fill overflow-hidden">
            <div className="v7-card-content">
              <div className="v7-card-header flex items-center justify-between gap-4">
                <SectionTitle title="Últimos registros" />

                <p className="text-sm text-zinc-600">
                  {Math.min(
                    latestRows.length,
                    LAVA_REPORT_HISTORY_LIMIT
                  )} registros
                </p>
              </div>

              <div className="v7-list-scroll premium-scroll mt-3 pr-1">
                {!isLoadingHistory &&
                  latestRows.length === 0 && (
                  <EmptyState message="Nenhum registro concluído no período." />
                )}

                  {latestRows
                    .map((entry) => ({
                      code: operationCode(entry.id),
                      title: entry.vehicle || entry.clientName,
                      meta: `${entry.plate} · ${entry.service} · ${entry.method}`,
                      amount: centsToBrl(
                        Math.round(entry.amount * 100)
                      ),
                    }))
                    .slice(0, LAVA_REPORT_HISTORY_LIMIT)
                    .map((item) => (
                  <div
                    key={item.code}
                    className="grid grid-cols-[76px_minmax(0,1fr)_auto] items-center gap-3 border-b border-white/[0.06] py-3 last:border-b-0"
                  >
                    <p className="text-sm font-semibold text-zinc-500">
                      {item.code}
                    </p>

                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-white">
                        {item.title}
                      </p>

                      <p className="mt-1 truncate text-xs text-zinc-600">
                        {item.meta}
                      </p>
                    </div>

                    <p className="text-sm font-medium text-zinc-400">
                      {item.amount}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </PremiumCard>
        </div>
      </div>
    </ContentStack>
  );
}
