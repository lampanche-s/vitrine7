import {
  createContext,
} from "react";

import type {
  SystemUserInput,
  SystemUserStatus,
} from "../../../entities/user";

import type {
  AdminDomainState,
} from "./admin-domain.types";

export type AdminDomainContextValue = {
  state: AdminDomainState;

  isLoading: boolean;
  isMutating: boolean;
  error: string | null;

  reload: () => Promise<void>;

  createUser: (
    input: SystemUserInput
  ) => Promise<boolean>;

  updateUser: (
    userId: number,
    input: SystemUserInput
  ) => Promise<boolean>;

  setUserStatus: (
    userId: number,
    status: SystemUserStatus
  ) => Promise<boolean>;

  resetUserPassword: (
    userId: number,
    newPassword: string
  ) => Promise<boolean>;

  removeUser: (
    userId: number
  ) => Promise<boolean>;
};

export const AdminDomainContext =
  createContext<AdminDomainContextValue | null>(
    null
  );
