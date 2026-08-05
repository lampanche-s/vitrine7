import {
  describe,
  expect,
  it,
} from "vitest";

import type {
  LavaDomainState,
} from "./lava-domain.types";

import {
  shouldShowLavaInitialLoading,
} from "./lava-domain.loading";

const emptyState: LavaDomainState = {
  services: [],
  clients: [],
  progressVehicles: [],
  financeEntries: [],
  historyEntries: [],
};

describe("lava domain loading", () => {
  it("exibe loading apenas antes do primeiro snapshot quando não há dados", () => {
    expect(
      shouldShowLavaInitialLoading(false, emptyState)
    ).toBe(true);
  });

  it("mantém a tela visível quando já existe snapshot carregado", () => {
    expect(
      shouldShowLavaInitialLoading(true, emptyState)
    ).toBe(false);
  });

  it("mantém dados existentes visíveis durante novas cargas", () => {
    expect(
      shouldShowLavaInitialLoading(false, {
        ...emptyState,
        clients: [
          {
            id: 1,
            name: "Cliente",
            phone: "",
            vehicle: "Honda Civic",
            plate: "QXZ8A21",
            lastService: "Ainda sem serviço",
            visits: 0,
            active: true,
          },
        ],
      })
    ).toBe(false);
  });
});
