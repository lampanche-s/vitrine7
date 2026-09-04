import type {
  BarCatalogItem,
} from "../../../entities/catalog-item";

import type {
  BarCommand,
} from "../../../entities/command";

import type {
  BarSaleHistoryEntry,
} from "../../../entities/sale-history";

export type BarDomainState = {
  commands: BarCommand[];
  catalogEntries: BarCatalogItem[];
  historyEntries: BarSaleHistoryEntry[];
};

export type BarDomainAction =
  | {
      type: "repository/snapshot-loaded";
      payload: BarDomainState;
    }
  | {
      type: "history/replaced";
      payload: BarSaleHistoryEntry[];
    }
  | {
      type: "command/created";
      payload: BarCommand;
    }
  | {
      type: "command/updated";
      payload: BarCommand;
    }
  | {
      type: "command/removed";
      payload: number;
    }
  | {
      type: "command/closed";
      payload: {
        commandId: number;
        historyEntry: BarSaleHistoryEntry;
        catalogEntries: BarCatalogItem[] | null;
      };
    }
  | {
      type: "catalog/created";
      payload: BarCatalogItem;
    }
  | {
      type: "catalog/updated";
      payload: BarCatalogItem;
    }
  | {
      type: "catalog/removed";
      payload: number;
    };
