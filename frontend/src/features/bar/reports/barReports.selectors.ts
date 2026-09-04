import {
  getBarSaleHistoryTimestamp,
} from "../../../entities/sale-history";

import type {
  BarSaleHistoryEntry,
} from "../../../entities/sale-history";

const BAR_REPORT_HISTORY_LIMIT = 5;

export function paymentLabel(method: string): string {
  if (method.includes(",")) {
    return method
      .split(",")
      .map((entry) => paymentLabel(entry.trim()))
      .join(" + ");
  }

  switch (method) {
    case "CASH":
      return "Dinheiro";
    case "PIX":
      return "Pix";
    case "CREDIT_CARD":
      return "Crédito";
    case "DEBIT_CARD":
      return "Débito";
    case "MULTIPLE":
      return "Múltiplas";
    default:
      return method || "Não informado";
  }
}

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

export function createReportRequestGuard() {
  let activeRequest = 0;

  return {
    begin() {
      const request = ++activeRequest;
      return () => request === activeRequest;
    },
    invalidate() {
      activeRequest += 1;
    },
  };
}
