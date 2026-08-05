import {
  getBarSaleHistoryTimestamp,
} from "../../../entities/sale-history";

import type {
  BarSaleHistoryEntry,
} from "../../../entities/sale-history";

const BAR_REPORT_HISTORY_LIMIT = 5;

export function getLatestRows(entries: BarSaleHistoryEntry[]) {
  return entries
    .map((entry, index) => ({
      entry,
      index,
    }))
    .sort((firstItem, secondItem) => {
      const dateDifference =
        getBarSaleHistoryTimestamp(secondItem.entry) -
        getBarSaleHistoryTimestamp(firstItem.entry);

      return dateDifference === 0
        ? firstItem.index - secondItem.index
        : dateDifference;
    })
    .slice(0, BAR_REPORT_HISTORY_LIMIT)
    .map((item) => item.entry);
}
