import { getNextNumericId } from "../../shared/lib/identifiers";

import type {
  LavaService,
  LavaServiceInput,
} from "./service.types";

export function normalizeLavaServiceInput(
  input: LavaServiceInput
): LavaServiceInput {
  const legacyPrice = input.price?.trim() ?? "";
  const smallCarPrice =
    input.smallCarPrice?.trim() || legacyPrice;
  const mediumCarPrice =
    input.mediumCarPrice?.trim() || legacyPrice;

  return {
    name: input.name.trim(),
    category: input.category.trim(),
    price: smallCarPrice,
    smallCarPrice,
    mediumCarPrice,
    duration: input.duration.trim(),
  };
}

export function isLavaServiceInputComplete(
  input: LavaServiceInput
): boolean {
  return (
    input.name.length > 0 &&
    input.category.length > 0 &&
    input.smallCarPrice.length > 0 &&
    input.mediumCarPrice.length > 0 &&
    input.duration.length > 0
  );
}

export function getLavaServiceSmallCarPrice(
  service: LavaService
) {
  return service.smallCarPrice || service.price || "R$ 0,00";
}

export function getLavaServiceMediumCarPrice(
  service: LavaService
) {
  return service.mediumCarPrice || service.price || "R$ 0,00";
}

export function getLavaServicePriceForSize(
  service: LavaService,
  vehicleSize: "Pequeno" | "Médio"
) {
  return vehicleSize === "Médio"
    ? getLavaServiceMediumCarPrice(service)
    : getLavaServiceSmallCarPrice(service);
}

export function getLavaServicePriceSummary(
  service: LavaService
) {
  return [
    `Pequeno ${getLavaServiceSmallCarPrice(service)}`,
    `Médio ${getLavaServiceMediumCarPrice(service)}`,
  ].join(" · ");
}

export function parseLavaServiceDurationMinutes(
  duration: string
): number | null {
  const normalizedDuration =
    duration.trim().toLowerCase();

  if (!normalizedDuration) {
    return null;
  }

  const hourMatch =
    normalizedDuration.match(/(\d+)\s*(?:h|hora|horas)\b/);
  const minuteMatch =
    normalizedDuration.match(/(\d+)\s*(?:min|mins|minuto|minutos)\b/);

  const hours = hourMatch
    ? Number(hourMatch[1])
    : 0;
  const minutes = minuteMatch
    ? Number(minuteMatch[1])
    : 0;

  if (hours > 0 || minutes > 0) {
    const totalMinutes = hours * 60 + minutes;

    return totalMinutes > 0
      ? totalMinutes
      : null;
  }

  const numericDuration =
    Number(normalizedDuration.replace(",", "."));

  return Number.isFinite(numericDuration) &&
    numericDuration > 0
    ? numericDuration
    : null;
}

export function createLavaService(
  services: readonly LavaService[],
  input: LavaServiceInput
): LavaService[] {
  return [
    {
      id: getNextNumericId(services),
      ...input,
      price: input.smallCarPrice,
      active: true,
    },
    ...services,
  ];
}

export function updateLavaService(
  services: readonly LavaService[],
  serviceId: number,
  input: LavaServiceInput
): LavaService[] {
  return services.map((service) =>
    service.id === serviceId
      ? {
          ...service,
          ...input,
        }
      : service
  );
}

export function toggleLavaServiceStatus(
  services: readonly LavaService[],
  serviceId: number
): LavaService[] {
  return services.map((service) =>
    service.id === serviceId
      ? {
          ...service,
          active: !service.active,
        }
      : service
  );
}

export function setLavaServiceActive(
  services: readonly LavaService[],
  serviceId: number,
  active: boolean
): LavaService[] {
  return services.map((service) =>
    service.id === serviceId
      ? {
          ...service,
          active,
        }
      : service
  );
}

export function removeLavaService(
  services: readonly LavaService[],
  serviceId: number
): LavaService[] {
  return services.filter(
    (service) => service.id !== serviceId
  );
}
