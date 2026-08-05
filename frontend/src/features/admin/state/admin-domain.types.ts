import type {
  SystemSettings,
} from "../../../entities/settings";

import type {
  SystemUser,
} from "../../../entities/user";

export type AdminDomainState = {
  users: SystemUser[];
  settings: SystemSettings;
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
    }
  | {
      type: "settings/updated";
      payload: SystemSettings;
    };
