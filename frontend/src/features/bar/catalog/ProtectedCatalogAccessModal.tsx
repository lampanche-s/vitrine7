import type { FormEvent } from "react";
import { AnimatedModal, Button, TextField } from "../../../components/ui";

type Props = { open: boolean; password: string; error: string; onPasswordChange: (password: string) => void; onCancel: () => void; onConfirm: () => void; };

export function ProtectedCatalogAccessModal(props: Props) {
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); props.onConfirm(); }
  return <AnimatedModal open={props.open} onClose={props.onCancel} labelledBy="catalog-access-title" panelClassName="w-full max-w-[440px] overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"><form onSubmit={submit}><h2 id="catalog-access-title" className="text-[var(--font-size-section-title)] font-semibold text-[var(--text-base)]">Acesso ao catálogo</h2><p className="mt-1 text-sm text-[var(--text-muted)]">Informe a senha para continuar.</p><div className="mt-5 grid gap-2"><TextField label="Senha" type="password" value={props.password} autoFocus onChange={props.onPasswordChange} />{props.error ? <p className="text-sm text-[var(--color-danger)]" role="alert">{props.error}</p> : null}</div><div className="mt-5 flex justify-end gap-2 border-t border-[var(--border-subtle)] pt-5"><Button variant="secondary" onClick={props.onCancel}>Cancelar</Button><Button type="submit" variant="primary">Confirmar</Button></div></form></AnimatedModal>;
}
