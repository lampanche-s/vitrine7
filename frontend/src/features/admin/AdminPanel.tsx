import {
  ContentStack,
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

export function AdminPanel() {
  const {
    can,
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

  if (!can("admin:users")) {
    return null;
  }

  return (
    <ContentStack>
      <AdminUsersPanel
        users={domainState.users}
        currentUserId={session.userId}
        currentUserRole={session.role}
        onCreate={createUser}
        onUpdate={updateUser}
        onSetStatus={setUserStatus}
        onResetPassword={resetUserPassword}
        onRemove={removeUser}
      />
    </ContentStack>
  );
}
