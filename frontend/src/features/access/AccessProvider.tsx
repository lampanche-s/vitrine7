import {
  useMemo,
  type ReactNode,
} from "react";

import {
  canAccessAdminPanel as checkAdminPanelAccess,
  canAccessSystemModule,
  hasAppPermission,
} from "./access.rules";

import {
  AccessContext,
} from "./access.context";

import type {
  AdminAccessPanel,
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

      canAccessAdminPanel(
        panel: AdminAccessPanel
      ) {
        return checkAdminPanelAccess(
          session,
          panel
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
