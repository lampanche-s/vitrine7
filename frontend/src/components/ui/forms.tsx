import {
  useEffect,
  useRef,
  useState,
} from "react";
import { Check, ChevronDown } from "lucide-react";

export function AdminInput({
  label,
  value,
  onChange,
  type = "text",
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
}) {
  return (
    <label className="block">
      <span className="mb-2 block text-xs font-medium uppercase tracking-normal text-[var(--text-subtle)]">
        {label}
      </span>

      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="v7-motion-fast h-[var(--control-height)] w-full rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 text-sm text-[var(--text-base)] outline-none hover:border-[var(--border-hover)] focus:border-[var(--border-hover)]"
      />
    </label>
  );
}

export function AdminSelect({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: string[];
  onChange: (value: string) => void;
}) {
  return (
    <DropdownField
      label={label}
      value={value}
      options={options}
      onChange={onChange}
    />
  );
}

export function DropdownField({
  label,
  value,
  options,
  onChange,
  hideLabel = false,
  placement = "bottom",
}: {
  label: string;
  value: string;
  options: string[];
  onChange: (value: string) => void;
  hideLabel?: boolean;
  placement?: "top" | "bottom";
}) {
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement | null>(null);

  const normalizedOptions = Array.from(new Set(options));

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      if (
        wrapperRef.current &&
        !wrapperRef.current.contains(event.target as Node)
      ) {
        setOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setOpen(false);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, []);

  function handleSelect(option: string) {
    onChange(option);
    setOpen(false);
  }

  return (
    <div ref={wrapperRef} className="relative grid gap-2">
      {!hideLabel && (
        <span className="text-[11px] font-medium uppercase tracking-normal text-[var(--text-subtle)]">
          {label}
        </span>
      )}

      <button
        type="button"
        aria-label={hideLabel ? label : undefined}
        onClick={() => setOpen((current) => !current)}
        className="v7-motion-fast v7-pressable flex h-[var(--control-height)] w-full items-center justify-between gap-3 rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 text-left text-sm text-[var(--text-base)] outline-none hover:border-[var(--border-hover)] focus:border-[var(--border-hover)]"
      >
        <span className="truncate text-[var(--text-base)]">{value}</span>

        <ChevronDown
          aria-hidden="true"
          className={[
            "h-4 w-4 shrink-0 text-[var(--text-muted)] transition duration-[var(--motion-fast)] ease-[var(--motion-ease)]",
            open ? "rotate-180 text-[var(--color-accent)]" : "",
          ].join(" ")}
        />
      </button>

      {open && (
        <div
          className={[
            "dropdown-panel absolute left-0 right-0 z-[380] overflow-hidden rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-card-strong)] shadow-[var(--shadow-dropdown)]",
            placement === "top"
              ? "bottom-[calc(100%+6px)]"
              : "top-[calc(100%+6px)]",
          ].join(" ")}
        >
          <div className="premium-scroll max-h-[176px] overflow-y-auto py-1">
            {normalizedOptions.map((option) => {
              const active = option === value;

              return (
                <button
                  key={option}
                  type="button"
                  onClick={() => handleSelect(option)}
                  className={[
                    "v7-motion-fast flex w-full items-center justify-between gap-3 px-3 py-2 text-left text-sm",
                    active
                      ? "bg-[var(--surface-hover)] text-[var(--text-base)]"
                      : "text-[var(--text-muted)] hover:bg-[var(--surface-raised)] hover:text-[var(--text-base)]",
                  ].join(" ")}
                >
                  <span className="truncate font-medium">{option}</span>

                  {active && (
                    <Check
                      className="h-4 w-4 shrink-0 text-[var(--color-accent)]"
                      aria-hidden="true"
                    />
                  )}
                </button>
              );
            })}

            {normalizedOptions.length === 0 ? (
              <p className="px-3 py-3 text-sm text-[var(--text-muted)]">
                Nenhuma opção disponível.
              </p>
            ) : null}
          </div>
        </div>
      )}
    </div>
  );
}
