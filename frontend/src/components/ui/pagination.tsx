import {
  ArrowLeft,
  ArrowRight,
} from "lucide-react";

import {
  Button,
} from "./button";

type PaginationProps = {
  currentPage: number;
  totalPages: number;
  totalItems: number;
  pageStart: number;
  pageSize: number;
  onPrevious: () => void;
  onNext: () => void;
};

export function Pagination({
  currentPage,
  totalPages,
  totalItems,
  pageStart,
  pageSize,
  onPrevious,
  onNext,
}: PaginationProps) {
  const firstItem =
    totalItems === 0
      ? 0
      : pageStart + 1;

  const lastItem = Math.min(
    pageStart + pageSize,
    totalItems
  );

  return (
    <div className="pagination-stable mt-4 flex flex-col gap-3 border-t border-[var(--border-subtle)] pt-4 sm:flex-row sm:items-center sm:justify-between">
      <p className="text-xs text-[var(--text-muted)]">
        {firstItem.toString().padStart(2, "0")}-
        {lastItem.toString().padStart(2, "0")} de{" "}
        {totalItems.toString().padStart(2, "0")}
      </p>

      <div className="flex items-center gap-2">
        <Button
          size="icon"
          variant="secondary"
          disabled={currentPage === 1}
          onClick={onPrevious}
          leadingIcon={<ArrowLeft />}
          aria-label="Página anterior"
          title="Voltar"
        />

        <span className="flex h-[var(--control-height-compact)] min-w-[48px] items-center justify-center rounded-[var(--control-radius)] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 text-xs font-medium text-[var(--text-muted)]">
          {currentPage}/{Math.max(totalPages, 1)}
        </span>

        <Button
          size="icon"
          variant="secondary"
          disabled={
            currentPage >= totalPages ||
            totalPages === 0
          }
          onClick={onNext}
          leadingIcon={<ArrowRight />}
          aria-label="Próxima página"
          title="Avançar"
        />
      </div>
    </div>
  );
}
