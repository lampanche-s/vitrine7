import {
  useMemo,
  type ReactNode,
} from "react";

import {
  canAccessSystemModule,
  hasAppPermission,
} from "./access.rules";

import {
  AccessContext,
} from "./access.context";

import type {
  AppModuleId,
  AppPermission,
  AppSession,
} from "./access.types";

type AccessProviderProps = {
  children: ReactNode;
  session: AppSession;
};

export function AccessProvider({
  children,
  session,
}: AccessProviderProps) {
  const value = useMemo(
    () => ({
      session,

      can(
        permission: AppPermission
      ) {
        return hasAppPermission(
          session,
          permission
        );
      },

      canAccessModule(
        moduleId: AppModuleId
      ) {
        return canAccessSystemModule(
          session,
          moduleId
        );
      },
    }),
    [session]
  );

  return (
    <AccessContext.Provider value={value}>
      {children}
    </AccessContext.Provider>
  );
}
