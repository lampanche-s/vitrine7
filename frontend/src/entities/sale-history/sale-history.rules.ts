import type {
  BarSaleHistoryEntry,
} from "./sale-history.types";

export function getBarSaleHistoryTimestamp(
  entry: BarSaleHistoryEntry
): number {
  const timestamp = Date.parse(
    entry.completedAt ?? ""
  );

  return Number.isNaN(timestamp)
    ? 0
    : timestamp;
}
