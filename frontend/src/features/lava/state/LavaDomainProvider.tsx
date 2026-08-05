import {
  useCallback,
  useEffect,
  useReducer,
  useRef,
  useState,
  type ReactNode,
} from "react";

import type {
  LavaClientInput,
} from "../../../entities/client";

import type {
  LavaWorkOrderPaymentInput,
  LavaWorkOrderPaymentResult,
  SettleLavaFinanceEntryInput,
} from "../../../entities/payment";

import type {
  LavaServiceInput,
} from "../../../entities/service";

import type {
  CreateLavaOrderInput,
  LavaProgressStage,
} from "../../../entities/work-order";

import type {
  LavaHistoryPageResult,
  OpenLavaWorkOrderRepositoryResult,
  LavaRepository,
} from "../../../data/contracts";

import {
  useToast,
} from "../../../components/ui";


import {
  useMutationLock,
} from "../../../shared/hooks/useMutationLock";

import {
  LavaDomainContext,
} from "./lava-domain.context";

import {
  lavaDomainReducer,
} from "./lava-domain.reducer";

import {
  shouldShowLavaInitialLoading,
} from "./lava-domain.loading";

import type {
  LavaDomainState,
} from "./lava-domain.types";

type LavaDomainProviderProps = {
  children: ReactNode;
  initialState: LavaDomainState;
  repository: LavaRepository;
};

const LAVA_SERVICE_TIME_CHECK_INTERVAL_MS =
  30_000;

function getErrorMessage(error: unknown): string {
  return error instanceof Error
    ? error.message
    : "Não foi possível concluir a operação.";
}

export function LavaDomainProvider({
  children,
  initialState,
  repository,
}: LavaDomainProviderProps) {
  const [state, dispatch] = useReducer(
    lavaDomainReducer,
    initialState
  );
  const pendingNotificationWorkOrders =
    useRef<Set<number>>(new Set());

  const {
    showErrorToast,
  } = useToast();

  const [isLoading, setIsLoading] = useState(false);
  const [
    hasLoadedSnapshot,
    setHasLoadedSnapshot,
  ] = useState(false);

  const [error, setError] =
    useState<string | null>(null);

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
        type: "state/replaced",
        payload: {
          services: snapshot.services,
          clients: snapshot.clients,
          progressVehicles: snapshot.workOrders,
          financeEntries: snapshot.financeEntries,
          historyEntries: snapshot.historyEntries,
        },
      });
      setHasLoadedSnapshot(true);
    } catch (currentError) {
      const message =
        getErrorMessage(currentError);

      setError(message);
      showErrorToast(currentError, {
        title: "Não foi possível carregar os dados",
        dedupeKey: `lava-reload|${message}`,
      });
    } finally {
      setIsLoading(false);
    }
  }, [repository, showErrorToast]);

  useEffect(() => {
    queueMicrotask(() => {
      void reload();
    });
  }, [reload]);

  const executeMutation = useCallback(<T,>(
    operation: () => Promise<T>,
    ignoredResult: T
  ): Promise<T> => {
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
            dedupeKey: `lava-mutation|${message}`,
          });

          return ignoredResult;
        }
      },
      ignoredResult
    );
  }, [runMutation, showErrorToast]);

  function createService(
    input: LavaServiceInput
  ): Promise<boolean> {
    return executeMutation(
      async () => {
      const createdService =
        await repository.createService(input);

      dispatch({
        type: "service/created",
        payload: createdService,
      });

      return true;
      },
      false
    );
  }

  function updateService(
    serviceId: number,
    input: LavaServiceInput
  ): Promise<boolean> {
    return executeMutation(
      async () => {
      const updatedService =
        await repository.updateService(
          serviceId,
          input
        );

      dispatch({
        type: "service/updated",
        payload: updatedService,
      });

      return true;
      },
      false
    );
  }

  function setServiceActive(
    serviceId: number,
    active: boolean
  ): Promise<boolean> {
    return executeMutation(
      async () => {
      const updatedService =
        await repository.setServiceActive(
          serviceId,
          active
        );

      dispatch({
        type: "service/updated",
        payload: updatedService,
      });

      return true;
      },
      false
    );
  }

  function removeService(
    serviceId: number
  ): Promise<boolean> {
    return executeMutation(
      async () => {
      await repository.removeService(serviceId);

      dispatch({
        type: "service/removed",
        payload: serviceId,
      });

      return true;
      },
      false
    );
  }

  function createClient(
    input: LavaClientInput
  ): Promise<boolean> {
    return executeMutation(
      async () => {
      const createdClient =
        await repository.createClient(input);

      dispatch({
        type: "client/created",
        payload: createdClient,
      });

      return true;
      },
      false
    );
  }

  function updateClient(
    clientId: number,
    input: LavaClientInput
  ): Promise<boolean> {
    return executeMutation(
      async () => {
      const updatedClient =
        await repository.updateClient(
          clientId,
          input
        );

      dispatch({
        type: "client/updated",
        payload: updatedClient,
      });

      return true;
      },
      false
    );
  }

  function setClientActive(
    clientId: number,
    active: boolean
  ): Promise<boolean> {
    return executeMutation(
      async () => {
      const updatedClient =
        await repository.setClientActive(
          clientId,
          active
        );

      dispatch({
        type: "client/updated",
        payload: updatedClient,
      });

      return true;
      },
      false
    );
  }

  function removeClient(
    clientId: number
  ): Promise<boolean> {
    return executeMutation(
      async () => {
      await repository.removeClient(clientId);

      dispatch({
        type: "client/removed",
        payload: clientId,
      });

      return true;
      },
      false
    );
  }

  function openWorkOrder(
    input: CreateLavaOrderInput
  ): Promise<OpenLavaWorkOrderRepositoryResult | null> {
    return executeMutation(
      async () => {
      const result =
        await repository.openWorkOrder(input);

      dispatch({
        type: "work-order/created",
        payload: result,
      });

      return result;
      },
      null
    );
  }

  function payWorkOrder(
    input: LavaWorkOrderPaymentInput
  ): Promise<LavaWorkOrderPaymentResult | null> {
    return executeMutation(
      async () => {
        const result =
          await repository.payWorkOrder(input);

        dispatch({
          type: "work-order/updated",
          payload: result.workOrder,
        });

        return result;
      },
      null
    );
  }

  function changeWorkOrderStage(
    workOrderId: number,
    stage: LavaProgressStage
  ): Promise<boolean> {
    return executeMutation(
      async () => {
      const updatedWorkOrder =
        await repository.changeWorkOrderStage(
          workOrderId,
          stage
        );

      dispatch({
        type: "work-order/updated",
        payload: updatedWorkOrder,
      });

      if (stage === "done") {
        const history =
          await repository.listHistory({
            page: 0,
            size: 7,
          });

        dispatch({
          type: "history/replaced",
          payload: history.entries,
        });
      }

      return true;
      },
      false
    );
  }

  function settleFinanceEntry(
    input: SettleLavaFinanceEntryInput
  ): Promise<boolean> {
    return executeMutation(
      async () => {
      const result =
        await repository.settleFinanceEntry(input);

      dispatch({
        type: "finance-entry/updated",
        payload: result.financeEntry,
      });

      if (result.workOrder) {
        dispatch({
          type: "work-order/updated",
          payload: result.workOrder,
        });
      }

      return true;
      },
      false
    );
  }

  const listHistory = useCallback((input: {
    page: number;
    size: number;
    search?: string;
    from?: string;
    to?: string;
  }): Promise<LavaHistoryPageResult | null> => {
    return (async () => {
      setError(null);

      try {
        const result =
          await repository.listHistory(input);

        return result;
      } catch (currentError) {
        const message =
          getErrorMessage(currentError);

        setError(message);
        showErrorToast(currentError, {
          title: "Não foi possível carregar o histórico",
          dedupeKey: `lava-history|${message}`,
        });
        return null;
      }
    })();
  }, [repository, showErrorToast]);

  const listFinance = useCallback((input: {
    from: string;
    to: string;
    page: number;
    size: number;
    search?: string;
  }) => {
    return executeMutation(
      () => repository.listFinance(input),
      null
    );
  }, [
    executeMutation,
    repository,
  ]);

  const markServiceTimeNotified = useCallback((
    workOrderId: number,
    notifiedAt: string
  ): Promise<boolean> => {
    return executeMutation(
      async () => {
        const updatedWorkOrder =
          await repository.markServiceTimeNotified(
            workOrderId,
            notifiedAt
          );

        dispatch({
          type: "work-order/updated",
          payload: updatedWorkOrder,
        });

        return true;
      },
      false
    );
  }, [
    executeMutation,
    repository,
  ]);

  useEffect(() => {
    function checkServiceTimes() {
      const now = Date.now();

      state.progressVehicles.forEach((workOrder) => {
        if (
          pendingNotificationWorkOrders.current.has(
            workOrder.id
          ) ||
          workOrder.stage === "cancelled" ||
          workOrder.serviceTimeNotifiedAt ||
          !workOrder.paidAt ||
          typeof workOrder.serviceDurationMinutes !==
            "number" ||
          workOrder.serviceDurationMinutes <= 0
        ) {
          return;
        }

        const financeEntry =
          state.financeEntries.find(
            (entry) =>
              entry.id ===
                workOrder.financeEntryId &&
              entry.status === "paid"
          );

        if (!financeEntry) {
          return;
        }

        const paidAtTime = new Date(
          workOrder.paidAt
        ).getTime();

        if (!Number.isFinite(paidAtTime)) {
          return;
        }

        const expectedFinishTime =
          paidAtTime +
          workOrder.serviceDurationMinutes *
            60_000;

        if (expectedFinishTime > now) {
          return;
        }

        const notifiedAt =
          new Date(now).toISOString();

        pendingNotificationWorkOrders.current.add(
          workOrder.id
        );

        void markServiceTimeNotified(
          workOrder.id,
          notifiedAt
        ).then((marked) => {
          if (!marked) {
            pendingNotificationWorkOrders.current.delete(
              workOrder.id
            );
            return;
          }

        });
      });
    }

    checkServiceTimes();

    const intervalId = window.setInterval(
      checkServiceTimes,
      LAVA_SERVICE_TIME_CHECK_INTERVAL_MS
    );

    return () => {
      window.clearInterval(intervalId);
    };
  }, [
    markServiceTimeNotified,
    state.financeEntries,
    state.progressVehicles,
  ]);

  return (
    <LavaDomainContext.Provider
      value={{
        state,
        isLoading:
          isLoading &&
          shouldShowLavaInitialLoading(
            hasLoadedSnapshot,
            state
          ),
        isMutating,
        error,
        reload,
        createService,
        updateService,
        setServiceActive,
        removeService,
        createClient,
        updateClient,
        setClientActive,
        removeClient,
        openWorkOrder,
        payWorkOrder,
        changeWorkOrderStage,
        settleFinanceEntry,
        listHistory,
        listFinance,
        markServiceTimeNotified,
      }}
    >
      {children}
    </LavaDomainContext.Provider>
  );
}

