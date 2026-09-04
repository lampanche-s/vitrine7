import type {
  ReactNode,
} from "react";

import {
  LockKeyhole,
} from "lucide-react";

import {
  PremiumCard,
} from "../../components/ui";

import type {
  AppModuleId,
} from "./access.types";

import {
  useAccessControl,
} from "./useAccessControl";

export function ModuleAccessGuard({
  moduleId,
  children,
}: {
  moduleId: AppModuleId;
  children: ReactNode;
}) {
  const {
    canAccessModule,
  } = useAccessControl();

  if (canAccessModule(moduleId)) {
    return children;
  }

  return (
    <PremiumCard>
      <div
        role="alert"
        className="grid min-h-64 place-items-center p-6 text-center"
      >
        <div>
          <LockKeyhole className="mx-auto h-7 w-7 text-[var(--text-subtle)]" />

          <p className="mt-3 text-sm font-semibold text-[var(--text-base)]">
            Acesso indisponível
          </p>

          <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-[var(--text-subtle)]">
            Seu perfil não possui acesso a esta área.
          </p>
        </div>
      </div>
    </PremiumCard>
  );
}

export function ModuleVisibility({
  moduleId,
  children,
}: {
  moduleId: AppModuleId;
  children: ReactNode;
}) {
  const {
    canAccessModule,
  } = useAccessControl();

  return canAccessModule(moduleId)
    ? children
    : null;
}
