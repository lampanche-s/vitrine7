export function TextField({
  label,
  value,
  onChange,
  placeholder,
  type = "text",
  disabled = false,
  min,
  max,
  step,
  className = "",
}: {
  label?: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  type?:
    | "text"
    | "password"
    | "email"
    | "number"
    | "tel"
    | "date";
  disabled?: boolean;
  min?: number;
  max?: number;
  step?: number;
  className?: string;
}) {
  return (
    <label className="grid gap-2">
      {label ? (
        <span className="text-[11px] font-medium uppercase tracking-normal text-[var(--text-subtle)]">
          {label}
        </span>
      ) : null}

      <input
        type={type}
        value={value}
        disabled={disabled}
        min={min}
        max={max}
        step={step}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        className={[
          "v7-motion-fast form-field-control form-clean-input w-full rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 text-sm text-[var(--text-base)] placeholder:text-[var(--text-subtle)] focus:border-[var(--border-hover)] disabled:cursor-not-allowed disabled:opacity-50",
          className,
        ].join(" ")}
      />
    </label>
  );
}
