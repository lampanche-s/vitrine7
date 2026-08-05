export function TextAreaField({
  label,
  value,
  onChange,
  placeholder,
  disabled = false,
  className = "",
}: {
  label?: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
}) {
  return (
    <label className="grid gap-2">
      {label ? (
        <span className="text-[11px] font-medium uppercase tracking-normal text-[var(--text-subtle)]">
          {label}
        </span>
      ) : null}

      <textarea
        value={value}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        className={[
          "v7-motion-fast form-textarea-control form-clean-input premium-scroll w-full resize-none rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 py-3 text-sm text-[var(--text-base)] placeholder:text-[var(--text-subtle)] focus:border-[var(--border-hover)] disabled:cursor-not-allowed disabled:opacity-50",
          className,
        ].join(" ")}
      />
    </label>
  );
}
