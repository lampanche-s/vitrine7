import { useEffect, useMemo, useState } from "react";
import {
  ContentStack,
  DropdownSelect,
  EmptyState,
  PremiumCard,
  SectionTitle,
  TextField,
  useToast,
} from "../../components/ui";
import type { EmployeesRepository } from "../../data/contracts/employees.repository";
import { repositories } from "../../data/repositories";
import type { Employee, EmployeeVoucherReport } from "../../entities/employee";
import { formatBrlCurrency } from "../../shared/lib/currency";
import { resolveVoucherReportRange, voucherReportPeriodOptions, type VoucherReportPeriod } from "./employeeVoucherReport.filters";

export function EmployeeVoucherReports({ employees, repository = repositories.employees }: { employees: Employee[]; repository?: EmployeesRepository }) {
  const [report, setReport] = useState<EmployeeVoucherReport | null>(null);
  const [period, setPeriod] = useState<VoucherReportPeriod>("today");
  const [employeeFilter, setEmployeeFilter] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const { showToast } = useToast();
  const filters = useMemo(() => ({
    ...resolveVoucherReportRange(period, from, to),
    ...(employeeFilter ? { employeeId: Number(employeeFilter) } : {}),
  }), [employeeFilter, from, period, to]);

  useEffect(() => {
    let active = true;
    async function load() {
      setLoading(true);
      setError("");
      try {
        const data = await repository.report(filters);
        if (active) setReport(data);
      } catch (loadError) {
        if (!active) return;
        const message = loadError instanceof Error ? loadError.message : "Não foi possível carregar o relatório de Vales.";
        setReport(null);
        setError(message);
        showToast({ title: message, variant: "error" });
      } finally {
        if (active) setLoading(false);
      }
    }
    void load();
    return () => { active = false; };
  }, [filters, repository, showToast]);

  const employeeOptions = [{ value: "", label: "Todos" }, ...employees.map((employee) => ({ value: String(employee.id), label: employee.name }))];

  return <ContentStack>
    <SectionTitle title="Relatórios de vales" subtitle="Análise exclusiva dos consumos internos registrados como Vale." />
    <PremiumCard><div className="grid gap-4 sm:grid-cols-2">
      <DropdownSelect label="Período" value={period} placeholder="Selecione o período" options={voucherReportPeriodOptions} onChange={(value) => setPeriod(value as VoucherReportPeriod)} />
      <DropdownSelect label="Funcionário" value={employeeFilter} placeholder="Selecione o funcionário" options={employeeOptions} onChange={setEmployeeFilter} />
      {period === "custom" ? <><TextField label="Data inicial" type="date" value={from} onChange={setFrom} /><TextField label="Data final" type="date" value={to} onChange={setTo} /></> : null}
    </div></PremiumCard>
    {error ? <EmployeeVoucherReportError message={error} /> : null}
    {loading ? <p className="text-sm text-[var(--text-subtle)]">Carregando relatório...</p> : null}
    {!loading && !error && report ? <EmployeeVoucherReportResults report={report} /> : null}
  </ContentStack>;
}

export function EmployeeVoucherReportError({ message }: { message: string }) {
  return <div role="alert" className="rounded-[4px] border border-[var(--color-danger-border)] bg-[var(--color-danger-soft)] px-4 py-3 text-sm text-[var(--color-danger)]">{message}</div>;
}

export function EmployeeVoucherReportResults({ report }: { report: EmployeeVoucherReport }) {
  if (report.summary.voucherCount === 0) return <EmptyState message="Nenhum Vale encontrado para os filtros selecionados." />;
  return <>
    <div className="grid gap-3 sm:grid-cols-4">{[
      ["Valor em Vales", formatBrlCurrency(report.summary.voucherValue)],
      ["Quantidade de Vales", String(report.summary.voucherCount)],
      ["Valor médio", formatBrlCurrency(report.summary.averageVoucher)],
      ["Unidades consumidas", String(report.summary.consumedUnits)],
    ].map(([label, value]) => <PremiumCard key={label}><span className="text-xs uppercase text-[var(--text-subtle)]">{label}</span><strong className="mt-2 block text-xl">{value}</strong></PremiumCard>)}</div>
    <ReportTable title="Por funcionário" headers={["Funcionário", "Quantidade de vales", "Valor total", "Consumo"]} rows={report.byEmployee.map((row) => [row.employeeName, String(row.voucherCount), formatBrlCurrency(row.total), String(row.consumption)])} />
    <ReportTable title="Itens e serviços" headers={["Item/Serviço", "Tipo", "Quantidade", "Valor", "Evolução"]} rows={report.byEntry.map((row) => [row.name, row.type === "ITEM" ? "Item" : "Serviço", String(row.quantity), formatBrlCurrency(row.total), row.evolutionPercent == null ? "—" : `${row.evolutionPercent.toFixed(1)}%`])} />
    <ReportTable title="Por dia" headers={["Data", "Vales", "Valor"]} rows={report.byDay.map((row) => [new Date(`${row.date}T12:00:00`).toLocaleDateString("pt-BR"), String(row.voucherCount), formatBrlCurrency(row.total)])} />
  </>;
}

function ReportTable({ title, headers, rows }: { title: string; headers: string[]; rows: string[][] }) {
  return <PremiumCard className="overflow-hidden"><h3 className="mb-3 font-semibold">{title}</h3><div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead className="border-b border-[var(--border-subtle)] text-xs uppercase text-[var(--text-subtle)]"><tr>{headers.map((header) => <th key={header} className="p-3">{header}</th>)}</tr></thead><tbody>{rows.map((row, rowIndex) => <tr key={`${title}:${rowIndex}`} className="border-b border-[var(--border-subtle)] last:border-0">{row.map((cell, cellIndex) => <td key={`${rowIndex}:${cellIndex}`} className="p-3">{cell}</td>)}</tr>)}</tbody></table></div></PremiumCard>;
}
