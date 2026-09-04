import { EmptyState, PremiumCard, SectionTitle } from "../../../components/ui";
import type { SalesReport, SalesReportPerformance } from "../../../data/contracts";
import { formatBrlCurrency } from "../../../shared/lib/currency";
import { paymentLabel } from "./barReports.selectors";

function formatQuantity(value: number) {
  return new Intl.NumberFormat("pt-BR").format(value);
}

function formatPercentage(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    minimumFractionDigits: 1,
    maximumFractionDigits: 2,
  }).format(value) + "%";
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "short" }).format(
    new Date(`${value}T12:00:00`)
  );
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0 rounded-[4px] border border-[var(--border-subtle)] px-4 py-3">
      <p className="text-[11px] font-medium uppercase text-[var(--text-subtle)]">{label}</p>
      <p className="mt-1 truncate text-lg font-semibold text-[var(--text-base)]">{value}</p>
    </div>
  );
}

function SectionCard({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <PremiumCard className="overflow-hidden">
      <div className="v7-card-header">
        <SectionTitle compact title={title} subtitle={subtitle} />
      </div>
      {children}
    </PremiumCard>
  );
}

function PerformanceTable({
  title,
  noun,
  rows,
}: {
  title: string;
  noun: string;
  rows: SalesReportPerformance[];
}) {
  return (
    <SectionCard
      title={title}
      subtitle={`Ordenado por receita gerada. Valores calculados a partir das linhas históricas de ${noun}.`}
    >
      {rows.length === 0 ? <EmptyState message={`Nenhum ${noun} no período.`} /> : (
        <div className="overflow-x-auto premium-scroll">
          <table className="w-full min-w-[620px] border-collapse text-left">
            <thead>
              <tr className="border-b border-[var(--border-subtle)] text-[11px] font-medium uppercase text-[var(--text-subtle)]">
                <th className="px-3 py-2 font-medium">Nome</th>
                <th className="px-3 py-2 text-right font-medium">Quantidade</th>
                <th className="px-3 py-2 text-right font-medium">Receita</th>
                <th className="px-3 py-2 text-right font-medium">Preço médio praticado</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={`${row.entryType}-${row.name}`} className="border-b border-[var(--border-subtle)] last:border-b-0">
                  <td className="px-3 py-3 text-sm font-medium text-[var(--text-base)]">{row.name}</td>
                  <td className="px-3 py-3 text-right text-sm text-[var(--text-muted)]">{formatQuantity(row.quantity)}</td>
                  <td className="px-3 py-3 text-right text-sm font-semibold text-[var(--text-base)]">{formatBrlCurrency(row.revenueCents / 100)}</td>
                  <td className="px-3 py-3 text-right text-sm text-[var(--text-muted)]">{formatBrlCurrency(row.averagePriceCents / 100)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </SectionCard>
  );
}

export function BarReportSections({
  report,
  isLoading,
}: {
  report: SalesReport | null;
  isLoading: boolean;
}) {
  const fallback = isLoading ? "Carregando..." : "—";
  const showProducts = report?.scope !== "SERVICE";
  const showServices = report?.scope !== "ITEM";

  return (
    <div className="grid gap-4 xl:grid-cols-12">
      <div className="xl:col-span-12">
        <PremiumCard>
          <SectionTitle compact title="Resumo principal" subtitle="Somente operações concluídas e pagamentos válidos." />
          <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            <Metric label="Total recebido" value={report ? formatBrlCurrency(report.totalReceivedCents / 100) : fallback} />
            <Metric label="Operações concluídas" value={report ? formatQuantity(report.operationCount) : fallback} />
            <Metric label="Ticket médio" value={report ? formatBrlCurrency(report.averageTicketCents / 100) : fallback} />
            <Metric label="Unidades vendidas" value={report ? formatQuantity(report.totalUnits) : fallback} />
            {showProducts ? <Metric label="Receita de produtos" value={report ? formatBrlCurrency(report.itemRevenueCents / 100) : fallback} /> : null}
            {showServices ? <Metric label="Receita de serviços" value={report ? formatBrlCurrency(report.serviceRevenueCents / 100) : fallback} /> : null}
          </div>
        </PremiumCard>
      </div>

      <div className="xl:col-span-7">
        <SectionCard title="Distribuição entre produtos e serviços" subtitle="Comparação sobre o total efetivamente recebido no período.">
          {!isLoading && (report?.distribution.length ?? 0) === 0 ? <EmptyState message="Nenhuma receita recebida no período." /> : null}
          <div className="grid gap-3">
            {report?.distribution.map((entry) => (
              <div key={entry.entryType} className="rounded-[4px] border border-[var(--border-subtle)] p-4">
                <p className="text-sm font-semibold text-[var(--text-base)]">{entry.entryType === "ITEM" ? "Produtos" : "Serviços"}</p>
                <div className="mt-3 grid grid-cols-3 gap-3">
                  <Metric label="Receita" value={formatBrlCurrency(entry.revenueCents / 100)} />
                  <Metric label={entry.entryType === "ITEM" ? "Unidades" : "Quantidade"} value={formatQuantity(entry.quantity)} />
                  <Metric label="Participação" value={formatPercentage(entry.participationPercentage)} />
                </div>
              </div>
            ))}
          </div>
        </SectionCard>
      </div>

      <div className="xl:col-span-5">
        <SectionCard title="Formas de pagamento" subtitle="Pagamentos divididos aparecem em seus respectivos métodos.">
          {!isLoading && (report?.byPaymentMethod.length ?? 0) === 0 ? <EmptyState message="Nenhum pagamento concluído no período." /> : null}
          <div className="grid gap-2">
            {report?.byPaymentMethod.map((entry) => (
              <div key={entry.method} className="flex items-center justify-between gap-3 rounded-[4px] border border-[var(--border-subtle)] p-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-[var(--text-base)]">{paymentLabel(entry.method)}</p>
                  <p className="mt-0.5 text-xs text-[var(--text-subtle)]">{formatQuantity(entry.paymentCount)} {entry.paymentCount === 1 ? "pagamento" : "pagamentos"} · {formatPercentage(entry.participationPercentage)}</p>
                </div>
                <p className="shrink-0 text-sm font-semibold text-[var(--text-base)]">{formatBrlCurrency(entry.amountCents / 100)}</p>
              </div>
            ))}
          </div>
        </SectionCard>
      </div>

      {showServices ? (
        <div className={showProducts ? "xl:col-span-6" : "xl:col-span-12"}>
          <PerformanceTable title="Desempenho de serviços" noun="serviço executado" rows={report?.servicePerformance ?? []} />
        </div>
      ) : null}

      {showProducts ? (
        <div className={showServices ? "xl:col-span-6" : "xl:col-span-12"}>
          <PerformanceTable title="Desempenho de produtos" noun="produto vendido" rows={report?.productPerformance ?? []} />
        </div>
      ) : null}

      <div className="xl:col-span-12">
        <SectionCard title="Evolução dentro do período" subtitle="Agregação diária das operações concluídas.">
          {!isLoading && (report?.dailyEvolution.length ?? 0) === 0 ? <EmptyState message="Nenhuma operação concluída no período." /> : null}
          {(report?.dailyEvolution.length ?? 0) > 0 ? (
            <div className="overflow-x-auto premium-scroll">
              <table className="w-full min-w-[560px] border-collapse text-left">
                <thead><tr className="border-b border-[var(--border-subtle)] text-[11px] font-medium uppercase text-[var(--text-subtle)]"><th className="px-3 py-2 font-medium">Data</th><th className="px-3 py-2 text-right font-medium">Operações</th><th className="px-3 py-2 text-right font-medium">Recebido</th><th className="px-3 py-2 text-right font-medium">Ticket médio</th></tr></thead>
                <tbody>{report?.dailyEvolution.map((day) => <tr key={day.date} className="border-b border-[var(--border-subtle)] last:border-b-0"><td className="px-3 py-3 text-sm font-medium text-[var(--text-base)]">{formatDate(day.date)}</td><td className="px-3 py-3 text-right text-sm text-[var(--text-muted)]">{formatQuantity(day.operationCount)}</td><td className="px-3 py-3 text-right text-sm font-semibold text-[var(--text-base)]">{formatBrlCurrency(day.receivedCents / 100)}</td><td className="px-3 py-3 text-right text-sm text-[var(--text-muted)]">{formatBrlCurrency(day.averageTicketCents / 100)}</td></tr>)}</tbody>
              </table>
            </div>
          ) : null}
        </SectionCard>
      </div>

      <div className="xl:col-span-12">
        <SectionCard title="Operações que compõem o relatório" subtitle="Evidência dos agregados do período, sem ações de edição ou reabertura.">
          {!isLoading && (report?.operations.length ?? 0) === 0 ? <EmptyState message="Nenhuma operação concluída no período." /> : null}
          {(report?.operations.length ?? 0) > 0 ? (
            <div className="overflow-x-auto premium-scroll">
              <table className="w-full min-w-[940px] border-collapse text-left">
                <thead><tr className="border-b border-[var(--border-subtle)] text-[11px] font-medium uppercase text-[var(--text-subtle)]"><th className="px-3 py-2 font-medium">Data/hora</th><th className="px-3 py-2 font-medium">Comanda ou cliente</th><th className="px-3 py-2 text-right font-medium">Itens</th><th className="px-3 py-2 text-right font-medium">Serviços</th><th className="px-3 py-2 font-medium">Pagamento</th><th className="px-3 py-2 font-medium">Operador</th><th className="px-3 py-2 text-right font-medium">Total</th></tr></thead>
                <tbody>{report?.operations.map((operation) => <tr key={operation.operationId} className="border-b border-[var(--border-subtle)] last:border-b-0"><td className="px-3 py-3 text-sm text-[var(--text-muted)]">{formatDateTime(operation.completedAt)}</td><td className="px-3 py-3 text-sm font-medium text-[var(--text-base)]">{operation.displayName}</td><td className="px-3 py-3 text-right text-sm text-[var(--text-muted)]">{formatQuantity(operation.itemUnits)}</td><td className="px-3 py-3 text-right text-sm text-[var(--text-muted)]">{formatQuantity(operation.serviceUnits)}</td><td className="px-3 py-3 text-sm text-[var(--text-muted)]">{paymentLabel(operation.paymentMethod)}</td><td className="px-3 py-3 text-sm text-[var(--text-muted)]">{operation.responsibleUserName ?? "Não informado"}</td><td className="px-3 py-3 text-right text-sm font-semibold text-[var(--text-base)]">{formatBrlCurrency(operation.netCents / 100)}</td></tr>)}</tbody>
              </table>
            </div>
          ) : null}
        </SectionCard>
      </div>
    </div>
  );
}
