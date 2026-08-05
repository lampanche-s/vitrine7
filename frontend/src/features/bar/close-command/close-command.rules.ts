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

  const historyEntry: BarSaleHistoryEntry = {
    id: getNextNumericId(historyEntries),
    source: "command",
    origin: command.name,
    description: getBarCommandSummary(command),
    receiptItems: command.items.map((item) => ({
      quantity: item.quantity,
      name: item.name,
      unitPrice: item.unitPrice,
      total: item.unitPrice * item.quantity,
    })),
    amount: getBarCommandTotal(command),
    method: input.payment,
    document: input.document,
    cashReceived:
      input.payment === "Dinheiro"
        ? input.cashReceived ?? getBarCommandTotal(command)
        : undefined,
    cashChange:
      input.payment === "Dinheiro"
        ? Math.max(
            0,
            (input.cashReceived ?? getBarCommandTotal(command)) -
              getBarCommandTotal(command)
          )
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
