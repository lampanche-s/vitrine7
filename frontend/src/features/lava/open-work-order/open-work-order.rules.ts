import type {
  CreateLavaOrderInput,
  LavaProgressVehicle,
} from "../../../entities/work-order";

import type {
  LavaFinanceEntry,
} from "../../../entities/payment";

import {
  getNextNumericId,
} from "../../../shared/lib/identifiers";

type OpenLavaWorkOrderResult = {
  progressVehicles: LavaProgressVehicle[];
  financeEntries: LavaFinanceEntry[];
};

export function openLavaWorkOrder(
  progressVehicles: readonly LavaProgressVehicle[],
  financeEntries: readonly LavaFinanceEntry[],
  order: CreateLavaOrderInput,
  _currentTime: string,
  currentTimestamp: string
): OpenLavaWorkOrderResult {
  const workOrderId = getNextNumericId(progressVehicles);
  const financeEntryId = getNextNumericId(financeEntries);

  const progressVehicle: LavaProgressVehicle = {
    id: workOrderId,
    financeEntryId,
    clientId: order.clientId,
    clientName: order.clientName,
    clientPhone: order.clientPhone,
    serviceId: order.serviceId,
    vehicle: order.vehicle,
    plate: order.plate,
    service: order.service,
    vehicleSize: order.vehicleSize,
    notes: order.notes ?? "",
    stage: "waiting",
    elapsed: "Agora",
    paidAt: undefined,
    createdAt: currentTimestamp,
    serviceDurationMinutes:
      order.serviceDurationMinutes,
  };

  const financeEntry: LavaFinanceEntry = {
    id: financeEntryId,
    workOrderId,
    vehicle: order.vehicle,
    plate: order.plate,
    service: order.service,
    amount: order.amount,
    method: "A definir",
    document: order.fiscalDocument,
    status: "pending",
    time: "Em aberto",
  };

  return {
    progressVehicles: [
      progressVehicle,
      ...progressVehicles,
    ],
    financeEntries: [
      financeEntry,
      ...financeEntries,
    ],
  };
}
