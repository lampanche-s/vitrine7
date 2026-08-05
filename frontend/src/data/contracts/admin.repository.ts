import type {
  SystemUser,
  SystemUserInput,
  SystemUserStatus,
} from "../../entities/user";

export type AdminRepositorySnapshot = {
  users: SystemUser[];
};

export interface AdminRepository {
  getSnapshot(): Promise<AdminRepositorySnapshot>;

  getSnapshotForPermissions?(
    permissions: readonly string[]
  ): Promise<AdminRepositorySnapshot>;

  createUser(
    input: SystemUserInput
  ): Promise<SystemUser>;

  updateUser(
    userId: number,
    input: SystemUserInput
  ): Promise<SystemUser>;

  setUserStatus(
    userId: number,
    status: SystemUserStatus
  ): Promise<SystemUser>;

  resetUserPassword(
    userId: number,
    newPassword: string
  ): Promise<SystemUser>;

  removeUser(
    userId: number
  ): Promise<void>;
}
