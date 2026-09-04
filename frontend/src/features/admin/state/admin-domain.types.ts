import type {
  SystemUser,
} from "../../../entities/user";

export type AdminDomainState = {
  users: SystemUser[];
};

export type AdminDomainAction =
  | {
      type: "repository/snapshot-loaded";
      payload: AdminDomainState;
    }
  | {
      type: "user/created";
      payload: SystemUser;
    }
  | {
      type: "user/updated";
      payload: SystemUser;
    }
  | {
      type: "user/removed";
      payload: number;
    };
