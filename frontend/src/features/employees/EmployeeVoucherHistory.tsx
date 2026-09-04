import { useEffect, useMemo, useState } from "react";
import { X } from "lucide-react";
import {
  AnimatedModal,
  Button,
  ContentStack,
  DropdownSelect,
  EmptyState,
  Pagination,
  PremiumCard,
  SearchField,
  SectionTitle,
  TextField,
  useToast,
} from "../../components/ui";
import { repositories } from "../../data/repositories";
import type { Employee, EmployeeVoucher } from "../../entities/employee";
import { formatBrlCurrency } from "../../shared/lib/currency";

const PAGE_SIZE = 7;

export function EmployeeVoucherHistory({ employees }: { employees: Employee[] }) {
  const [vouchers, setVouchers] = useState<EmployeeVoucher[]>([]);
  const [employeeId, setEmployeeId] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<EmployeeVoucher | null>(null);
  const [selectionTime, setSelectionTime] = useState(0);
  const { showToast } = useToast();
  const canReopenSelected = selected
    ? selectionTime <= new Date(selected.closedAt).getTime() + 60 * 60 * 1000
    : false;

  const filtered = useMemo(() => {
    const query = search.trim().toLocaleLowerCase("pt-BR");
    return vouchers.filter((voucher) => !query || [voucher.employeeName, voucher.registeredTabName, voucher.operatorName].some((value) => value.toLocaleLowerCase("pt-BR").includes(query)));
  }, [search, vouchers]);
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const pageStart = (currentPage - 1) * PAGE_SIZE;
  const records = filtered.slice(pageStart, pageStart + PAGE_SIZE);

  async function reopenSelected() {
    if (!selected || !canReopenSelected) return;
    try {
      await repositories.bar.reopenCommand(selected.operationId);
      setVouchers((current) => current.filter((voucher) => voucher.operationId !== selected.operationId));
      setSelected(null);
    } catch (error) {
      showToast({ title: error instanceof Error ? error.message : "Não foi possível retomar a comanda.", variant: "error" });
    }
  }

  useEffect(() => {
    let active = true;
    const nextDay = to ? new Date(`${to}T00:00:00`) : null;
    if (nextDay) nextDay.setDate(nextDay.getDate() + 1);
    async function load() {
      setLoading(true);
      try {
        const data = await repositories.employees.listVouchers({
          ...(employeeId ? { employeeId: Number(employeeId) } : {}),
          ...(from ? { from: new Date(`${from}T00:00:00`).toISOString() } : {}),
          ...(nextDay ? { to: nextDay.toISOString() } : {}),
        });
        if (active) { setVouchers(data); setPage(1); }
      } catch (error) {
        if (active) showToast({ title: error instanceof Error ? error.message : "Não foi possível carregar o histórico de Vales.", variant: "error" });
      } finally {
        if (active) setLoading(false);
      }
    }
    void load();
    return () => { active = false; };
  }, [employeeId, from, showToast, to]);

  return <ContentStack>
    <SectionTitle title="Histórico de vales" subtitle="Consumos encerrados como Vale, sem movimentação financeira." />
    <PremiumCard><div className="grid gap-4 sm:grid-cols-3">
      <DropdownSelect label="Funcionário" value={employeeId} placeholder="Selecione o funcionário" options={[{ value: "", label: "Todos" }, ...employees.map((employee) => ({ value: String(employee.id), label: employee.name }))]} onChange={setEmployeeId} />
      <TextField label="De" type="date" value={from} onChange={setFrom} />
      <TextField label="Até" type="date" value={to} onChange={setTo} />
    </div></PremiumCard>
    <PremiumCard className="v7-card-fill" contentClassName="v7-card-content"><div className="flex min-h-0 flex-1 flex-col">
      <SearchField value={search} onChange={(value) => { setSearch(value); setPage(1); }} placeholder="Buscar no histórico..." />
      <div className="v7-list-scroll premium-scroll mt-3 pr-1">
        {loading ? <p className="py-4 text-sm text-[var(--text-subtle)]">Carregando histórico...</p> : null}
        {!loading && filtered.length === 0 ? <EmptyState message="Nenhum Vale encontrado. Os Vales registrados aparecerão aqui." /> : null}
        {!loading ? records.map((voucher) => <div key={voucher.operationId} className="min-h-[76px] border-b border-[var(--border-subtle)] py-3 last:border-b-0"><div className="grid gap-3 xl:grid-cols-[minmax(0,1fr)_auto] xl:items-center">
          <div className="min-w-0"><p className="truncate text-sm font-semibold text-[var(--text-base)]">{voucher.employeeName}</p><p className="mt-1 text-xs text-[var(--text-muted)]">{voucher.registeredTabName} · {new Date(voucher.closedAt).toLocaleString("pt-BR")} · {voucher.totalUnits} unidades · {voucher.operatorName}</p></div>
          <div className="flex items-center gap-3 xl:justify-end"><strong className="text-sm">{formatBrlCurrency(voucher.total)}</strong><Button size="compact" variant="secondary" onClick={() => { setSelectionTime(Date.now()); setSelected(voucher); }}>Visualizar</Button></div>
        </div></div>) : null}
      </div>
      <div className="v7-card-footer">{!loading && filtered.length > PAGE_SIZE ? <Pagination currentPage={currentPage} totalPages={totalPages} totalItems={filtered.length} pageStart={pageStart} pageSize={records.length} onPrevious={() => setPage(Math.max(1, currentPage - 1))} onNext={() => setPage(Math.min(totalPages, currentPage + 1))} /> : null}</div>
    </div></PremiumCard>

    <AnimatedModal open={selected !== null} onClose={() => setSelected(null)} labelledBy="voucher-detail-title" backdropClassName="z-[220] p-4" panelClassName="w-full max-w-[620px] overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]">
      {selected ? <><div className="flex items-start justify-between gap-4"><div id="voucher-detail-title"><SectionTitle compact title={`Detalhe do Vale #${selected.operationId}`} subtitle={`${selected.employeeName} · ${new Date(selected.closedAt).toLocaleString("pt-BR")}`} /></div><Button size="icon" variant="ghost" leadingIcon={<X />} aria-label="Fechar modal" title="Fechar" onClick={() => setSelected(null)} /></div><div className="mt-4 max-h-72 overflow-y-auto">{selected.lines.map((line) => <div key={`${line.catalogEntryId}:${line.itemName}`} className="flex justify-between gap-3 border-b border-[var(--border-subtle)] py-3 text-sm last:border-0"><span>{line.quantity}x {line.itemName} <small className="text-[var(--text-muted)]">{line.type === "ITEM" ? "Item" : "Serviço"}</small></span><strong>{formatBrlCurrency(line.total)}</strong></div>)}</div><div className="mt-5 flex justify-end gap-2">{canReopenSelected ? <Button variant="secondary" onClick={() => void reopenSelected()}>Retomar comanda</Button> : null}<Button variant="secondary" onClick={() => setSelected(null)}>Fechar</Button></div></> : null}
    </AnimatedModal>
  </ContentStack>;
}
