import {
  useMemo,
  useState,
} from "react";

import {
  DatabaseBackup,
  Download,
} from "lucide-react";

import {
  Button,
  PageActions,
} from "../../components/ui";

import type {
  ReportsRepository,
  SalesReport,
} from "../../data/contracts";

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

function paymentLabel(method: string) {
  switch (method) {
    case "CASH":
      return "Dinheiro";
    case "PIX":
      return "Pix";
    case "CREDIT_CARD":
      return "Crédito";
    case "DEBIT_CARD":
      return "Débito";
    default:
      return method || "Não informado";
  }
}

function entryTypeLabel(type: "ITEM" | "SERVICE") {
  return type === "SERVICE" ? "Serviço" : "Item";
}


function reportErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : "Não foi possível exportar o relatório.";
}

function buildWorkbookTables(report: SalesReport): ReportTable[] {
  return [
    {
      sheetName: "Operações",
      title: "Comandas concluídas",
      columns: [
        { key: "command", header: "Comanda", width: 24 },
        { key: "completedAt", header: "Conclusão", width: 22 },
        { key: "payment", header: "Pagamento", width: 18 },
        { key: "operator", header: "Operador", width: 24 },
        { key: "lines", header: "Linhas", width: 12, type: "number" },
        { key: "units", header: "Unidades", width: 12, type: "number" },
        { key: "gross", header: "Bruto", width: 16, type: "currency" },
        { key: "discount", header: "Desconto", width: 16, type: "currency" },
        { key: "net", header: "Recebido", width: 16, type: "currency" },
      ],
      rows: report.operations.map((operation) => ({
        command: operation.displayName,
        completedAt: new Date(operation.completedAt),
        payment: paymentLabel(operation.paymentMethod),
        operator: operation.responsibleUserName ?? "Não informado",
        lines: operation.lineCount,
        units: operation.totalUnits,
        gross: operation.grossCents / 100,
        discount: operation.discountCents / 100,
        net: operation.netCents / 100,
      })),
    },
    {
      sheetName: report.scope === "SERVICE" ? "Serviços" : "Itens",
      title: report.scope === "SERVICE"
        ? "Serviços vendidos"
        : "Itens vendidos",
      columns: [
        { key: "command", header: "Comanda", width: 24 },
        { key: "completedAt", header: "Conclusão", width: 22, type: "date" },
        { key: "name", header: "Descrição", width: 34 },
        { key: "type", header: "Tipo", width: 14 },
        { key: "quantity", header: "Quantidade", width: 14, type: "number" },
        { key: "unitPrice", header: "Valor unitário", width: 18, type: "currency" },
        { key: "total", header: "Total", width: 16, type: "currency" },
      ],
      rows: report.lines.map((line) => ({
        command: line.displayName,
        completedAt: new Date(line.completedAt),
        name: line.itemName,
        type: entryTypeLabel(line.entryType),
        quantity: line.quantity,
        unitPrice: line.unitPriceCents / 100,
        total: line.totalCents / 100,
      })),
    },
  ];
}

async function exportWorkbook(
  scope: ReportScope,
  range: ReportDateRange,
  report: SalesReport
) {
  const isService = scope === "SERVICE";

  await exportReportWorkbook({
    fileName: `${isService ? "relatorio-lava-jato" : "relatorio-espeto-bar"}-${range.fileSuffix}`,
    title: isService
      ? "Relatório Lava Jato"
      : "Relatório Espeto Bar",
    subtitle: `Período: ${range.label}`,
    generatedAt: new Date(),
    metrics: [
      {
        label: "Total recebido",
        value: formatBrlCurrency(report.totalReceivedCents / 100),
      },
      {
        label: "Comandas",
        value: String(report.operationCount),
      },
      {
        label: "Ticket médio",
        value: formatBrlCurrency(report.averageTicketCents / 100),
      },
      {
        label: "Unidades",
        value: String(report.totalUnits),
      },
    ],
    tables: buildWorkbookTables(report),
  });
}

export function UnifiedReportExportActions({
  repository,
}: {
  repository: ReportsRepository;
}) {
  const defaults = useMemo(
    () => getDefaultCustomReportDates(),
    []
  );

  const [exportScope, setExportScope] = useState<ReportScope | null>(null);
  const [exportPeriod, setExportPeriod] = useState<ReportPeriodPreset>("currentMonth");
  const [customFrom, setCustomFrom] = useState(defaults.from);
  const [customTo, setCustomTo] = useState(defaults.to);
  const [exportError, setExportError] = useState("");
  const [backupError, setBackupError] = useState("");
  const [isExporting, setIsExporting] = useState(false);
  const [isBackingUp, setIsBackingUp] = useState(false);

  function openExportModal(scope: ReportScope) {
    setExportScope(scope);
    setExportError("");
  }

  function closeExportModal() {
    if (!isExporting) {
      setExportScope(null);
      setExportError("");
    }
  }

  async function handleExportReport() {
    if (!exportScope || isExporting) {
      return;
    }

    setExportError("");
    setIsExporting(true);

    try {
      const range = resolveReportDateRange(
        exportPeriod,
        customFrom,
        customTo
      );

      const report = await repository.export({
        from: range.startDate,
        to: range.endDate,
        scope: exportScope,
      });

      await exportWorkbook(exportScope, range, report);
      setExportScope(null);
    } catch (error) {
      setExportError(reportErrorMessage(error));
    } finally {
      setIsExporting(false);
    }
  }


  async function handleSystemBackup() {
    if (isBackingUp) {
      return;
    }

    setBackupError("");
    setIsBackingUp(true);

    try {
      const backup =
        await repository.downloadSystemBackup();

      const objectUrl =
        URL.createObjectURL(backup.blob);
      const anchor =
        document.createElement("a");

      anchor.href = objectUrl;
      anchor.download = backup.fileName;
      anchor.style.display = "none";

      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();

      window.setTimeout(
        () => URL.revokeObjectURL(objectUrl),
        0
      );
    } catch (error) {
      setBackupError(
        error instanceof Error
          ? error.message
          : "Não foi possível baixar o backup."
      );
    } finally {
      setIsBackingUp(false);
    }
  }

  return (
    <>
      <PageActions>
        <Button
          variant="secondary"
          leadingIcon={<Download />}
          onClick={() => openExportModal("ITEM")}
        >
          Relatório Espeto Bar
        </Button>

        <Button
          variant="secondary"
          leadingIcon={<Download />}
          onClick={() => openExportModal("SERVICE")}
        >
          Relatório Lava Jato
        </Button>

        <Button
          variant="secondary"
          leadingIcon={<DatabaseBackup />}
          disabled={isBackingUp}
          onClick={() =>
            void handleSystemBackup()
          }
        >
          {isBackingUp
            ? "Gerando backup..."
            : "Baixar backup"}
        </Button>
      </PageActions>

      {backupError ? (
        <p
          className="text-sm text-[var(--color-danger)]"
          role="alert"
        >
          {backupError}
        </p>
      ) : null}

      <ReportExportModal
        open={exportScope !== null}
        scope={exportScope}
        period={exportPeriod}
        customFrom={customFrom}
        customTo={customTo}
        error={exportError}
        isExporting={isExporting}
        onPeriodChange={setExportPeriod}
        onCustomFromChange={setCustomFrom}
        onCustomToChange={setCustomTo}
        onClose={closeExportModal}
        onConfirm={() => void handleExportReport()}
      />
    </>
  );
}
