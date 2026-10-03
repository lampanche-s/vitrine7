import { useEffect, useId, useRef, useState } from "react";
import { CalendarDays } from "lucide-react";
import {
  formatIsoDateForBrazil,
  maskBrazilianDate,
  parseBrazilianDate,
} from "../../shared/lib/brazilian-date";

type DateFieldProps = {
  label: string;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  className?: string;
};

export function DateField({ label, value, onChange, disabled = false, className = "" }: DateFieldProps) {
  const inputId = useId();
  const errorId = useId();
  const calendarRef = useRef<HTMLInputElement>(null);
  const lastEmitted = useRef(value);
  const [draft, setDraft] = useState(() => formatIsoDateForBrazil(value));
  const [blurred, setBlurred] = useState(false);

  useEffect(() => {
    if (value !== lastEmitted.current) {
      setDraft(formatIsoDateForBrazil(value));
      setBlurred(false);
    }
    lastEmitted.current = value;
  }, [value]);

  const validDate = parseBrazilianDate(draft);
  const invalid = draft !== "" && !validDate && (blurred || draft.length === 10);

  function updateDraft(nextDraft: string) {
    const masked = maskBrazilianDate(nextDraft);
    setDraft(masked);
    setBlurred(false);
    const nextValue = parseBrazilianDate(masked) ?? "";
    lastEmitted.current = nextValue;
    if (nextValue !== value) onChange(nextValue);
  }

  function openCalendar() {
    const input = calendarRef.current;
    if (!input) return;
    if (typeof input.showPicker === "function") input.showPicker();
    else input.click();
  }

  return (
    <div className={["grid gap-2", className].join(" ")}>
      <label htmlFor={inputId} className="text-[11px] font-medium uppercase tracking-normal text-[var(--text-subtle)]">
        {label}
      </label>
      <div className="relative">
        <input
          id={inputId}
          type="text"
          inputMode="numeric"
          autoComplete="off"
          placeholder="dd/mm/aaaa"
          value={draft}
          disabled={disabled}
          maxLength={10}
          aria-invalid={invalid}
          aria-describedby={invalid ? errorId : undefined}
          onChange={(event) => updateDraft(event.target.value)}
          onKeyDown={(event) => {
            const input = event.currentTarget;
            if (
              event.key === "Backspace" &&
              input.selectionStart === input.selectionEnd &&
              input.selectionStart !== null &&
              draft[input.selectionStart - 1] === "/"
            ) {
              event.preventDefault();
              input.setSelectionRange(input.selectionStart - 1, input.selectionStart - 1);
            }
          }}
          onBlur={() => setBlurred(true)}
          className="v7-motion-fast form-field-control form-clean-input w-full rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 pr-11 text-sm text-[var(--text-base)] placeholder:text-[var(--text-subtle)] focus:border-[var(--border-hover)] disabled:cursor-not-allowed disabled:opacity-50"
        />
        <button
          type="button"
          disabled={disabled}
          onClick={openCalendar}
          aria-label={`Escolher ${label.toLocaleLowerCase("pt-BR")} no calendário`}
          title="Escolher no calendário"
          className="absolute inset-y-0 right-0 flex w-10 items-center justify-center text-[var(--text-muted)] hover:text-[var(--text-base)] disabled:cursor-not-allowed disabled:opacity-50"
        >
          <CalendarDays size={17} aria-hidden="true" />
        </button>
        <input
          ref={calendarRef}
          type="date"
          lang="pt-BR"
          value={validDate ?? ""}
          disabled={disabled}
          tabIndex={-1}
          aria-hidden="true"
          onChange={(event) => {
            const nextValue = event.target.value;
            setDraft(formatIsoDateForBrazil(nextValue));
            setBlurred(false);
            lastEmitted.current = nextValue;
            onChange(nextValue);
          }}
          className="pointer-events-none absolute bottom-0 right-0 h-px w-px opacity-0"
        />
      </div>
      {invalid ? <span id={errorId} className="text-xs text-[var(--color-danger)]">Informe uma data válida em dd/mm/aaaa.</span> : null}
    </div>
  );
}
