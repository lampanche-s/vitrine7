import type {
  ReactNode,
} from "react";
import {
  useMemo,
} from "react";

import {
  repositories,
} from "../../data/repositories";

import {
  ToastProvider,
} from "../../components/ui";

import {
  AccessProvider,
  type AppSession,
} from "../access";

import {
  AdminDomainProvider,
  type AdminDomainState,
} from "../admin/state";

function createSessionInitialAdminDomainState(): AdminDomainState {
  return {
    users: [],
    settings: {
      companyName: "",
      cnpj: "",
      phone: "",
      address: "",
      adminMode: false,
    },
  };
}

export function AppProviders({
  children,
  session,
}: {
  children: ReactNode;
  session: AppSession;
}) {
  const initialAdminDomainState =
    useMemo(
      () =>
        createSessionInitialAdminDomainState(),
      []
    );

  return (
    <ToastProvider>
      <AdminDomainProvider
        initialState={
          initialAdminDomainState
        }
        repository={repositories.admin}
        permissions={session.permissions}
      >
        <AccessProvider session={session}>
          {children}
        </AccessProvider>
      </AdminDomainProvider>
    </ToastProvider>
  );
}
