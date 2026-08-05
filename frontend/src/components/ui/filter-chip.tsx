type FilterChipProps = {
  label: string;
  active: boolean;
  onClick: () => void;
};

export function FilterChip({
  label,
  active,
  onClick,
}: FilterChipProps) {
  return (
    <button
      type="button"
      className={[
        "v7-motion-fast v7-pressable inline-flex h-[var(--control-height-compact)] items-center rounded-[var(--control-radius)] border px-3 text-xs font-medium",
        active
          ? "border-[var(--color-accent-border)] bg-[var(--color-accent-soft)] text-[var(--color-accent)]"
          : "border-[var(--border-subtle)] bg-[var(--surface-control)] text-[var(--text-muted)] hover:border-[var(--border-hover)] hover:bg-[var(--surface-hover)] hover:text-[var(--text-base)]",
      ].join(" ")}
      onClick={onClick}
    >
      {label}
    </button>
  );
}
