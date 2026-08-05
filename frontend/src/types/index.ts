import type { ElementType } from "react";

export type TabId =
  | "lava-service"
  | "lava-history"
  | "lava-clients"
  | "lava-reports"
  | "bar-order"
  | "bar-menu"
  | "bar-finance"
  | "bar-reports";

export type TabItem = {
  id: TabId;
  label: string;
  shortLabel: string;
  icon: ElementType;
};
