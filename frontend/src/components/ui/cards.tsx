import type { ReactNode } from "react";

export function PremiumCard({
  children,
  className = "",
  contentClassName = "",
}: {
  children: ReactNode;
  className?: string;
  contentClassName?: string;
}) {
  return (
    <div
      className={[
        "v7-motion-soft premium-card relative min-h-0 overflow-visible rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-card)] p-4 shadow-[var(--shadow-panel)]",
        className,
      ].join(" ")}
    >
      <div className={["relative min-h-0", contentClassName].join(" ")}>
        {children}
      </div>
    </div>
  );
}
