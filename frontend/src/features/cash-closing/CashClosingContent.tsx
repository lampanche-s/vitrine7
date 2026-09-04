import {
  useEffect,
  useState,
} from "react";

import {
  Download,
  Printer,
  RefreshCw,
  WalletCards,
} from "lucide-react";

import {
  Button,
  ContentStack,
  PageActions,
  PremiumCard,
  SectionTitle,
} from "../../components/ui";

import type {
  CashClosingDay,
  CashClosingReport,
  CashClosingRepository,
} from "../../data/contracts/cash-closing.repository";

import {
  repositories,
} from "../../data/repositories";

import {
  formatBrlCurrency,
} from "../../shared/lib/currency";

import {
  useToast,
} from "../../components/ui";

import {
  exportCashClosingPdf,
} from "./cashClosingPdf";

import {
  useReceiptPrinter,
} from "../../shared/receipts/useReceiptPrinter";

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
    case "MULTIPLE":
      return "Múltiplas";
    default:
      return method;
  }
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR").format(
    new Date(`${value}T12:00:00`)
  );
}

function formatCashPeriod(businessDate: string) {
  const start = new Date(`${businessDate}T12:00:00`);
  const end = new Date(start);
  end.setDate(end.getDate() + 1);
  const formatter = new Intl.DateTimeFormat("pt-BR");

  return `${formatter.format(start)} 05:00 → ${formatter.format(end)} 04:59`;
}

function entryTypeLabel(entryType: string) {
  return entryType === "SERVICE" ? "Serviço" : "Item";
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "—";
  }

  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

export function CashClosingContent({
  repository = repositories.cashClosing,
}: {
  repository?: CashClosingRepository;
}) {
  const [day, setDay] = useState<CashClosingDay>("TODAY");
  const [report, setReport] = useState<CashClosingReport | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isClosing, setIsClosing] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const {
    isPrinting,
    printCashClosing,
  } = useReceiptPrinter();
  const {
    showToast,
    showErrorToast,
  } = useToast();

  useEffect(() => {
    let current = true;

    async function loadCurrentDay() {
      try {
        const response = await repository.get(day);
        if (current) {
          setReport(response);
        }
      } catch (error) {
        if (current) {
          showErrorToast(error, {
            title: "Não foi possível carregar o caixa",
          });
        }
      } finally {
        if (current) {
          setIsLoading(false);
        }
      }
    }

    void loadCurrentDay();

    return () => {
      current = false;
    };
  }, [day, repository, showErrorToast]);

  async function refresh() {
    setIsLoading(true);
    try {
      setReport(await repository.get(day));
    } catch (error) {
      showErrorToast(error, {
        title: "Não foi possível carregar o caixa",
      });
    } finally {
      setIsLoading(false);
    }
  }

  async function handleClose() {
    if (isClosing) {
      return;
    }

    setIsClosing(true);
    try {
      const closed = await repository.close(day);
      setReport(closed);
      showToast({
        title: "Caixa fechado",
        description: `Fechamento de ${formatDate(closed.businessDate)} registrado.`,
        variant: "success",
      });
    } catch (error) {
      showErrorToast(error, {
        title: "Não foi possível fechar o caixa",
      });
    } finally {
      setIsClosing(false);
    }
  }

  async function handleExport() {
    if (!report || isExporting) {
      return;
    }

    setIsExporting(true);
    try {
      await exportCashClosingPdf(report);
    } catch (error) {
      showErrorToast(error, {
        title: "Não foi possível exportar o PDF",
      });
    } finally {
      setIsExporting(false);
    }
  }

  return (
    <ContentStack>
      <PageActions>
        <Button
          variant={day === "TODAY" ? "primary" : "secondary"}
          onClick={() => {
            setIsLoading(true);
            setDay("TODAY");
          }}
        >
          Hoje
        </Button>
        <Button
          variant={day === "YESTERDAY" ? "primary" : "secondary"}
          onClick={() => {
            setIsLoading(true);
            setDay("YESTERDAY");
          }}
        >
          Ontem
        </Button>
        <Button
          variant="secondary"
          leadingIcon={<RefreshCw />}
          disabled={isLoading}
          onClick={() => void refresh()}
        >
          Atualizar
        </Button>
        <Button
          variant="secondary"
          leadingIcon={<Download />}
          disabled={!report || isExporting}
          onClick={() => void handleExport()}
        >
          {isExporting ? "Exportando..." : "Exportar PDF"}
        </Button>
        <Button
          variant="secondary"
          leadingIcon={<Printer />}
          disabled={!report || isPrinting}
          onClick={() => void printCashClosing(day)}
        >
          {isPrinting ? "Imprimindo..." : "Imprimir fechamento"}
        </Button>
      </PageActions>

      <PremiumCard>
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <SectionTitle compact title="Fechamento de caixa" />
            <p className="mt-1 text-sm text-[var(--text-muted)]">
              {report
                ? `${formatDate(report.businessDate)} · ${report.userName}`
                : "Carregando período..."}
            </p>
            {report ? (
              <p className="mt-1 text-sm text-[var(--text-subtle)]">
                Período do caixa: {formatCashPeriod(report.businessDate)}
              </p>
            ) : null}
          </div>

          <div className="flex items-center gap-3">
            {report?.closed ? (
              <span className="text-sm text-[var(--text-muted)]">
                Fechado em {formatDateTime(report.closedAt)}
              </span>
            ) : (
              <span className="text-sm text-[var(--text-subtle)]">
                Ainda não fechado
              </span>
            )}

            <Button
              variant="primary"
              leadingIcon={<WalletCards />}
              disabled={!report || isClosing}
              onClick={() => void handleClose()}
            >
              {isClosing
                ? "Fechando..."
                : report?.closed
                  ? "Atualizar fechamento"
                  : "Fechar caixa"}
            </Button>
          </div>
        </div>
      </PremiumCard>

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <PremiumCard>
          <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
            Total bruto vendido
          </p>
          <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
            {formatBrlCurrency((report?.grossSalesCents ?? 0) / 100)}
          </p>
        </PremiumCard>

        <PremiumCard>
          <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
            Total líquido
          </p>
          <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
            {formatBrlCurrency((report?.totalReceivedCents ?? 0) / 100)}
          </p>
        </PremiumCard>

        <PremiumCard>
          <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
            Total em itens
          </p>
          <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
            {formatBrlCurrency((report?.itemSalesCents ?? 0) / 100)}
          </p>
        </PremiumCard>

        <PremiumCard>
          <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
            Total em serviços
          </p>
          <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
            {formatBrlCurrency((report?.serviceSalesCents ?? 0) / 100)}
          </p>
        </PremiumCard>

        <PremiumCard>
          <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
            Vendas concluídas
          </p>
          <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
            {report?.saleCount ?? 0}
          </p>
        </PremiumCard>

        <PremiumCard>
          <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
            Ticket médio
          </p>
          <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
            {formatBrlCurrency((report?.averageTicketCents ?? 0) / 100)}
          </p>
        </PremiumCard>

        <PremiumCard>
          <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
            Estornos
          </p>
          <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
            {formatBrlCurrency((report?.reversedCents ?? 0) / 100)}
          </p>
          <p className="mt-1 text-xs text-[var(--text-subtle)]">
            {report?.reversedCount ?? 0} operação(ões)
          </p>
        </PremiumCard>

        <PremiumCard>
          <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
            Recebido em espécie
          </p>
          <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
            {formatBrlCurrency((report?.cashReceivedCents ?? 0) / 100)}
          </p>
        </PremiumCard>

        <PremiumCard>
          <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
            Troco entregue
          </p>
          <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
            {formatBrlCurrency((report?.cashChangeCents ?? 0) / 100)}
          </p>
        </PremiumCard>

        <PremiumCard>
          <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
            Comandas em aberto
          </p>
          <p className="mt-1 text-lg font-semibold text-[var(--text-base)]">
            {report?.openCommandCount ?? 0}
          </p>
          <p className="mt-1 text-xs text-[var(--text-subtle)]">
            {formatBrlCurrency((report?.openCommandAmountCents ?? 0) / 100)} em consumo
          </p>
        </PremiumCard>

        <PremiumCard>
          <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">
            Movimento do caixa
          </p>
          <p className="mt-1 text-sm font-semibold text-[var(--text-base)]">
            {formatDateTime(report?.firstSaleAt ?? null)}
          </p>
          <p className="mt-1 text-xs text-[var(--text-subtle)]">
            até {formatDateTime(report?.lastSaleAt ?? null)}
          </p>
        </PremiumCard>
      </div>

      <div className="grid gap-3 xl:grid-cols-2">
        <PremiumCard>
          <SectionTitle compact title="Formas de pagamento" />
          <div className="mt-3 divide-y divide-[var(--border-subtle)]">
            {(report?.paymentBreakdown.length ?? 0) === 0 ? (
              <p className="py-3 text-sm text-[var(--text-subtle)]">
                Nenhuma venda concluída.
              </p>
            ) : null}
            {report?.paymentBreakdown.map((item) => (
              <div
                key={item.method}
                className="flex items-center justify-between gap-3 py-3"
              >
                <div>
                  <p className="text-sm font-medium text-[var(--text-base)]">
                    {paymentLabel(item.method)}
                  </p>
                  <p className="text-xs text-[var(--text-subtle)]">
                    {item.saleCount} venda(s)
                  </p>
                </div>
                <p className="text-sm font-semibold text-[var(--text-muted)]">
                  {formatBrlCurrency(item.amountCents / 100)}
                </p>
              </div>
            ))}
          </div>
        </PremiumCard>

        <PremiumCard>
          <SectionTitle compact title="Operações do dia" />
          <div className="premium-scroll mt-3 max-h-[420px] overflow-y-auto pr-1">
            {(report?.operations.length ?? 0) === 0 ? (
              <p className="py-3 text-sm text-[var(--text-subtle)]">
                Nenhuma operação no período.
              </p>
            ) : null}
            {report?.operations.map((operation) => (
              <div
                key={`${operation.operationId}-${operation.completedAt}`}
                className="grid grid-cols-[minmax(0,1fr)_auto] gap-3 border-b border-[var(--border-subtle)] py-3 last:border-b-0"
              >
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-[var(--text-base)]">
                    {operation.displayName}
                  </p>
                  <p className="mt-1 text-xs text-[var(--text-subtle)]">
                    Comanda #{operation.operationId} · {paymentLabel(operation.paymentMethod)} · {formatDateTime(operation.completedAt)}
                    {operation.paymentStatus === "REVERSED"
                      ? " · Estornada"
                      : " · Aprovada"}
                  </p>
                  {operation.lines.length > 0 ? (
                    <div className="mt-2 space-y-1.5">
                      {operation.lines.map((line, index) => (
                        <div
                          key={`${operation.operationId}-${index}-${line.itemName}`}
                          className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-[var(--text-muted)]"
                        >
                          <span className="rounded-full border border-[var(--border-subtle)] px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-[var(--text-subtle)]">
                            {entryTypeLabel(line.entryType)}
                          </span>
                          <span>
                            {line.quantity}x {line.itemName}
                          </span>
                          <span className="text-[var(--text-subtle)]">
                            {formatBrlCurrency(line.lineTotalCents / 100)}
                          </span>
                        </div>
                      ))}
                    </div>
                  ) : null}
                </div>
                <p className={[
                  "text-sm font-medium",
                  operation.paymentStatus === "REVERSED"
                    ? "text-[var(--color-danger)] line-through"
                    : "text-[var(--text-muted)]",
                ].join(" ")}
                >
                  {formatBrlCurrency(operation.amountCents / 100)}
                </p>
              </div>
            ))}
          </div>
        </PremiumCard>
      </div>
    </ContentStack>
  );
}
