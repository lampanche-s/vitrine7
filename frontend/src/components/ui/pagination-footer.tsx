import {
  ChevronLeft,
  ChevronRight,
} from "lucide-react";

import {
  Button,
} from "./button";

export function PaginationFooter({
  page,
  totalPages,
  totalItems,
  first,
  last,
  onPrevious,
  onNext,
}: {
  page: number;
  totalPages: number;
  totalItems?: number;
  first?: boolean;
  last?: boolean;
  onPrevious: () => void;
  onNext: () => void;
}) {
  if (totalPages <= 1) {
    return null;
  }

  const isFirstPage =
    first ?? page <= 1;

  const isLastPage =
    last ?? page >= totalPages;

  return (
    <div className="pagination-stable flex flex-col gap-3 border-t border-[var(--border-subtle)] pt-4 sm:flex-row sm:items-center sm:justify-between">
      <p className="text-xs text-[var(--text-muted)]">
        Página{" "}
        <span className="font-medium text-[var(--text-base)]">
          {page}
        </span>{" "}
        de{" "}
        <span className="font-medium text-[var(--text-base)]">
          {totalPages}
        </span>

        {typeof totalItems === "number" ? (
          <>
            {" "}·{" "}
            <span className="font-medium text-[var(--text-base)]">
              {totalItems}
            </span>{" "}
            registros
          </>
        ) : null}
      </p>

      <div className="flex items-center gap-2">
        <Button
          size="compact"
          variant="secondary"
          disabled={isFirstPage}
          onClick={onPrevious}
          leadingIcon={<ChevronLeft />}
        >
          Anterior
        </Button>

        <Button
          size="compact"
          variant="secondary"
          disabled={isLastPage}
          onClick={onNext}
          trailingIcon={<ChevronRight />}
        >
          Próxima
        </Button>
      </div>
    </div>
  );
}
