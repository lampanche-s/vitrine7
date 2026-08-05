import type { ElementType } from "react";

export type TabId =
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
