import type {
  AppSession,
} from "../../features/access";

export const mockCurrentSession: AppSession = {
  userId: 1,
  displayName: "Administrador",
  role: "ADMINISTRADOR",
  permissions: [
    "lava:access",
    "bar:access",
    "payment:reverse",
    "admin:users",
    "admin:settings",
    "admin:payment-config",
  ],
};
