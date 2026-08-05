import type {
  AppModuleId,
  AppPermission,
  AppSession,
} from "./access.types";

const modulePermissionMap: Record<
  AppModuleId,
  AppPermission
> = {
  bar: "bar:access",
  clients: "clients:manage",
  reports: "reports:access",
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
