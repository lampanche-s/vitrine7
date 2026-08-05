import {
  useMemo,
  useState,
} from "react";

import {
  Download,
} from "lucide-react";

import {
  Button,
  PageActions,
} from "../../components/ui";

import type {
  BarHistoryPageRequest,
  BarHistoryPageResult,
  LavaHistoryPageRequest,
  LavaHistoryPageResult,
} from "../../data/contracts";

import type {
  BarSaleHistoryEntry,
} from "../../entities/sale-history";

import type {
  LavaHistoryEntry,
} from "../../entities/work-order";

import {
  exportReportWorkbook,
  type ReportTable,
} from "../../shared/exporters/reportWorkbookExporter";

import {
  formatBrlCurrency,
} from "../../shared/lib/currency";

import {
  ReportExportModal,
} from "./ReportExportModal";

import {
  getDefaultCustomReportDates,
  resolveReportDateRange,
  type ReportDateRange,
  type ReportPeriodPreset,
  type ReportScope,
} from "./report-period";

const REPORT_PAGE_SIZE = 100;

type UnifiedReportExportActionsProps = {
  onLoadBarHistory: (
    input: BarHistoryPageRequest
  ) => Promise<
    BarHistoryPageResult | null
  >;

  onLoadLavaHistory: (
    input: LavaHistoryPageRequest
  ) => Promise<
    LavaHistoryPageResult | null
  >;
};

function formatQuantity(
  value: number
) {
  return String(value).padStart(
    2,
    "0"
  );
}

function formatDateTime(
  value?: string | null
) {
  if (!value) {
    return "—";
  }

  const parsed =
    new Date(value);

  if (
    Number.isNaN(
      parsed.getTime()
    )
  ) {
    return value;
  }

  return parsed.toLocaleString(
    "pt-BR"
  );
}

function operationCode(
  workOrderId: number
) {
  return `OS-${String(
    workOrderId
  ).padStart(3, "0")}`;
}

function reportErrorMessage(
  error: unknown
) {
  if (
    error instanceof Error &&
    error.message.trim()
  ) {
    return error.message;
  }

  return "Não foi possível gerar o relatório selecionado.";
}

export function UnifiedReportExportActions({
  onLoadBarHistory,
  onLoadLavaHistory,
}: UnifiedReportExportActionsProps) {
  const [
    exportScope,
    setExportScope,
  ] = useState<ReportScope | null>(
    null
  );

  const [
    exportPeriod,
    setExportPeriod,
  ] =
    useState<ReportPeriodPreset>(
      "currentMonth"
    );

  const initialCustomDates =
    useMemo(
      () =>
        getDefaultCustomReportDates(),
      []
    );

  const [
    customFrom,
    setCustomFrom,
  ] = useState(
    initialCustomDates.from
  );

  const [
    customTo,
    setCustomTo,
  ] = useState(
    initialCustomDates.to
  );

  const [
    exportError,
    setExportError,
  ] = useState("");

  const [
    isExporting,
    setIsExporting,
  ] = useState(false);

  function openExportModal(
    scope: ReportScope
  ) {
    const dates =
      getDefaultCustomReportDates();

    setExportScope(scope);
    setExportPeriod(
      "currentMonth"
    );
    setCustomFrom(dates.from);
    setCustomTo(dates.to);
    setExportError("");
  }

  function closeExportModal() {
    if (isExporting) {
      return;
    }

    setExportScope(null);
    setExportError("");
  }

  async function loadAllBarHistory(
    range: ReportDateRange
  ) {
    const firstPage =
      await onLoadBarHistory({
        page: 0,
        size: REPORT_PAGE_SIZE,
        from: range.from,
        to: range.to,
      });

    if (!firstPage) {
      throw new Error(
        "Não foi possível carregar as comandas do período."
      );
    }

    const result = [
      ...firstPage.entries,
    ];

    for (
      let page = 1;
      page <
      firstPage.totalPages;
      page += 1
    ) {
      const currentPage =
        await onLoadBarHistory({
          page,
          size: REPORT_PAGE_SIZE,
          from: range.from,
          to: range.to,
        });

      if (!currentPage) {
        throw new Error(
          "Não foi possível carregar todas as comandas do período."
        );
      }

      result.push(
        ...currentPage.entries
      );
    }

    return result;
  }

  async function loadAllLavaHistory(
    range: ReportDateRange
  ) {
    const firstPage =
      await onLoadLavaHistory({
        page: 0,
        size: REPORT_PAGE_SIZE,
        from: range.from,
        to: range.to,
      });

    if (!firstPage) {
      throw new Error(
        "Não foi possível carregar as ordens de serviço do período."
      );
    }

    const result = [
      ...firstPage.entries,
    ];

    for (
      let page = 1;
      page <
      firstPage.totalPages;
      page += 1
    ) {
      const currentPage =
        await onLoadLavaHistory({
          page,
          size: REPORT_PAGE_SIZE,
          from: range.from,
          to: range.to,
        });

      if (!currentPage) {
        throw new Error(
          "Não foi possível carregar todas as ordens de serviço do período."
        );
      }

      result.push(
        ...currentPage.entries
      );
    }

    return result;
  }

  async function exportBarReport(
    range: ReportDateRange,
    entries: BarSaleHistoryEntry[]
  ) {
    const totalReceived =
      entries.reduce(
        (total, entry) =>
          total + entry.amount,
        0
      );

    const averageTicket =
      entries.length > 0
        ? totalReceived /
          entries.length
        : 0;

    const paymentMethodCount =
      new Set(
        entries.map(
          (entry) =>
            entry.method
        )
      ).size;

    const itemRows =
      entries.flatMap(
        (entry) =>
          (
            entry.receiptItems ??
            []
          ).map((item) => ({
            command:
              entry.origin,

            item:
              item.name,

            quantity:
              item.quantity,

            unitPrice:
              item.unitPrice,

            total:
              item.total,
          }))
      );

    const tables: ReportTable[] =
      [
        {
          sheetName:
            "Comandas",

          title:
            `Comandas concluídas — ${range.label}`,

          columns: [
            {
              key: "command",
              header: "Comanda",
              width: 26,
            },
            {
              key: "payment",
              header: "Pagamento",
              width: 18,
            },
            {
              key: "document",
              header: "Comprovante",
              width: 20,
            },
            {
              key: "completedAt",
              header: "Conclusão",
              width: 22,
            },
            {
              key: "items",
              header:
                "Itens e serviços",
              width: 44,
            },
            {
              key: "amount",
              header: "Valor",
              width: 16,
              type: "currency",
            },
          ],

          rows:
            entries.map(
              (entry) => ({
                command:
                  entry.origin,

                payment:
                  entry.method,

                document:
                  entry.document,

                completedAt:
                  formatDateTime(
                    entry.completedAt
                  ),

                items:
                  entry.description,

                amount:
                  entry.amount,
              })
            ),
        },
        {
          sheetName:
            "Itens e serviços",

          title:
            `Itens e serviços vendidos — ${range.label}`,

          columns: [
            {
              key: "command",
              header: "Comanda",
              width: 26,
            },
            {
              key: "item",
              header:
                "Item ou serviço",
              width: 32,
            },
            {
              key: "quantity",
              header: "Quantidade",
              width: 14,
              type: "number",
            },
            {
              key: "unitPrice",
              header:
                "Valor unitário",
              width: 18,
              type: "currency",
            },
            {
              key: "total",
              header: "Total",
              width: 16,
              type: "currency",
            },
          ],

          rows: itemRows,
        },
      ];

    await exportReportWorkbook({
      fileName:
        `relatorio-espeto-bar-${range.fileSuffix}`,

      title:
        "Relatório Espeto Bar",

      subtitle:
        `Período: ${range.label}`,

      generatedAt:
        new Date(),

      metrics: [
        {
          label:
            "Total recebido",

          value:
            formatBrlCurrency(
              totalReceived
            ),
        },
        {
          label:
            "Vendas concluídas",

          value:
            formatQuantity(
              entries.length
            ),
        },
        {
          label:
            "Ticket médio",

          value:
            formatBrlCurrency(
              averageTicket
            ),
        },
        {
          label:
            "Formas de pagamento",

          value:
            formatQuantity(
              paymentMethodCount
            ),
        },
      ],

      tables,
    });
  }

  async function exportLavaReport(
    range: ReportDateRange,
    entries: LavaHistoryEntry[]
  ) {
    const totalReceived =
      entries.reduce(
        (total, entry) =>
          total + entry.amount,
        0
      );

    const averageTicket =
      entries.length > 0
        ? totalReceived /
          entries.length
        : 0;

    const paymentMethodCount =
      new Set(
        entries.map(
          (entry) =>
            entry.method
        )
      ).size;

    const serviceRows =
      entries.flatMap(
        (entry) => {
          const items =
            entry.receiptItems
              ?.length
              ? entry.receiptItems
              : [
                  {
                    quantity: 1,
                    name:
                      entry.service,
                    unitPrice:
                      entry.amount,
                    total:
                      entry.amount,
                  },
                ];

          return items.map(
            (item) => ({
              order:
                operationCode(
                  entry.id
                ),

              client:
                entry.clientName,

              vehicle:
                entry.vehicle,

              plate:
                entry.plate,

              service:
                item.name,

              quantity:
                item.quantity,

              total:
                item.total,
            })
          );
        }
      );

    const tables: ReportTable[] =
      [
        {
          sheetName:
            "Ordens de serviço",

          title:
            `Ordens de serviço concluídas — ${range.label}`,

          columns: [
            {
              key: "order",
              header: "OS",
              width: 14,
            },
            {
              key: "client",
              header: "Cliente",
              width: 28,
            },
            {
              key: "phone",
              header: "Telefone",
              width: 18,
            },
            {
              key: "vehicle",
              header: "Veículo",
              width: 24,
            },
            {
              key: "plate",
              header: "Placa",
              width: 14,
            },
            {
              key: "service",
              header: "Serviço",
              width: 30,
            },
            {
              key: "payment",
              header: "Pagamento",
              width: 18,
            },
            {
              key: "completedAt",
              header: "Conclusão",
              width: 22,
            },
            {
              key: "amount",
              header: "Valor",
              width: 16,
              type: "currency",
            },
          ],

          rows:
            entries.map(
              (entry) => ({
                order:
                  operationCode(
                    entry.id
                  ),

                client:
                  entry.clientName,

                phone:
                  entry.clientPhone,

                vehicle:
                  entry.vehicle,

                plate:
                  entry.plate,

                service:
                  entry.service,

                payment:
                  entry.method,

                completedAt:
                  formatDateTime(
                    entry.completedAt
                  ),

                amount:
                  entry.amount,
              })
            ),
        },
        {
          sheetName:
            "Serviços realizados",

          title:
            `Serviços realizados — ${range.label}`,

          columns: [
            {
              key: "order",
              header: "OS",
              width: 14,
            },
            {
              key: "client",
              header: "Cliente",
              width: 28,
            },
            {
              key: "vehicle",
              header: "Veículo",
              width: 24,
            },
            {
              key: "plate",
              header: "Placa",
              width: 14,
            },
            {
              key: "service",
              header: "Serviço",
              width: 30,
            },
            {
              key: "quantity",
              header: "Quantidade",
              width: 14,
              type: "number",
            },
            {
              key: "total",
              header: "Total",
              width: 16,
              type: "currency",
            },
          ],

          rows:
            serviceRows,
        },
      ];

    await exportReportWorkbook({
      fileName:
        `relatorio-lava-jato-${range.fileSuffix}`,

      title:
        "Relatório Lava Jato",

      subtitle:
        `Período: ${range.label}`,

      generatedAt:
        new Date(),

      metrics: [
        {
          label:
            "Total recebido",

          value:
            formatBrlCurrency(
              totalReceived
            ),
        },
        {
          label:
            "Ordens concluídas",

          value:
            formatQuantity(
              entries.length
            ),
        },
        {
          label:
            "Ticket médio",

          value:
            formatBrlCurrency(
              averageTicket
            ),
        },
        {
          label:
            "Formas de pagamento",

          value:
            formatQuantity(
              paymentMethodCount
            ),
        },
      ],

      tables,
    });
  }

  async function handleExportReport() {
    if (
      !exportScope ||
      isExporting
    ) {
      return;
    }

    setExportError("");
    setIsExporting(true);

    try {
      const range =
        resolveReportDateRange(
          exportPeriod,
          customFrom,
          customTo
        );

      if (
        exportScope === "bar"
      ) {
        const entries =
          await loadAllBarHistory(
            range
          );

        await exportBarReport(
          range,
          entries
        );
      } else {
        const entries =
          await loadAllLavaHistory(
            range
          );

        await exportLavaReport(
          range,
          entries
        );
      }

      setExportScope(null);
    } catch (error) {
      setExportError(
        reportErrorMessage(
          error
        )
      );
    } finally {
      setIsExporting(false);
    }
  }

  return (
    <>
      <PageActions>
        <Button
          variant="secondary"
          leadingIcon={<Download />}
          onClick={() =>
            openExportModal(
              "bar"
            )
          }
        >
          Relatório Espeto Bar
        </Button>

        <Button
          variant="secondary"
          leadingIcon={<Download />}
          onClick={() =>
            openExportModal(
              "lava"
            )
          }
        >
          Relatório Lava Jato
        </Button>
      </PageActions>

      <ReportExportModal
        open={
          exportScope !== null
        }
        scope={exportScope}
        period={exportPeriod}
        customFrom={customFrom}
        customTo={customTo}
        error={exportError}
        isExporting={
          isExporting
        }
        onPeriodChange={
          setExportPeriod
        }
        onCustomFromChange={
          setCustomFrom
        }
        onCustomToChange={
          setCustomTo
        }
        onClose={
          closeExportModal
        }
        onConfirm={() =>
          void handleExportReport()
        }
      />
    </>
  );
}
