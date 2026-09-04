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

function userCreateToBackend(input: SystemUserInput) {
  return {
    name: input.name,
    username: input.username,
    password: input.password,
    role: roleToBackend[input.role],
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
    return {
      users: await listAllUsers(),
    };
  },

  async getSnapshotForPermissions(
    permissions
  ): Promise<AdminRepositorySnapshot> {
    if (!permissions.includes("admin:users")) {
      return {
        users: [],
      };
    }

    return {
      users: await listAllUsers(),
    };
  },

  async createUser(input) {
    return mapUser(
      await httpClient.post<UserResponse>(
        "/users",
        userCreateToBackend(input)
      )
    );
  },

  async updateUser(userId, input) {
    return mapUser(
      await httpClient.put<UserResponse>(
        `/users/${userId}`,
        userUpdateToBackend(input)
      )
    );
  },

  async setUserStatus(userId, status) {
    return mapUser(
      await httpClient.patch<UserResponse>(
        `/users/${userId}/status`,
        {
          status: statusToBackend[status],
        }
      )
    );
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
};
