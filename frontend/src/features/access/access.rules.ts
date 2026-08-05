import type {
  AdminAccessPanel,
  AppModuleId,
  AppPermission,
  AppSession,
} from "./access.types";

const modulePermissionMap: Record<
  AppModuleId,
  AppPermission
> = {
  lava: "lava:access",
  bar: "bar:access",
  users: "admin:users",
};

export function hasAppPermission(
  session: AppSession,
  permission: AppPermission
): boolean {
  return session.permissions.includes(
    permission
  );
}

export function canAccessSystemModule(
  session: AppSession,
  moduleId: AppModuleId
): boolean {
  return hasAppPermission(
    session,
    modulePermissionMap[moduleId]
  );
}

export function canAccessAdminPanel(
  session: AppSession,
  panel: AdminAccessPanel
): boolean {
  if (panel === "users") {
    return hasAppPermission(
      session,
      "admin:users"
    );
  }

  return hasAppPermission(
    session,
    "admin:settings"
  );
}
