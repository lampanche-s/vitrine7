type CounterCardProps = {
  label: string;
  value: number;
};

export function CounterCard({ label, value }: CounterCardProps) {
  return (
    <div className="rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 py-2 text-center">
      <p className="text-lg font-semibold tracking-[-0.04em] text-[var(--text-base)]">
        {value.toString().padStart(2, "0")}
      </p>

      <p className="mt-1 text-[9px] font-medium uppercase tracking-normal text-[var(--text-subtle)]">
        {label}
      </p>
    </div>
  );
}
