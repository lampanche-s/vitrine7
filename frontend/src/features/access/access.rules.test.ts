import {
  describe,
  expect,
  it,
} from "vitest";

import {
  mockCurrentSession,
} from "../../data/test-fixtures/session.fixture";

import {
  canAccessSystemModule,
  hasAppPermission,
} from "./access.rules";

import type {
  AppSession,
} from "./access.types";

describe("access.rules", () => {
  it(
    "permite acesso quando a sessão possui a permissão correspondente",
    () => {
      expect(
        canAccessSystemModule(
          mockCurrentSession,
          "clients"
        )
      ).toBe(true);

      expect(
        hasAppPermission(
          mockCurrentSession,
          "admin:users"
        )
      ).toBe(true);
    }
  );

  it(
    "nega acesso quando a sessão não possui permissão",
    () => {
      const limitedSession: AppSession = {
        userId: 99,
        displayName: "Operador limitado",
        role: "OPERADOR",
        permissions: [
          "bar:access",
        ],
      };

      expect(
        canAccessSystemModule(
          limitedSession,
          "clients"
        )
      ).toBe(false);

      expect(
        hasAppPermission(
          limitedSession,
          "admin:users"
        )
      ).toBe(false);
    }
  );
});
