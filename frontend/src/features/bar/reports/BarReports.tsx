import {
  useEffect,
  useState,
} from "react";

import {
  ContentStack,
  PremiumCard,
  SectionTitle,
} from "../../../components/ui";

import type {
  ReportsRepository,
  SalesReport,
} from "../../../data/contracts";

import {
  formatBrlCurrency,
} from "../../../shared/lib/currency";

import {
  UnifiedReportExportActions,
} from "../../reports/UnifiedReportExportActions";

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

function formatQuantity(value: number) {
  return String(value).padStart(2, "0");
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

export function BarReports({
  repository,
}: {
  repository: ReportsRepository;
}) {
  const [report, setReport] = useState<SalesReport | null>(null);
  const [loadError, setLoadError] = useState("");

  useEffect(() => {
    let current = true;

    async function load() {
      setLoadError("");

      try {
        const response = await repository.summary({ scope: "ALL" });
        if (current) {
          setReport(response);
        }
      } catch (error) {
        if (current) {
          setReport(null);
          setLoadError(
            error instanceof Error
              ? error.message
              : "Não foi possível carregar o relatório."
          );
        }
      }
    }

    void load();
    return () => {
      current = false;
    };
  }, [repository]);

  return (
    <ContentStack>
      <UnifiedReportExportActions repository={repository} />

      {loadError ? (
        <p className="text-sm text-[var(--color-danger)]" role="alert">
          {loadError}
        </p>
      ) : null}

      <PremiumCard>
        <div className="grid gap-3 xl:grid-cols-3">
          <div className="border-r border-[var(--border-subtle)] pr-4 last:border-r-0">
            <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
              Total recebido
            </p>
            <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
              {formatBrlCurrency((report?.totalReceivedCents ?? 0) / 100)}
            </p>
          </div>

          <div className="border-r border-[var(--border-subtle)] pr-4 last:border-r-0">
            <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
              Comandas concluídas
            </p>
            <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
              {formatQuantity(report?.operationCount ?? 0)}
            </p>
          </div>

          <div className="pr-4">
            <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
              Ticket médio
            </p>
            <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
              {formatBrlCurrency((report?.averageTicketCents ?? 0) / 100)}
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
              {(report?.byPaymentMethod.length ?? 0) === 0 ? (
                <p className="py-3 text-sm text-[var(--text-subtle)]">
                  Nenhuma venda concluída.
                </p>
              ) : null}

              {report?.byPaymentMethod.map((entry) => (
                <div
                  key={entry.method}
                  className="flex items-center justify-between gap-3 border-b border-[var(--border-subtle)] py-2.5 last:border-b-0"
                >
                  <p className="truncate text-sm font-medium text-[var(--text-base)]">
                    {paymentLabel(entry.method)}
                  </p>
                  <p className="shrink-0 text-sm text-[var(--text-muted)]">
                    {formatBrlCurrency(entry.amountCents / 100)}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </PremiumCard>

        <PremiumCard className="v7-card-fill overflow-hidden">
          <div className="v7-card-content">
            <div className="v7-card-header flex items-center justify-between gap-4">
              <SectionTitle compact title="Últimos registros" />
              <p className="text-sm text-[var(--text-subtle)]">
                {report?.latestOperations.length ?? 0} registro(s)
              </p>
            </div>

            <div className="v7-list-scroll premium-scroll pr-1">
              {(report?.latestOperations.length ?? 0) === 0 ? (
                <p className="py-3 text-sm text-[var(--text-subtle)]">
                  Nenhuma venda concluída.
                </p>
              ) : null}

              {report?.latestOperations.map((operation) => (
                <div
                  key={operation.operationId}
                  className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-3 border-b border-[var(--border-subtle)] py-3 last:border-b-0"
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-[var(--text-base)]">
                      {operation.displayName}
                    </p>
                    <p className="mt-1 truncate text-xs text-[var(--text-subtle)]">
                      {paymentLabel(operation.paymentMethod)} · {formatDateTime(operation.completedAt)}
                    </p>
                  </div>
                  <p className="text-sm font-medium text-[var(--text-muted)]">
                    {formatBrlCurrency(operation.netCents / 100)}
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
