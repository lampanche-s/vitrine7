type InfoFieldProps = {
  label: string;
  value: string;
};

export function InfoField({ label, value }: InfoFieldProps) {
  return (
    <div className="min-w-0 rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 py-2">
      <p className="text-[10px] font-medium uppercase tracking-normal text-[var(--text-subtle)]">
        {label}
      </p>

      <p className="mt-1 truncate text-sm font-medium text-[var(--text-base)]">
        {value}
      </p>
    </div>
  );
}
