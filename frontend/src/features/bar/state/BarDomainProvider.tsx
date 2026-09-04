import {
  useCallback,
  useEffect,
  useReducer,
  useState,
  type ReactNode,
} from "react";

import type {
  BarCatalogItem,
  BarCatalogItemInput,
} from "../../../entities/catalog-item";

import type {
  AddBarCommandItemInput,
  BarCommand,
  BarCommandStatus,
  CloseBarCommandInput,
  OpenBarCommandInput,
  VoucherBarCommandInput,
} from "../../../entities/command";

import type {
  BarHistoryPageRequest,
  BarHistoryPageResult,
  BarRepository,
} from "../../../data/contracts";

import {
  useToast,
} from "../../../components/ui";

import {
  useMutationLock,
} from "../../../shared/hooks/useMutationLock";

import {
  BarDomainContext,
} from "./bar-domain.context";

import {
  barDomainReducer,
} from "./bar-domain.reducer";

import type {
  BarDomainState,
} from "./bar-domain.types";

type BarDomainProviderProps = {
  children: ReactNode;
  initialState: BarDomainState;
  repository: BarRepository;
};

function getErrorMessage(error: unknown): string {
  return error instanceof Error
    ? error.message
    : "Não foi possível concluir a operação.";
}

export function BarDomainProvider({
  children,
  initialState,
  repository,
}: BarDomainProviderProps) {
  const [state, dispatch] = useReducer(
    barDomainReducer,
    initialState
  );
  const [isLoading, setIsLoading] =
    useState(false);
  const [error, setError] =
    useState<string | null>(null);
  const {
    showErrorToast,
  } = useToast();
  const {
    isMutating,
    runMutation,
  } = useMutationLock();

  const reload = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      const snapshot =
        await repository.getSnapshot();

      dispatch({
        type: "repository/snapshot-loaded",
        payload: snapshot,
      });
    } catch (currentError) {
      const message =
        getErrorMessage(currentError);

      setError(message);
      showErrorToast(currentError, {
        title: "Não foi possível carregar os dados",
        dedupeKey: `bar-reload|${message}`,
      });
    } finally {
      setIsLoading(false);
    }
  }, [repository, showErrorToast]);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  useEffect(() => {
    queueMicrotask(() => {
      void reload();
    });
  }, [reload]);

  function executeMutation<T>(
    operation: () => Promise<T>,
    ignoredResult: T
  ): Promise<T> {
    return runMutation(
      async () => {
        setError(null);

        try {
          return await operation();
        } catch (currentError) {
          const message =
            getErrorMessage(currentError);

          setError(message);
          showErrorToast(currentError, {
            title: "Não foi possível concluir a operação",
            dedupeKey: `bar-mutation|${message}`,
          });

          return ignoredResult;
        }
      },
      ignoredResult
    );
  }

  const listHistory = useCallback(
    async (
      input: BarHistoryPageRequest
    ): Promise<BarHistoryPageResult | null> => {
      setError(null);

      try {
        return await repository.listHistory(input);
      } catch (currentError) {
        const message =
          getErrorMessage(currentError);

        setError(message);
        showErrorToast(currentError, {
          title: "Não foi possível carregar o histórico",
          dedupeKey: `bar-history|${message}`,
        });

        return null;
      }
    },
    [repository, showErrorToast]
  );

  function openCommand(
    input: OpenBarCommandInput
  ): Promise<BarCommand | null> {
    return executeMutation(
      async () => {
        const command =
          await repository.openCommand(input);

        dispatch({
          type: "command/created",
          payload: command,
        });
        return command;
      },
      null
    );
  }

  function reopenCommand(
    commandId: number
  ): Promise<BarCommand | null> {
    return executeMutation(
      async () => {
        const command = await repository.reopenCommand(commandId);
        dispatch({
          type: "command/created",
          payload: command,
        });
        return command;
      },
      null
    );
  }

  function setCommandStatus(
    commandId: number,
    status: BarCommandStatus
  ): Promise<boolean> {
    return executeMutation(
      async () => {
        const command =
          await repository.setCommandStatus(
            commandId,
            status
          );

        dispatch({
          type: "command/updated",
          payload: command,
        });
        return true;
      },
      false
    );
  }

  function printPrePaymentNote(
    commandId: number
  ): Promise<boolean> {
    return executeMutation(
      async () => {
        await repository.printPrePaymentNote(commandId);
        return true;
      },
      false
    );
  }

  function printItems(tabId: number): Promise<boolean> {
    return executeMutation(
      async () => {
        await repository.printItems(tabId);
        return true;
      },
      false
    );
  }

  function printServices(tabId: number): Promise<boolean> {
    return executeMutation(
      async () => {
        await repository.printServices(tabId);
        return true;
      },
      false
    );
  }

  function printLine(
    tabId: number,
    lineId: number
  ): Promise<boolean> {
    return executeMutation(
      async () => {
        await repository.printLine(tabId, lineId);
        return true;
      },
      false
    );
  }

  function addCommandItem(
    commandId: number,
    input: AddBarCommandItemInput
  ): Promise<boolean> {
    return executeMutation(
      async () => {
        const command =
          await repository.addCommandItem(
            commandId,
            input
          );

        dispatch({
          type: "command/updated",
          payload: command,
        });
        return true;
      },
      false
    );
  }

  function updateCommandItemQuantity(
    commandId: number,
    itemKey: string,
    quantity: number,
    unitPrice?: number
  ): Promise<boolean> {
    return executeMutation(
      async () => {
        const command =
          await repository.updateCommandItemQuantity(
            commandId,
            itemKey,
            quantity,
            unitPrice
          );

        dispatch({
          type: "command/updated",
          payload: command,
        });
        return true;
      },
      false
    );
  }

  function removeCommandItem(
    commandId: number,
    itemKey: string
  ): Promise<boolean> {
    return executeMutation(
      async () => {
        const command =
          await repository.removeCommandItem(
            commandId,
            itemKey
          );

        dispatch({
          type: "command/updated",
          payload: command,
        });
        return true;
      },
      false
    );
  }

  function cancelCommand(
    commandId: number
  ): Promise<boolean> {
    return executeMutation(
      async () => {
        await repository.cancelCommand(commandId);
        dispatch({
          type: "command/removed",
          payload: commandId,
        });
        return true;
      },
      false
    );
  }

  function closeCommand(
    input: CloseBarCommandInput
  ): Promise<BarCommand | null> {
    return executeMutation(
      async () => {
        const result =
          await repository.closeCommand(input);

        dispatch({
          type: "command/closed",
          payload: {
            commandId: result.closedCommand.id,
            historyEntry: result.historyEntry,
            catalogEntries: result.catalogEntries,
          },
        });
        return result.closedCommand;
      },
      null
    );
  }

  function closeVoucher(input: VoucherBarCommandInput): Promise<BarCommand | null> {
    return executeMutation(async () => {
      const command = await repository.closeVoucher(input);
      dispatch({ type: "command/removed", payload: command.id });
      const snapshot = await repository.getSnapshot();
      dispatch({ type: "repository/snapshot-loaded", payload: snapshot });
      return command;
    }, null);
  }

  function createCatalogEntry(
    input: BarCatalogItemInput
  ): Promise<BarCatalogItem | null> {
    return executeMutation(
      async () => {
        const entry =
          await repository.createCatalogEntry(input);

        dispatch({
          type: "catalog/created",
          payload: entry,
        });
        return entry;
      },
      null
    );
  }

  function updateCatalogEntry(
    entryId: number,
    input: BarCatalogItemInput
  ): Promise<BarCatalogItem | null> {
    return executeMutation(
      async () => {
        const entry =
          await repository.updateCatalogEntry(
            entryId,
            input
          );

        dispatch({
          type: "catalog/updated",
          payload: entry,
        });
        return entry;
      },
      null
    );
  }

  function removeCatalogEntry(
    entryId: number
  ): Promise<boolean> {
    return executeMutation(
      async () => {
        await repository.removeCatalogEntry(entryId);
        dispatch({
          type: "catalog/removed",
          payload: entryId,
        });
        return true;
      },
      false
    );
  }

  return (
    <BarDomainContext.Provider
      value={{
        state,
        dispatch,
        isLoading,
        isMutating,
        error,
        reload,
        clearError,
        listHistory,
        openCommand,
        reopenCommand,
        setCommandStatus,
        printPrePaymentNote,
        printItems,
        printServices,
        printLine,
        addCommandItem,
        updateCommandItemQuantity,
        removeCommandItem,
        cancelCommand,
        closeCommand,
        closeVoucher,
        createCatalogEntry,
        updateCatalogEntry,
        removeCatalogEntry,
      }}
    >
      {children}
    </BarDomainContext.Provider>
  );
}
