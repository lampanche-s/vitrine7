import type {
  LavaFinanceEntry,
  SettleLavaFinanceEntryInput,
} from "./payment.types";

export function settleLavaFinanceEntry(
  entries: readonly LavaFinanceEntry[],
  input: SettleLavaFinanceEntryInput
): LavaFinanceEntry[] {
  return entries.map((entry) => {
    if (
      entry.id !== input.entryId ||
      entry.status !== "pending"
    ) {
      return entry;
    }

    return {
      ...entry,
      status: "paid",
      method: input.method,
      document: input.document,
      time: input.time,
    };
  });
}
