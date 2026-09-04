import {
  useCallback,
  useEffect,
  useReducer,
  useState,
  type ReactNode,
} from "react";

import type {
  AdminRepository,
} from "../../../data/contracts";

import {
  useToast,
} from "../../../components/ui";

import type {
  SystemUserInput,
  SystemUserStatus,
} from "../../../entities/user";

import {
  useMutationLock,
} from "../../../shared/hooks/useMutationLock";

import {
  AdminDomainContext,
} from "./admin-domain.context";

import {
  adminDomainReducer,
} from "./admin-domain.reducer";

import type {
  AdminDomainState,
} from "./admin-domain.types";

type AdminDomainProviderProps = {
  children: ReactNode;
  initialState: AdminDomainState;
  repository: AdminRepository;
  permissions?: readonly string[];
};

function getErrorMessage(error: unknown): string {
  return error instanceof Error
    ? error.message
    : "Não foi possível concluir a operação.";
}

export function AdminDomainProvider({
  children,
  initialState,
  repository,
  permissions = [],
}: AdminDomainProviderProps) {
  const [state, dispatch] = useReducer(
    adminDomainReducer,
    initialState
  );

  const [isLoading, setIsLoading] =
    useState(false);

  const [error, setError] =
    useState<string | null>(null);

  const {
    showErrorToast,
  } = useToast();

  const {
    isMutating,
    runMutation,
  } = useMutationLock();

  const reload = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      const snapshot =
        repository.getSnapshotForPermissions
          ? await repository.getSnapshotForPermissions(
              permissions
            )
          : await repository.getSnapshot();

      dispatch({
        type: "repository/snapshot-loaded",

        payload: {
          users: snapshot.users,
        },
      });
    } catch (currentError) {
      const message =
        getErrorMessage(currentError);

      setError(message);
      showErrorToast(currentError, {
        title: "Não foi possível carregar os dados",
        dedupeKey: `admin-reload|${message}`,
      });
    } finally {
      setIsLoading(false);
    }
  }, [permissions, repository, showErrorToast]);

  useEffect(() => {
    queueMicrotask(() => {
      void reload();
    });
  }, [reload]);

  function executeUserMutation<T>(
    operation: () => Promise<T>,
    ignoredResult: T
  ): Promise<T> {
    return runMutation(
      async () => {
        setError(null);

        try {
          return await operation();
        } catch (currentError) {
          const message =
            getErrorMessage(currentError);

          setError(message);
          showErrorToast(currentError, {
            title: "Não foi possível salvar o usuário",
            dedupeKey: `admin-user|${message}`,
          });

          return ignoredResult;
        }
      },
      ignoredResult
    );
  }

  function createUser(
    input: SystemUserInput
  ): Promise<boolean> {
    return executeUserMutation(
      async () => {
      const createdUser =
        await repository.createUser(input);

      dispatch({
        type: "user/created",
        payload: createdUser,
      });

      return true;
      },
      false
    );
  }

  function updateUser(
    userId: number,
    input: SystemUserInput
  ): Promise<boolean> {
    return executeUserMutation(
      async () => {
      const updatedUser =
        await repository.updateUser(
          userId,
          input
        );

      dispatch({
        type: "user/updated",
        payload: updatedUser,
      });

      return true;
      },
      false
    );
  }

  function setUserStatus(
    userId: number,
    status: SystemUserStatus
  ): Promise<boolean> {
    return executeUserMutation(
      async () => {
      const updatedUser =
        await repository.setUserStatus(
          userId,
          status
        );

      dispatch({
        type: "user/updated",
        payload: updatedUser,
      });

      return true;
      },
      false
    );
  }

  function resetUserPassword(
    userId: number,
    newPassword: string
  ): Promise<boolean> {
    return executeUserMutation(
      async () => {
      const updatedUser =
        await repository.resetUserPassword(
          userId,
          newPassword
        );

      dispatch({
        type: "user/updated",
        payload: updatedUser,
      });

      return true;
      },
      false
    );
  }

  function removeUser(
    userId: number
  ): Promise<boolean> {
    return executeUserMutation(
      async () => {
      await repository.removeUser(userId);

      dispatch({
        type: "user/removed",
        payload: userId,
      });

      return true;
      },
      false
    );
  }

  return (
    <AdminDomainContext.Provider
      value={{
        state,
        isLoading,
        isMutating,
        error,
        reload,

        createUser,
        updateUser,
        setUserStatus,
        resetUserPassword,
        removeUser,
      }}
    >
      {children}
    </AdminDomainContext.Provider>
  );
}
