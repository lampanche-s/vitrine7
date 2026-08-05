export type AppPermission =
  | "bar:access"
  | "bar:manage-catalog"
  | "clients:manage"
  | "reports:access"
  | "payment:reverse"
  | "admin:users"
  | "admin:payment-config";

export type AppModuleId =
  | "bar"
  | "clients"
  | "reports"
  | "users";

export type AppUserRole =
  | "SUPER_ADMIN"
  | "ADMINISTRADOR"
  | "OPERADOR";

export type AppSession = {
  userId: number;
  displayName: string;
  role: AppUserRole;
  permissions: AppPermission[];
};
