import {
  Search,
} from "lucide-react";

export function SearchField({
  value,
  onChange,
  placeholder,
  className = "",
  disabled = false,
}: {
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  className?: string;
  disabled?: boolean;
}) {
  return (
    <label
      className={[
        "v7-motion-fast search-field-control flex items-center gap-3 rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 text-sm text-[var(--text-muted)] focus-within:border-[var(--border-hover)]",
        disabled
          ? "cursor-not-allowed opacity-50"
          : "",
        className,
      ].join(" ")}
    >
      <Search
        className="h-4 w-4 shrink-0"
        aria-hidden="true"
      />

      <input
        value={value}
        disabled={disabled}
        onChange={(event) =>
          onChange(event.target.value)
        }
        placeholder={placeholder}
        className="search-clean-input min-w-0 flex-1 appearance-none border-0 bg-transparent text-sm text-[var(--text-base)] shadow-none outline-none placeholder:text-[var(--text-subtle)] focus:border-0 focus:outline-none focus:ring-0 focus-visible:outline-none"
        style={{
          border: 0,
          outline: "none",
          boxShadow: "none",
        }}
      />
    </label>
  );
}
