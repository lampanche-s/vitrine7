import type {
  BarCommand,
} from "../../../entities/command";

export function canPrintPrePaymentNote(
  command: Pick<BarCommand, "status" | "items">
) {
  return command.status === "open" && command.items.length > 0;
}
