import {
  ContentStack,
  SectionTitle,
} from "../../components/ui";

import {
  useAdminDomain,
} from "./state";

import {
  useAccessControl,
} from "../access";

import {
  AdminUsersPanel,
} from "./users";

import {
  AdminSettingsPanel,
} from "./settings";

export type AdminTab =
  | "users"
  | "settings";

export function AdminPanel({
  activePanel,
}: {
  activePanel: AdminTab;
}) {
  const {
    canAccessAdminPanel,
    session,
  } = useAccessControl();

  const {
    state: domainState,
    createUser,
    updateUser,
    setUserStatus,
    resetUserPassword,
    removeUser,
  } = useAdminDomain();

  const {
    users,
  } = domainState;

  const canViewActivePanel =
    activePanel === "users"
      ? canAccessAdminPanel("users")
      : canAccessAdminPanel(
          "settings"
        );

  if (!canViewActivePanel) {
    return null;
  }

  return (
    <ContentStack>
      {activePanel === "settings" ? (
        <SectionTitle title="Configurações" />
      ) : null}

      {activePanel === "users" ? (
        <AdminUsersPanel
          users={users}
          currentUserId={
            session.userId
          }
          currentUserRole={
            session.role
          }
          onCreate={createUser}
          onUpdate={updateUser}
          onSetStatus={
            setUserStatus
          }
          onResetPassword={
            resetUserPassword
          }
          onRemove={removeUser}
        />
      ) : (
        <AdminSettingsPanel />
      )}
    </ContentStack>
  );
}
