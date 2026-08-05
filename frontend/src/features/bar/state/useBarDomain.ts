import { useContext } from "react";

import {
  BarDomainContext,
} from "./bar-domain.context";

export function useBarDomain() {
  const context = useContext(BarDomainContext);

  if (!context) {
    throw new Error(
      "useBarDomain deve ser usado dentro de BarDomainProvider."
    );
  }

  return context;
}
