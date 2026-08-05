type EmptyStateProps = {
  message: string;
  variant?: "card" | "table";
};

export function EmptyState({
  message,
  variant = "card",
}: EmptyStateProps) {
  const className =
    variant === "table"
      ? "bg-[var(--surface-control)] px-4 py-5 text-sm text-[var(--text-muted)]"
      : "rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] p-4 text-sm text-[var(--text-muted)]";

  return <div className={className}>{message}</div>;
}
