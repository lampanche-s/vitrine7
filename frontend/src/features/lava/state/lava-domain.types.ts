import type {
  LavaClient,
} from "../../../entities/client";

import type {
  LavaFinanceEntry,
} from "../../../entities/payment";

import type {
  LavaService,
} from "../../../entities/service";

import type {
  LavaHistoryEntry,
  LavaProgressVehicle,
} from "../../../entities/work-order";

export type LavaDomainState = {
  services: LavaService[];
  clients: LavaClient[];
  progressVehicles: LavaProgressVehicle[];
  financeEntries: LavaFinanceEntry[];
  historyEntries: LavaHistoryEntry[];
};

export type LavaDomainAction =
  | {
      type: "state/replaced";
      payload: LavaDomainState;
    }
  | {
      type: "service/created";
      payload: LavaService;
    }
  | {
      type: "service/updated";
      payload: LavaService;
    }
  | {
      type: "service/removed";
      payload: number;
    }
  | {
      type: "client/created";
      payload: LavaClient;
    }
  | {
      type: "client/updated";
      payload: LavaClient;
    }
  | {
      type: "client/removed";
      payload: number;
    }
  | {
      type: "work-order/created";
      payload: {
        workOrder: LavaProgressVehicle;
        financeEntry?: LavaFinanceEntry;
      };
    }
  | {
      type: "work-order/updated";
      payload: LavaProgressVehicle;
    }
  | {
      type: "finance-entry/updated";
      payload: LavaFinanceEntry;
    }
  | {
      type: "history/replaced";
      payload: LavaHistoryEntry[];
    };
