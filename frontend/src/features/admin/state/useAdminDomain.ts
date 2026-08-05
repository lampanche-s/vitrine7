import { useContext } from "react";

import {
  AdminDomainContext,
} from "./admin-domain.context";

export function useAdminDomain() {
  const context = useContext(AdminDomainContext);

  if (!context) {
    throw new Error(
      "useAdminDomain deve ser usado dentro do provider de administração."
    );
  }

  return context;
}
