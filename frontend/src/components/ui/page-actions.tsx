import type {
  PropsWithChildren,
} from "react";

export function PageActions({
  children,
}: PropsWithChildren) {
  return (
    <div className="flex shrink-0 flex-wrap items-center justify-start gap-2">
      {children}
    </div>
  );
}
