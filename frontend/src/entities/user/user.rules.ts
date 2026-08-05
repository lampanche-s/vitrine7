import { getNextNumericId } from "../../shared/lib/identifiers";

import type {
  SystemUser,
  SystemUserInput,
  SystemUserStatus,
} from "./user.types";

export const USERNAME_MAX_LENGTH = 255;

export const PASSWORD_MIN_LENGTH = 6;

export const PASSWORD_POLICY_MESSAGE =
  "A senha deve ter pelo menos 6 caracteres.";

export function isPasswordAccepted(
  password: string
): boolean {
  return password.length >= PASSWORD_MIN_LENGTH;
}

export function normalizeSystemUserInput(
  input: SystemUserInput
): SystemUserInput {
  return {
    name: input.name.trim(),
    username: input.username.trim(),
    password: input.password,
    role: input.role,
    status: input.status,
  };
}

export function isSystemUserInputComplete(
  input: SystemUserInput
): boolean {
  return Object.values(input).every(
    (value) => value.length > 0
  );
}

export function createSystemUser(
  users: readonly SystemUser[],
  input: SystemUserInput
): SystemUser[] {
  return [
    {
      id: getNextNumericId(users),
      ...input,
    },
    ...users,
  ];
}

export function updateSystemUser(
  users: readonly SystemUser[],
  userId: number,
  input: SystemUserInput
): SystemUser[] {
  return users.map((user) =>
    user.id === userId
      ? {
          ...user,
          ...input,
          password:
            input.password.length > 0
              ? input.password
              : user.password,
        }
      : user
  );
}

export function toggleSystemUserStatus(
  users: readonly SystemUser[],
  userId: number
): SystemUser[] {
  return users.map((user) =>
    user.id === userId
      ? {
          ...user,
          status:
            user.status === "Ativo"
              ? "Bloqueado"
              : "Ativo",
        }
      : user
  );
}

export function setSystemUserStatus(
  users: readonly SystemUser[],
  userId: number,
  status: SystemUserStatus
): SystemUser[] {
  return users.map((user) =>
    user.id === userId
      ? {
          ...user,
          status,
        }
      : user
  );
}

export function resetSystemUserPassword(
  users: readonly SystemUser[],
  userId: number,
  newPassword: string
): SystemUser[] {
  return users.map((user) =>
    user.id === userId
      ? {
          ...user,
          password: newPassword,
        }
      : user
  );
}

export function removeSystemUser(
  users: readonly SystemUser[],
  userId: number
): SystemUser[] {
  return users.filter((user) => user.id !== userId);
}
