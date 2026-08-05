import {
  describe,
  expect,
  it,
} from "vitest";

import {
  mockCurrentSession,
} from "../../data/test-fixtures/session.fixture";

import {
  canAccessAdminPanel,
  canAccessSystemModule,
  hasAppPermission,
} from "./access.rules";

import type {
  AppSession,
} from "./access.types";

describe("access.rules", () => {
  it(
    "permite acesso quando a função possui a permissão correspondente",
    () => {
      expect(
        canAccessSystemModule(
          mockCurrentSession,
          "lava"
        )
      ).toBe(true);

      expect(
        hasAppPermission(
          mockCurrentSession,
          "admin:settings"
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
          "lava"
        )
      ).toBe(false);

      expect(
        canAccessAdminPanel(
          limitedSession,
          "users"
        )
      ).toBe(false);

      expect(
        canAccessAdminPanel(
          limitedSession,
          "settings"
        )
      ).toBe(false);
    }
  );
});
