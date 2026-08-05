import {
  createContext,
} from "react";

import type {
  AppModuleId,
  AppPermission,
  AppSession,
} from "./access.types";

export type AccessContextValue = {
  session: AppSession;

  can: (
    permission: AppPermission
  ) => boolean;

  canAccessModule: (
    moduleId: AppModuleId
  ) => boolean;
};

export const AccessContext =
  createContext<AccessContextValue | null>(
    null
  );
