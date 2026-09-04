import {
  getBarCommandSummary,
  getBarCommandTotal,
  removeBarCommand,
} from "../../../entities/command";

import type {
  BarCommand,
  CloseBarCommandInput,
} from "../../../entities/command";

import type {
  BarCatalogItem,
} from "../../../entities/catalog-item";

import type {
  BarSaleHistoryEntry,
} from "../../../entities/sale-history";

import {
  getNextNumericId,
} from "../../../shared/lib/identifiers";

export type CloseBarCommandResult = {
  commands: BarCommand[];
  historyEntries: BarSaleHistoryEntry[];
  closedCommand: BarCommand;
  historyEntry: BarSaleHistoryEntry;
};

export function shouldRequestVehicleDetails(
  command: BarCommand,
  catalogEntries: readonly BarCatalogItem[]
) {
  return command.clientId == null && command.items.some((item) =>
    catalogEntries.some(
      (catalogEntry) =>
        catalogEntry.id === item.catalogItemId &&
        catalogEntry.type === "SERVICE"
    )
  );
}

export function closeBarCommand(
  commands: readonly BarCommand[],
  historyEntries:
    readonly BarSaleHistoryEntry[],
  input: CloseBarCommandInput
): CloseBarCommandResult {
  const command = commands.find(
    (currentCommand) =>
      currentCommand.id === input.commandId
  );

  if (!command) {
    throw new Error(
      `Comanda ${input.commandId} não encontrada.`
    );
  }

  if (
    command.status !==
    "awaitingPayment"
  ) {
    throw new Error(
      "Envie a comanda para pagamento antes de fechá-la."
    );
  }

  if (command.items.length === 0) {
    throw new Error(
      "Não é possível fechar uma comanda sem itens."
    );
  }

  const payments = input.payments?.length
    ? input.payments
    : [{
        method: input.payment ?? "Dinheiro",
        amount: getBarCommandTotal(command),
        cashReceived: input.cashReceived,
      }];
  const method = payments.length > 1
    ? "Múltiplas" as const
    : payments[0].method;
  const cashReceived = payments
    .filter((part) => part.method === "Dinheiro")
    .reduce((sum, part) => sum + (part.cashReceived ?? part.amount), 0);
  const cashAmount = payments
    .filter((part) => part.method === "Dinheiro")
    .reduce((sum, part) => sum + part.amount, 0);

  const historyEntry: BarSaleHistoryEntry = {
    id: getNextNumericId(historyEntries),
    origin: command.name,
    description: getBarCommandSummary(command),
    receiptItems: command.items.map((item) => ({
      quantity: item.quantity,
      name: item.name,
      unitPrice: item.unitPrice,
      total: item.unitPrice * item.quantity,
    })),
    amount: getBarCommandTotal(command),
    method,
    document: input.document,
    cashReceived: cashReceived > 0 ? cashReceived : undefined,
    cashChange: cashReceived > 0
      ? Math.max(0, cashReceived - cashAmount)
      : undefined,
    time: input.time,
  };

  return {
    commands: removeBarCommand(
      commands,
      command.id
    ),

    historyEntries: [
      historyEntry,
      ...historyEntries,
    ],

    closedCommand: {
      ...command,

      items: command.items.map((item) => ({
        ...item,
      })),
    },

    historyEntry: {
      ...historyEntry,
    },
  };
}
