import { useEffect, useState } from "react";
import { ContentStack, useToast } from "../../components/ui";
import { repositories } from "../../data/repositories";
import type { Supplier, SupplierDetail, SupplierInput } from "../../entities/supplier";
import { ModuleAccessGuard } from "../access";
import { SuppliersManager } from "./SuppliersManager";

export function SuppliersContent() { return <ModuleAccessGuard moduleId="bar"><SuppliersContentView /></ModuleAccessGuard>; }
function SuppliersContentView() {
  const [suppliers, setSuppliers] = useState<Supplier[]>([]); const [loading, setLoading] = useState(true); const { showToast } = useToast();
  useEffect(() => { let active = true; repositories.suppliers.list().then((data) => { if (active) setSuppliers(data); }).catch((error: unknown) => showToast({ title: error instanceof Error ? error.message : "Não foi possível carregar os fornecedores.", variant: "error" })).finally(() => { if (active) setLoading(false); }); return () => { active = false; }; }, [showToast]);
  async function create(input: SupplierInput) { try { const supplier = await repositories.suppliers.create(input); setSuppliers((current) => [supplier, ...current]); return true; } catch (error) { showToast({ title: error instanceof Error ? error.message : "Não foi possível cadastrar o fornecedor.", variant: "error" }); return false; } }
  async function update(id: number, input: SupplierInput) { try { const supplier = await repositories.suppliers.update(id, input); setSuppliers((current) => current.map((item) => item.id === id ? supplier : item)); return true; } catch (error) { showToast({ title: error instanceof Error ? error.message : "Não foi possível atualizar o fornecedor.", variant: "error" }); return false; } }
  async function remove(id: number) { try { await repositories.suppliers.remove(id); setSuppliers((current) => current.filter((item) => item.id !== id)); return true; } catch (error) { showToast({ title: error instanceof Error ? error.message : "Não foi possível excluir o fornecedor.", variant: "error" }); return false; } }
  async function detail(id: number): Promise<SupplierDetail | null> { try { return await repositories.suppliers.detail(id); } catch (error) { showToast({ title: error instanceof Error ? error.message : "Não foi possível carregar o fornecedor.", variant: "error" }); return null; } }
  return <ContentStack>{loading ? <p className="text-sm text-[var(--text-subtle)]">Carregando fornecedores...</p> : <SuppliersManager suppliers={suppliers} onCreate={create} onUpdate={update} onRemove={remove} onLoadDetail={detail}/>}</ContentStack>;
}
