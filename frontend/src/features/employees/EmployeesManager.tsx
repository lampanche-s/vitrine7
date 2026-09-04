import { useMemo, useState } from "react";
import { Pencil, Plus, Trash2, X } from "lucide-react";
import {
  AnimatedModal,
  Button,
  ContentStack,
  EmptyState,
  PageActions,
  Pagination,
  PremiumCard,
  SearchField,
  SectionTitle,
  TextField,
} from "../../components/ui";
import type { Employee, EmployeeInput } from "../../entities/employee";

const PAGE_SIZE = 7;

export function EmployeesManager({ employees, onCreate, onUpdate, onRemove }: {
  employees: Employee[];
  onCreate(input: EmployeeInput): Promise<boolean>;
  onUpdate(id: number, input: EmployeeInput): Promise<boolean>;
  onRemove(id: number): Promise<boolean>;
}) {
  const [editing, setEditing] = useState<Employee | null>(null);
  const [pendingDelete, setPendingDelete] = useState<Employee | null>(null);
  const [name, setName] = useState("");
  const [formOpen, setFormOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [error, setError] = useState("");

  const filtered = useMemo(() => {
    const query = search.trim().toLocaleLowerCase("pt-BR");
    return employees.filter((employee) => employee.name.toLocaleLowerCase("pt-BR").includes(query));
  }, [employees, search]);
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const pageStart = (currentPage - 1) * PAGE_SIZE;
  const records = filtered.slice(pageStart, pageStart + PAGE_SIZE);

  function resetForm() { setEditing(null); setName(""); setError(""); }
  function closeForm() { setFormOpen(false); resetForm(); }
  function openCreate() { resetForm(); setFormOpen(true); }
  function openEdit(employee: Employee) { setEditing(employee); setName(employee.name); setError(""); setFormOpen(true); }
  async function submit() {
    const input = { name: name.trim() };
    if (!input.name) { setError("Informe o nome do funcionário."); return; }
    const saved = editing ? await onUpdate(editing.id, input) : await onCreate(input);
    if (saved) { setPage(1); closeForm(); }
  }
  async function confirmDelete() {
    if (pendingDelete && await onRemove(pendingDelete.id)) setPendingDelete(null);
  }

  return <ContentStack>
    <SectionTitle title="Funcionários" subtitle="Cadastre as pessoas que podem ter consumos registrados como Vale." />
    <PageActions><Button variant="primary" leadingIcon={<Plus />} onClick={openCreate}>Novo funcionário</Button></PageActions>
    <PremiumCard className="v7-card-fill" contentClassName="v7-card-content"><div className="flex min-h-0 flex-1 flex-col">
      <SearchField value={search} onChange={(value) => { setSearch(value); setPage(1); }} placeholder="Buscar funcionário..." />
      <div className="v7-list-scroll premium-scroll mt-3 pr-1">
        {filtered.length === 0 ? <EmptyState message="Nenhum funcionário encontrado." /> : null}
        {records.map((employee) => <div key={employee.id} className="min-h-[68px] border-b border-[var(--border-subtle)] py-3 last:border-b-0"><div className="grid gap-3 xl:grid-cols-[minmax(0,1fr)_auto] xl:items-center">
          <p className="min-w-0 truncate text-sm font-semibold text-[var(--text-base)]">{employee.name}</p>
          <div className="flex flex-wrap gap-2 xl:justify-end">
            <Button size="compact" variant="secondary" leadingIcon={<Pencil />} onClick={() => openEdit(employee)}>Editar</Button>
            <Button size="compact" variant="danger" leadingIcon={<Trash2 />} onClick={() => setPendingDelete(employee)}>Excluir</Button>
          </div>
        </div></div>)}
      </div>
      <div className="v7-card-footer">{filtered.length > PAGE_SIZE ? <Pagination currentPage={currentPage} totalPages={totalPages} totalItems={filtered.length} pageStart={pageStart} pageSize={records.length} onPrevious={() => setPage(Math.max(1, currentPage - 1))} onNext={() => setPage(Math.min(totalPages, currentPage + 1))} /> : null}</div>
    </div></PremiumCard>

    <AnimatedModal open={formOpen} onClose={closeForm} labelledBy="employee-form-title" backdropClassName="z-[220] p-4" panelClassName="w-full max-w-[460px] overflow-visible rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]">
      <div className="flex items-start justify-between gap-4"><div id="employee-form-title"><SectionTitle compact title={editing ? "Editar funcionário" : "Novo funcionário"} /></div><Button size="icon" variant="ghost" leadingIcon={<X />} aria-label="Fechar modal" title="Fechar" onClick={closeForm} /></div>
      <div className="mt-4 grid gap-4"><TextField label="Nome" value={name} placeholder="Nome do funcionário" onChange={(value) => { setName(value); setError(""); }} />{error ? <div role="alert" className="rounded-[var(--control-radius)] border border-[var(--color-danger-border)] bg-[var(--color-danger-soft)] px-3 py-2 text-sm text-[var(--color-danger)]">{error}</div> : null}</div>
      <div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end"><Button variant="secondary" onClick={closeForm}>Cancelar</Button><Button variant="primary" onClick={() => void submit()}>{editing ? "Salvar alterações" : "Criar"}</Button></div>
    </AnimatedModal>

    <AnimatedModal open={pendingDelete !== null} onClose={() => setPendingDelete(null)} labelledBy="employee-delete-title" backdropClassName="z-[230] p-4" panelClassName="w-full max-w-md overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]">
      {pendingDelete ? <><div id="employee-delete-title"><SectionTitle compact title="Excluir funcionário" subtitle={pendingDelete.name} /></div><p className="mt-4 text-sm text-[var(--text-muted)]">O histórico de Vales será preservado.</p><div className="mt-5 flex justify-end gap-2"><Button variant="secondary" onClick={() => setPendingDelete(null)}>Cancelar</Button><Button variant="danger" onClick={() => void confirmDelete()}>Excluir</Button></div></> : null}
    </AnimatedModal>
  </ContentStack>;
}
