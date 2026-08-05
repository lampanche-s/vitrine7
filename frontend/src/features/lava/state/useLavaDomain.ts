import { useContext } from "react";

import {
  LavaDomainContext,
} from "./lava-domain.context";

export function useLavaDomain() {
  const context = useContext(LavaDomainContext);

  if (!context) {
    throw new Error(
      "useLavaDomain deve ser usado dentro de LavaDomainProvider."
    );
  }

  return context;
}
