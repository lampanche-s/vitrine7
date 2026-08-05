import type { ElementType } from "react";
import { SwitchVisual } from "./switch";

export function AdminToggle({
  title,
  text,
  checked,
  onClick,
}: {
  title: string;
  text: string;
  checked: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={checked}
      className="group v7-motion-fast v7-pressable mb-3 flex w-full items-center justify-between gap-4 rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] p-4 text-left hover:bg-[var(--surface-raised)] last:mb-0"
    >
      <div>
        <p className="text-sm font-medium text-[var(--text-base)]">{title}</p>
        <p className="mt-1 text-xs leading-5 text-[var(--text-muted)]">{text}</p>
      </div>

      <SwitchVisual checked={checked} />
    </button>
  );
}

export function ModuleToggle({
  icon: Icon,
  title,
  active,
  onClick,
}: {
  icon: ElementType;
  title: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={[
        "v7-motion-fast v7-pressable mb-3 flex w-full items-center justify-between rounded-[4px] border border-[var(--border-subtle)] p-4 text-left last:mb-0",
        active ? "bg-[var(--surface-raised)]" : "bg-[var(--surface-control)] opacity-55",
      ].join(" ")}
    >
      <div className="flex items-center gap-3">
        <div className="grid h-10 w-10 place-items-center rounded-[4px] bg-[var(--surface-card-strong)]">
          <Icon className="h-4 w-4 text-[var(--text-base)]" aria-hidden="true" />
        </div>

        <div>
          <p className="text-sm font-medium text-[var(--text-base)]">{title}</p>
          <p className="mt-1 text-xs text-[var(--text-muted)]">{active ? "Ativo" : "Desativado"}</p>
        </div>
      </div>

      <span className={active ? "text-xs font-medium text-[var(--text-base)]" : "text-xs text-[var(--text-subtle)]"}>
        {active ? "ON" : "OFF"}
      </span>
    </button>
  );
}
