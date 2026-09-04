import { useMemo, useState } from "react";
import { Eye, Pencil, Plus, Trash2, X } from "lucide-react";
import { AnimatedModal, Button, ContentStack, EmptyState, InfoField, PageActions, Pagination, PremiumCard, SearchField, SectionTitle, TextField } from "../../components/ui";
import type { Supplier, SupplierDetail, SupplierInput } from "../../entities/supplier";
import { isSupplierInputComplete, normalizeSupplierInput } from "../../entities/supplier";
import { formatBrlCurrency } from "../../shared/lib/currency";

const emptyForm: SupplierInput = { name: "", cnpj: null, phone: null, cep: null };
const PAGE_SIZE = 7;

export function SuppliersManager({ suppliers, onCreate, onUpdate, onRemove, onLoadDetail }: {
  suppliers: Supplier[];
  onCreate: (input: SupplierInput) => Promise<boolean>;
  onUpdate: (id: number, input: SupplierInput) => Promise<boolean>;
  onRemove: (id: number) => Promise<boolean>;
  onLoadDetail: (id: number) => Promise<SupplierDetail | null>;
}) {
  const [form, setForm] = useState<SupplierInput>(emptyForm);
  const [editing, setEditing] = useState<Supplier | null>(null);
  const [pendingDelete, setPendingDelete] = useState<Supplier | null>(null);
  const [detail, setDetail] = useState<SupplierDetail | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [error, setError] = useState("");

  const filtered = useMemo(() => {
    const query = search.trim().toLocaleLowerCase("pt-BR");
    return suppliers.filter((supplier) => !query || [supplier.name, supplier.cnpj ?? "", supplier.phone ?? "", supplier.cep ?? ""].some((value) => value.toLocaleLowerCase("pt-BR").includes(query)));
  }, [search, suppliers]);
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const start = (currentPage - 1) * PAGE_SIZE;
  const records = filtered.slice(start, start + PAGE_SIZE);

  function resetForm() { setForm(emptyForm); setEditing(null); setError(""); }
  function closeForm() { setFormOpen(false); resetForm(); }
  function openCreate() { resetForm(); setFormOpen(true); }
  function openEdit(supplier: Supplier) { setEditing(supplier); setForm({ name: supplier.name, cnpj: supplier.cnpj, phone: supplier.phone, cep: supplier.cep }); setError(""); setFormOpen(true); }
  function update(field: keyof SupplierInput, value: string) { setError(""); setForm((current) => ({ ...current, [field]: field === "name" ? value : value || null })); }
  async function submit() {
    const input = normalizeSupplierInput(form);
    if (!isSupplierInputComplete(input)) { setError("Informe o nome do fornecedor."); return; }
    const success = editing ? await onUpdate(editing.id, input) : await onCreate(input);
    if (!success) { setError(editing ? "Não foi possível atualizar o fornecedor." : "Não foi possível cadastrar o fornecedor."); return; }
    setPage(1); closeForm();
  }
  async function openDetail(id: number) { setDetail(null); setDetailOpen(true); setDetail(await onLoadDetail(id)); }
  async function confirmDelete() { if (pendingDelete && await onRemove(pendingDelete.id)) setPendingDelete(null); }

  return <ContentStack>
    <PageActions><Button variant="primary" leadingIcon={<Plus />} onClick={openCreate}>Novo fornecedor</Button></PageActions>
    <PremiumCard className="v7-card-fill" contentClassName="v7-card-content"><div className="flex min-h-0 flex-1 flex-col">
      <SearchField value={search} onChange={(value) => { setSearch(value); setPage(1); }} placeholder="Buscar fornecedor..." />
      <div className="v7-list-scroll premium-scroll mt-3 pr-1">
        {filtered.length === 0 ? <EmptyState message="Nenhum fornecedor encontrado." /> : null}
        {records.map((supplier) => <div key={supplier.id} className="min-h-[68px] border-b border-[var(--border-subtle)] py-3 last:border-b-0"><div className="grid gap-3 xl:grid-cols-[minmax(0,1fr)_auto] xl:items-center"><div className="min-w-0"><p className="truncate text-sm font-semibold text-[var(--text-base)]">{supplier.name}</p><p className="mt-1 text-xs text-[var(--text-muted)]">{supplier.cnpj ? `CNPJ: ${supplier.cnpj}` : "Sem CNPJ informado"}</p></div><div className="flex flex-wrap gap-2 xl:justify-end"><Button size="compact" variant="secondary" leadingIcon={<Eye />} onClick={() => void openDetail(supplier.id)}>Visualizar</Button><Button size="compact" variant="secondary" leadingIcon={<Pencil />} onClick={() => openEdit(supplier)}>Editar</Button><Button size="compact" variant="danger" leadingIcon={<Trash2 />} onClick={() => setPendingDelete(supplier)}>Excluir</Button></div></div></div>)}
      </div>
      <div className="v7-card-footer">{filtered.length > PAGE_SIZE ? <Pagination currentPage={currentPage} totalPages={totalPages} totalItems={filtered.length} pageStart={start} pageSize={records.length} onPrevious={() => setPage(Math.max(1, currentPage - 1))} onNext={() => setPage(Math.min(totalPages, currentPage + 1))} /> : null}</div>
    </div></PremiumCard>
    <AnimatedModal open={formOpen} onClose={closeForm} labelledBy="supplier-form-title" backdropClassName="z-[220] p-4" panelClassName="w-full max-w-[460px] overflow-visible rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"><div className="flex items-start justify-between gap-4"><SectionTitle compact title={editing ? "Editar fornecedor" : "Novo fornecedor"}/><Button size="icon" variant="ghost" onClick={closeForm} leadingIcon={<X />} aria-label="Fechar modal" title="Fechar" /></div><div className="mt-4 grid gap-4"><TextField label="Nome" value={form.name} placeholder="Nome do fornecedor" onChange={(value) => update("name", value)} /><TextField label="CNPJ" value={form.cnpj ?? ""} placeholder="00.000.000/0000-00" onChange={(value) => update("cnpj", value)} /><TextField label="Telefone" value={form.phone ?? ""} placeholder="(00) 00000-0000" onChange={(value) => update("phone", value)} /><TextField label="CEP" value={form.cep ?? ""} placeholder="00000-000" onChange={(value) => update("cep", value)} />{error ? <div role="alert" className="rounded-[var(--control-radius)] border border-[var(--color-danger-border)] bg-[var(--color-danger-soft)] px-3 py-2 text-sm text-[var(--color-danger)]">{error}</div> : null}</div><div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end"><Button variant="secondary" onClick={closeForm}>Cancelar</Button><Button variant="primary" onClick={() => void submit()}>{editing ? "Salvar alterações" : "Criar"}</Button></div></AnimatedModal>
    <AnimatedModal open={detailOpen} onClose={() => { setDetailOpen(false); setDetail(null); }} labelledBy="supplier-detail-title" backdropClassName="z-[220] p-4" panelClassName="w-full max-w-[620px] overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"><div className="flex items-start justify-between gap-4"><SectionTitle compact title={detail?.name ?? "Visualizar fornecedor"}/><Button size="icon" variant="ghost" onClick={() => setDetailOpen(false)} leadingIcon={<X />} aria-label="Fechar modal" title="Fechar" /></div>{detail ? <div className="mt-4 grid gap-4"><div className="grid gap-3 sm:grid-cols-3"><InfoField label="CNPJ" value={detail.cnpj ?? "Não informado"}/><InfoField label="Telefone" value={detail.phone ?? "Não informado"}/><InfoField label="CEP" value={detail.cep ?? "Não informado"}/></div><div><p className="text-sm font-semibold text-[var(--text-base)]">Itens vinculados</p><div className="mt-2 max-h-60 overflow-y-auto rounded-[4px] border border-[var(--border-subtle)]">{detail.catalogEntries.length ? detail.catalogEntries.map((item) => <div key={item.id} className="flex items-center justify-between gap-3 border-b border-[var(--border-subtle)] px-3 py-2 text-sm last:border-0"><span className="min-w-0 truncate text-[var(--text-base)]">{item.name}</span><span className="shrink-0 text-[var(--text-muted)]">{formatBrlCurrency(item.price)}</span></div>) : <p className="px-3 py-4 text-sm text-[var(--text-subtle)]">Nenhum item vinculado.</p>}</div></div></div> : <p className="mt-4 text-sm text-[var(--text-subtle)]">Carregando fornecedor...</p>}</AnimatedModal>
    <AnimatedModal open={pendingDelete !== null} onClose={() => setPendingDelete(null)} labelledBy="supplier-delete-title" backdropClassName="z-[230] p-4" panelClassName="w-full max-w-md overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]">{pendingDelete ? <><SectionTitle compact title="Excluir fornecedor" subtitle={pendingDelete.name}/><p className="mt-4 text-sm text-[var(--text-muted)]">Os itens vinculados serão preservados e ficarão sem fornecedor.</p><div className="mt-5 flex justify-end gap-2"><Button variant="secondary" onClick={() => setPendingDelete(null)}>Cancelar</Button><Button variant="danger" onClick={() => void confirmDelete()}>Excluir</Button></div></> : null}</AnimatedModal>
  </ContentStack>;
}
