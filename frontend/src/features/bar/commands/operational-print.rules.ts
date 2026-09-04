import type {
  BarCommand,
  BarCommandItem,
} from "../../../entities/command";

export function canPrintItems(command: BarCommand): boolean {
  return canPrintType(command, "ITEM");
}

export function canPrintServices(command: BarCommand): boolean {
  return canPrintType(command, "SERVICE");
}

export function canPrintLine(
  command: BarCommand,
  line: BarCommandItem
): boolean {
  return (
    command.status === "open" &&
    typeof line.lineId === "number" &&
    line.lineId > 0 &&
    line.quantity > 0 &&
    (line.entryType === "ITEM" || line.entryType === "SERVICE")
  );
}

export function operationalPrintLineLabel(
  line: BarCommandItem
): string | null {
  if (line.entryType === "ITEM") {
    return "Imprimir este item";
  }

  if (line.entryType === "SERVICE") {
    return "Imprimir este serviço";
  }

  return null;
}

function canPrintType(
  command: BarCommand,
  type: "ITEM" | "SERVICE"
): boolean {
  return (
    command.status === "open" &&
    command.items.some(
      (line) => line.entryType === type && line.quantity > 0
    )
  );
}
