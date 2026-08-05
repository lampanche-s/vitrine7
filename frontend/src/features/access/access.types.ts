export type AppPermission =
  | "lava:access"
  | "bar:access"
  | "payment:reverse"
  | "admin:users"
  | "admin:settings"
  | "admin:payment-config";

export type AppModuleId =
  | "lava"
  | "bar"
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

export type AdminAccessPanel =
  | "users"
  | "settings";
