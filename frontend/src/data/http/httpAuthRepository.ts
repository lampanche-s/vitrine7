import type {
  AppPermission,
  AppSession,
} from "../../features/access";

import {
  HttpError,
  httpClient,
} from "../../shared/http";

export const ACCOUNT_BLOCKED_ERROR_CODE =
  "ACCOUNT_BLOCKED";

export const ACCOUNT_BLOCKED_LOGIN_MESSAGE =
  "Acesso bloqueado. Entre em contato pelo telefone (71) 98716-0075 ou pelo e-mail jorge.lampanche@hotmail.com.";

const appPermissions = new Set<string>([
  "bar:access",
  "bar:manage-catalog",
  "clients:manage",
  "reports:access",
  "payment:reverse",
  "admin:users",
  "admin:payment-config",
]);

const appRoles = new Set<string>([
  "SUPER_ADMIN",
  "ADMINISTRADOR",
  "OPERADOR",
]);

type CurrentUserResponse = {
  id: number;
  name: string;
  username: string;
  role: string;
  status: string;
  permissions: string[];
};

type LoginRequest = {
  username: string;
  password: string;
};

function getApiErrorCode(error: HttpError): string | null {
  if (
    typeof error.payload === "object" &&
    error.payload !== null &&
    "code" in error.payload &&
    typeof error.payload.code === "string"
  ) {
    return error.payload.code;
  }

  return null;
}

function toAppSession(
  user: CurrentUserResponse
): AppSession {
  return {
    userId: user.id,
    displayName:
      user.name || user.username,
    role: appRoles.has(user.role)
      ? user.role as AppSession["role"]
      : "OPERADOR",
    permissions: user.permissions.filter(
      (permission): permission is AppPermission =>
        appPermissions.has(permission)
    ),
  };
}

async function getCurrentSession(): Promise<AppSession> {
  const user =
    await httpClient.get<CurrentUserResponse>(
      "/auth/me"
    );

  return toAppSession(user);
}

export const httpAuthRepository = {
  getCurrentSession,

  async login(
    credentials: LoginRequest
  ): Promise<AppSession> {
    try {
      await httpClient.post(
        "/auth/login",
        credentials
      );
    } catch (error) {
      if (
        error instanceof HttpError &&
        getApiErrorCode(error) ===
          ACCOUNT_BLOCKED_ERROR_CODE
      ) {
        throw new Error(
          ACCOUNT_BLOCKED_LOGIN_MESSAGE,
          {
            cause: error,
          }
        );
      }

      throw error;
    }

    return getCurrentSession();
  },

  async logout(): Promise<void> {
    await httpClient.post(
      "/auth/logout"
    );
  },
};
