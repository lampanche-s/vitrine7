import {
  useContext,
} from "react";

import {
  AccessContext,
} from "./access.context";

export function useAccessControl() {
  const context =
    useContext(AccessContext);

  if (!context) {
    throw new Error(
      "useAccessControl deve ser usado dentro de AccessProvider."
    );
  }

  return context;
}
