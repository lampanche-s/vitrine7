import type {
  LavaProgressStage,
  LavaProgressStageDefinition,
  LavaProgressVehicle,
} from "./work-order.types";

export const lavaProgressStages: readonly LavaProgressStageDefinition[] = [
  {
    id: "waiting",
    label: "Aguardando",
    shortLabel: "Aguardando",
  },
  {
    id: "prewash",
    label: "Pré-lavagem",
    shortLabel: "Pré",
  },
  {
    id: "washing",
    label: "Em lavagem",
    shortLabel: "Lavagem",
  },
  {
    id: "finishing",
    label: "Finalização",
    shortLabel: "Finalização",
  },
  {
    id: "delivery",
    label: "Pronto para entrega",
    shortLabel: "Entrega",
  },
  {
    id: "done",
    label: "Concluído",
    shortLabel: "Concluído",
  },
];

export function getLavaProgressStageLabel(
  stage: LavaProgressStage
): string {
  if (stage === "cancelled") {
    return "Cancelado";
  }

  return (
    lavaProgressStages.find(
      (progressStage) => progressStage.id === stage
    )?.label ?? stage
  );
}

export function getNextLavaProgressStage(
  stage: LavaProgressStage
): LavaProgressStageDefinition | null {
  if (stage === "cancelled") {
    return null;
  }

  const currentIndex = lavaProgressStages.findIndex(
    (progressStage) => progressStage.id === stage
  );

  if (
    currentIndex < 0 ||
    currentIndex === lavaProgressStages.length - 1
  ) {
    return null;
  }

  return lavaProgressStages[currentIndex + 1];
}

export function getPreviousLavaProgressStage(
  stage: LavaProgressStage
): LavaProgressStageDefinition | null {
  if (stage === "cancelled") {
    return null;
  }

  const currentIndex = lavaProgressStages.findIndex(
    (progressStage) => progressStage.id === stage
  );

  if (currentIndex <= 0) {
    return null;
  }

  return lavaProgressStages[currentIndex - 1];
}

function updateLavaProgressVehicleStage(
  vehicles: readonly LavaProgressVehicle[],
  vehicleId: number,
  stage: LavaProgressStage
): LavaProgressVehicle[] {
  return vehicles.map((vehicle) =>
    vehicle.id === vehicleId
      ? {
          ...vehicle,
          stage,
        }
      : vehicle
  );
}

export function setLavaProgressVehicleStage(
  vehicles: readonly LavaProgressVehicle[],
  vehicleId: number,
  stage: LavaProgressStage
): LavaProgressVehicle[] {
  return updateLavaProgressVehicleStage(
    vehicles,
    vehicleId,
    stage
  );
}

export function advanceLavaProgressVehicle(
  vehicles: readonly LavaProgressVehicle[],
  vehicleId: number
): LavaProgressVehicle[] {
  const vehicle = vehicles.find(
    (currentVehicle) => currentVehicle.id === vehicleId
  );

  if (!vehicle) {
    return [...vehicles];
  }

  const nextStage = getNextLavaProgressStage(vehicle.stage);

  if (!nextStage) {
    return [...vehicles];
  }

  return updateLavaProgressVehicleStage(
    vehicles,
    vehicleId,
    nextStage.id
  );
}

export function returnLavaProgressVehicle(
  vehicles: readonly LavaProgressVehicle[],
  vehicleId: number
): LavaProgressVehicle[] {
  const vehicle = vehicles.find(
    (currentVehicle) => currentVehicle.id === vehicleId
  );

  if (!vehicle) {
    return [...vehicles];
  }

  const previousStage = getPreviousLavaProgressStage(
    vehicle.stage
  );

  if (!previousStage) {
    return [...vehicles];
  }

  return updateLavaProgressVehicleStage(
    vehicles,
    vehicleId,
    previousStage.id
  );
}

export function cancelLavaProgressVehicle(
  vehicles: readonly LavaProgressVehicle[],
  vehicleId: number
): LavaProgressVehicle[] {
  const vehicle = vehicles.find(
    (currentVehicle) => currentVehicle.id === vehicleId
  );

  if (!vehicle || vehicle.stage === "cancelled") {
    return [...vehicles];
  }

  return updateLavaProgressVehicleStage(
    vehicles,
    vehicleId,
    "cancelled"
  );
}

export function reopenLavaProgressVehicle(
  vehicles: readonly LavaProgressVehicle[],
  vehicleId: number
): LavaProgressVehicle[] {
  const vehicle = vehicles.find(
    (currentVehicle) => currentVehicle.id === vehicleId
  );

  if (!vehicle || vehicle.stage !== "cancelled") {
    return [...vehicles];
  }

  return updateLavaProgressVehicleStage(
    vehicles,
    vehicleId,
    "waiting"
  );
}
