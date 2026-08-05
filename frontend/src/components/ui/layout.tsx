import type { ReactNode } from "react";

export function ContentStack({
  children,
  stretch = false,
  className = "",
}: {
  children: ReactNode;
  stretch?: boolean;
  className?: string;
}) {
  return (
    <div
      className={[
        stretch
          ? "v7-tab-fill gap-4 pb-2"
          : "v7-tab-fill gap-4 pb-2",
        "[&>.relative:last-child]:flex-1",
        className,
      ].join(" ")}
    >
      {children}
    </div>
  );
}

export function SectionTitle({
  title,
  subtitle,
  compact = false,
}: {
  title: string;
  subtitle?: string;
  compact?: boolean;
}) {
  return (
    <div className={compact ? "mb-4" : ""}>
      <h3
        className={[
          "font-semibold uppercase text-[var(--text-base)]",
          compact
            ? "text-sm tracking-[0.055em]"
            : "text-lg tracking-[0.045em]",
        ].join(" ")}
      >
        {title}
      </h3>

      {subtitle ? (
        <p className="mt-1 text-sm leading-6 text-[var(--text-muted)]">
          {subtitle}
        </p>
      ) : null}
    </div>
  );
}
