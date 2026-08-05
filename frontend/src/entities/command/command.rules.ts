import {
  getNextNumericId,
} from "../../shared/lib/identifiers";

import type {
  BarCommand,
  BarCommandItem,
  BarCommandStatus,
  OpenBarCommandInput,
  ResolvedBarCommandItemInput,
} from "./command.types";

export const barCommandStatusLabels: Record<
  BarCommandStatus,
  string
> = {
  open: "Em consumo",
  awaitingPayment: "Aguardando pagamento",
};

export function getBarCommandItemKey(
  catalogItemId: number
): string {
  return String(catalogItemId);
}

export function normalizeBarCommandName(
  name: string
): string {
  return name.trim().replace(/\s+/g, " ");
}

export function isOpenBarCommandInputComplete(
  input: OpenBarCommandInput
): boolean {
  return normalizeBarCommandName(
    input.name
  ).length > 0;
}

export function createBarCommand(
  commands: readonly BarCommand[],
  input: OpenBarCommandInput
): BarCommand[] {
  const id = getNextNumericId(commands);

  const command: BarCommand = {
    id,
    name: normalizeBarCommandName(
      input.name
    ),
    status: "open",
    openedAt: input.openedAt,
    items: [],
  };

  return [
    command,
    ...commands,
  ];
}

export function setBarCommandStatus(
  commands: readonly BarCommand[],
  commandId: number,
  status: BarCommandStatus
): BarCommand[] {
  return commands.map((command) =>
    command.id === commandId
      ? {
          ...command,
          status,
        }
      : command
  );
}

export function addBarCommandItem(
  commands: readonly BarCommand[],
  commandId: number,
  input: ResolvedBarCommandItemInput
): BarCommand[] {
  const itemKey = getBarCommandItemKey(
    input.catalogItemId
  );

  return commands.map((command) => {
    if (command.id !== commandId) {
      return command;
    }

    const existingItem = command.items.find(
      (item) => item.key === itemKey
    );

    if (existingItem) {
      return {
        ...command,

        items: command.items.map((item) =>
          item.key === itemKey
            ? {
                ...item,
                quantity:
                  item.quantity +
                  input.quantity,
              }
            : item
        ),
      };
    }

    const newItem: BarCommandItem = {
      key: itemKey,
      catalogItemId:
        input.catalogItemId,
      name: input.name,
      unitPrice: input.unitPrice,
      quantity: input.quantity,
    };

    return {
      ...command,
      items: [
        ...command.items,
        newItem,
      ],
    };
  });
}

export function updateBarCommandItemQuantity(
  commands: readonly BarCommand[],
  commandId: number,
  itemKey: string,
  quantity: number,
  unitPrice?: number
): BarCommand[] {
  return commands.map((command) => {
    if (command.id !== commandId) {
      return command;
    }

    if (quantity <= 0) {
      return {
        ...command,

        items: command.items.filter(
          (item) => item.key !== itemKey
        ),
      };
    }

    return {
      ...command,

      items: command.items.map((item) =>
        item.key === itemKey
          ? {
              ...item,
              quantity,
              ...(typeof unitPrice === "number"
                ? { unitPrice }
                : {}),
            }
          : item
      ),
    };
  });
}

export function removeBarCommandItem(
  commands: readonly BarCommand[],
  commandId: number,
  itemKey: string
): BarCommand[] {
  return commands.map((command) =>
    command.id === commandId
      ? {
          ...command,

          items: command.items.filter(
            (item) => item.key !== itemKey
          ),
        }
      : command
  );
}

export function removeBarCommand(
  commands: readonly BarCommand[],
  commandId: number
): BarCommand[] {
  return commands.filter(
    (command) => command.id !== commandId
  );
}

export function getBarCommandTotal(
  command: BarCommand
): number {
  return command.items.reduce(
    (total, item) =>
      total +
      item.unitPrice * item.quantity,
    0
  );
}

export function getBarCommandItemCount(
  command: BarCommand
): number {
  return command.items.reduce(
    (total, item) =>
      total + item.quantity,
    0
  );
}

export function getBarCommandSummary(
  command: BarCommand
): string {
  if (command.items.length === 0) {
    return "Nenhum item adicionado";
  }

  return command.items
    .map(
      (item) =>
        `${item.quantity}x ${item.name}`
    )
    .join(", ");
}

export function getBarCommandStatusLabel(
  status: BarCommandStatus
): string {
  return barCommandStatusLabels[status];
}
