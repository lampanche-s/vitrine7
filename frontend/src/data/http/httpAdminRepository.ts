import type {
  SystemSettings,
} from "../../entities/settings";

import type {
  SystemUser,
  SystemUserInput,
  SystemUserRole,
  SystemUserStatus,
} from "../../entities/user";

import type {
  AdminRepository,
  AdminRepositorySnapshot,
} from "../contracts";

import {
  httpClient,
} from "../../shared/http";

import {
  listAllPages,
  type HttpPageResponse,
} from "./listAllPages";

type UserResponse = {
  id: number;
  name: string;
  username: string;
  role: "SUPER_ADMIN" | "ADMINISTRADOR" | "OPERADOR";
  status: "ATIVO" | "BLOQUEADO";
};

type SettingsResponse = {
  companyName: string;
  cnpj: string;
  phone: string;
  address: string;
  adminMode: boolean;
};

type PreferenceResponse = {
  adminMode: boolean;
};

const roleToBackend: Record<SystemUserRole, UserResponse["role"]> = {
  Administrador: "ADMINISTRADOR",
  Operador: "OPERADOR",
};

const statusToBackend: Record<SystemUserStatus, UserResponse["status"]> = {
  Ativo: "ATIVO",
  Bloqueado: "BLOQUEADO",
};

function mapUser(user: UserResponse): SystemUser {
  return {
    id: user.id,
    name: user.name,
    username: user.username,
    password: "",
    role:
      user.role === "ADMINISTRADOR"
        ? "Administrador"
        : "Operador",
    status:
      user.status === "ATIVO"
        ? "Ativo"
        : "Bloqueado",
  };
}

function mapSettings(
  settings: SettingsResponse
): SystemSettings {
  return {
    companyName: settings.companyName,
    cnpj: settings.cnpj,
    phone: settings.phone,
    address: settings.address,
    adminMode: settings.adminMode,
  };
}

function settingsToBackend(settings: SystemSettings) {
  return {
    companyName: settings.companyName,
    cnpj: settings.cnpj,
    phone: settings.phone,
    address: settings.address,
  };
}

function userInputToBackend(input: SystemUserInput) {
  return {
    name: input.name,
    username: input.username,
    password: input.password,
    role: roleToBackend[input.role],
    status: statusToBackend[input.status],
  };
}

function userUpdateToBackend(input: SystemUserInput) {
  return {
    name: input.name,
    username: input.username,
    role: roleToBackend[input.role],
  };
}

async function listAllUsers(): Promise<SystemUser[]> {
  const users = await listAllPages(
    (page) =>
      httpClient.get<
        HttpPageResponse<UserResponse>
      >(
        `/users?page=${page}&size=100&sort=name&direction=ASC`
      )
  );

  return users.map(mapUser);
}

export const httpAdminRepository: AdminRepository = {
  async getSnapshot(): Promise<AdminRepositorySnapshot> {
    const [users, settings] = await Promise.all([
      listAllUsers(),
      httpClient.get<SettingsResponse>("/settings"),
    ]);

    return {
      users,
      settings: mapSettings(settings),
    };
  },

  async getSnapshotForPermissions(
    permissions
  ): Promise<AdminRepositorySnapshot> {
    const canLoadUsers =
      permissions.includes("admin:users");

    const [users, settings] = await Promise.all([
      canLoadUsers
        ? listAllUsers()
        : Promise.resolve([]),
      httpClient.get<SettingsResponse>("/settings"),
    ]);

    return {
      users,
      settings: mapSettings(settings),
    };
  },

  async createUser(input) {
    const user = await httpClient.post<UserResponse>(
      "/users",
      userInputToBackend(input)
    );

    return mapUser(user);
  },

  async updateUser(userId, input) {
    const user = await httpClient.put<UserResponse>(
      `/users/${userId}`,
      userUpdateToBackend(input)
    );

    return mapUser(user);
  },

  async setUserStatus(userId, status) {
    const user = await httpClient.patch<UserResponse>(
      `/users/${userId}/status`,
      {
        status: statusToBackend[status],
      }
    );

    return mapUser(user);
  },

  async resetUserPassword(userId, newPassword) {
    await httpClient.post<void>(
      `/users/${userId}/reset-password`,
      {
        newPassword,
      }
    );

    return mapUser(
      await httpClient.get<UserResponse>(
        `/users/${userId}`
      )
    );
  },

  async removeUser(userId) {
    await httpClient.delete<void>(
      `/users/${userId}`
    );
  },

  async saveSettings(settings) {
    if (settings.adminMode !== undefined) {
      await httpClient.patch<PreferenceResponse>(
        "/preferences/admin-mode",
        {
          enabled: settings.adminMode,
        }
      );
    }

    const [updatedSettings, updatedPreference] =
      await Promise.all([
        httpClient.put<SettingsResponse>(
          "/settings",
          settingsToBackend(settings)
        ),
        httpClient.get<PreferenceResponse>(
          "/preferences/me"
        ),
      ]);

    return {
      ...mapSettings(updatedSettings),
      adminMode: updatedPreference.adminMode,
    };
  },
};
