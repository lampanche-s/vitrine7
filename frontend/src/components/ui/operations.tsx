export function SubSectionTabs({
  options,
  active,
  onChange,
}: {
  options: {
    id: string;
    label: string;
    description?: string;
  }[];
  active: string;
  onChange: (id: string) => void;
}) {
  return (
    <div
      className="grid gap-2"
      style={{
        gridTemplateColumns: `repeat(${options.length}, minmax(0, 1fr))`,
      }}
    >
      {options.map((option) => {
        const selected = active === option.id;

        return (
          <button
            key={option.id}
            type="button"
            onClick={() => onChange(option.id)}
            className={[
              "relative flex min-h-12 items-center rounded-[4px] border px-3 py-2.5 text-left transition",
              selected
                ? "border-[var(--border-hover)] bg-[var(--surface-hover)] text-[var(--text-base)] shadow-[inset_0_0_0_1px_rgba(255,255,255,0.025)]"
                : "border-[var(--border-subtle)] bg-[var(--surface-control)] text-[var(--text-muted)] hover:bg-[var(--surface-raised)] hover:text-[var(--text-base)]",
            ].join(" ")}
          >
            <div className="min-w-0">
              <p className="truncate text-[13px] font-semibold leading-none">
                {option.label}
              </p>

              {option.description ? (
                <p className="mt-1.5 truncate text-xs font-normal leading-4 text-[var(--text-subtle)]">
                  {option.description}
                </p>
              ) : null}
            </div>
          </button>
        );
      })}
    </div>
  );
}
