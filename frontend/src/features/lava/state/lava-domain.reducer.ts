import type {
  LavaDomainAction,
  LavaDomainState,
} from "./lava-domain.types";

import type {
  LavaProgressVehicle,
} from "../../../entities/work-order";

function isActiveWorkOrder(
  workOrder: LavaProgressVehicle
): boolean {
  if (workOrder.operationalStatus) {
    return (
      workOrder.operationalStatus === "OPEN" ||
      workOrder.operationalStatus ===
        "PAYMENT_PENDING" ||
      workOrder.operationalStatus === "PAID"
    );
  }

  return (
    workOrder.stage !== "done" &&
    workOrder.stage !== "cancelled"
  );
}

function uniqueActiveWorkOrders(
  workOrders: LavaProgressVehicle[]
): LavaProgressVehicle[] {
  const workOrdersById =
    new Map<number, LavaProgressVehicle>();

  workOrders.forEach((workOrder) => {
    if (isActiveWorkOrder(workOrder)) {
      workOrdersById.set(workOrder.id, workOrder);
    }
  });

  return Array.from(workOrdersById.values());
}

export function lavaDomainReducer(
  state: LavaDomainState,
  action: LavaDomainAction
): LavaDomainState {
  switch (action.type) {
    case "state/replaced":
      return {
        ...action.payload,
        progressVehicles: uniqueActiveWorkOrders(
          action.payload.progressVehicles
        ),
      };

    case "service/created":
      return {
        ...state,
        services: [
          action.payload,
          ...state.services.filter(
            (service) =>
              service.id !== action.payload.id
          ),
        ],
      };

    case "service/updated":
      return {
        ...state,
        services: state.services.map((service) =>
          service.id === action.payload.id
            ? action.payload
            : service
        ),
      };

    case "service/removed":
      return {
        ...state,
        services: state.services.filter(
          (service) => service.id !== action.payload
        ),
      };

    case "client/created":
      return {
        ...state,
        clients: [
          action.payload,
          ...state.clients.filter(
            (client) =>
              client.id !== action.payload.id
          ),
        ],
      };

    case "client/updated":
      return {
        ...state,
        clients: state.clients.map((client) =>
          client.id === action.payload.id
            ? action.payload
            : client
        ),
      };

    case "client/removed":
      return {
        ...state,
        clients: state.clients.filter(
          (client) => client.id !== action.payload
        ),
      };

    case "work-order/created":
      return {
        ...state,

        progressVehicles: isActiveWorkOrder(
          action.payload.workOrder
        )
          ? [
              action.payload.workOrder,
              ...state.progressVehicles.filter(
                (workOrder) =>
                  workOrder.id !==
                  action.payload.workOrder.id
              ),
            ]
          : state.progressVehicles.filter(
              (workOrder) =>
                workOrder.id !==
                action.payload.workOrder.id
            ),

        financeEntries: action.payload.financeEntry
          ? [
              action.payload.financeEntry,
              ...state.financeEntries.filter(
                (entry) =>
                  entry.id !==
                  action.payload.financeEntry?.id
              ),
            ]
          : state.financeEntries,
      };

    case "work-order/updated":
      return {
        ...state,

        progressVehicles: isActiveWorkOrder(
          action.payload
        )
          ? [
              action.payload,
              ...state.progressVehicles.filter(
                (workOrder) =>
                  workOrder.id !== action.payload.id
              ),
            ]
          : state.progressVehicles.filter(
              (workOrder) =>
                workOrder.id !== action.payload.id
            ),
      };

    case "finance-entry/updated":
      return {
        ...state,

        financeEntries:
          state.financeEntries.map((entry) =>
            entry.id === action.payload.id
              ? action.payload
              : entry
          ),
      };

    case "history/replaced":
      return {
        ...state,
        historyEntries: action.payload,
      };

    default:
      return state;
  }
}
