import type { ReactNode } from "react";

export type StatusBadgeTone =
  | "neutral"
  | "success"
  | "danger"
  | "warning";

const toneClassNames: Record<StatusBadgeTone, string> = {
  neutral:
    "border-[var(--border-subtle)] bg-[var(--surface-control)] text-[var(--text-muted)]",

  success:
    "border-[var(--color-success-border)] bg-[var(--color-success-soft)] text-[var(--color-success)]",

  danger:
    "border-[var(--color-danger-border)] bg-[var(--color-danger-soft)] text-[var(--color-danger)]",

  warning:
    "border-[var(--color-accent-border)] bg-[var(--color-accent-soft)] text-[var(--color-accent)]",
};

export function StatusBadge({
  children,
  tone = "neutral",
}: {
  children: ReactNode;
  tone?: StatusBadgeTone;
}) {
  return (
    <span
      className={[
        "inline-flex min-h-6 items-center rounded-[var(--control-radius)] border px-2.5 py-1 text-[11px] font-medium",
        toneClassNames[tone],
      ].join(" ")}
    >
      {children}
    </span>
  );
}
