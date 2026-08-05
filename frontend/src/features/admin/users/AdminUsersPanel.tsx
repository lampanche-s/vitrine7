import {
  useMemo,
  useState,
} from "react";

import {
  Plus,
  X,
} from "lucide-react";

import {
  AdminSelect,
  AnimatedModal,
  Button,
  EmptyState,
  PageActions,
  PremiumCard,
  SectionTitle,
  SearchField,
  TextField,
  useToast,
} from "../../../components/ui";

import {
  PASSWORD_POLICY_MESSAGE,
  USERNAME_MAX_LENGTH,
  isPasswordAccepted,
  normalizeSystemUserInput,
} from "../../../entities/user";

import type {
  AppUserRole,
} from "../../access";

import type {
  SystemUser,
  SystemUserInput,
  SystemUserRole,
  SystemUserStatus,
} from "../../../entities/user";

const emptySystemUserForm: SystemUserInput = {
  name: "",
  username: "",
  password: "",
  role: "Operador",
  status: "Ativo",
};

function getSystemUserForm(user: SystemUser): SystemUserInput {
  return {
    name: user.name,
    username: user.username,
    password: user.password,
    role: user.role,
    status: user.status,
  };
}

export function AdminUsersPanel({
  users,
  currentUserId,
  currentUserRole,
  onCreate,
  onUpdate,
  onSetStatus,
  onResetPassword,
  onRemove,
}: {
  users: SystemUser[];
  currentUserId: number;
  currentUserRole: AppUserRole;

  onCreate: (
    input: SystemUserInput
  ) => Promise<boolean>;

  onUpdate: (
    userId: number,
    input: SystemUserInput
  ) => Promise<boolean>;

  onSetStatus: (
    userId: number,
    status: SystemUserStatus
  ) => Promise<boolean>;

  onResetPassword: (
    userId: number,
    newPassword: string
  ) => Promise<boolean>;

  onRemove: (
    userId: number
  ) => Promise<boolean>;
}) {
  const [createUserModalOpen, setCreateUserModalOpen] =
    useState(false);
  const [manageUserModalOpen, setManageUserModalOpen] =
    useState(false);
  const [deleteUserModalOpen, setDeleteUserModalOpen] =
    useState(false);
  const [resetPasswordModalOpen, setResetPasswordModalOpen] =
    useState(false);
  const [resetPasswordUser, setResetPasswordUser] =
    useState<SystemUser | null>(null);
  const [selectedUser, setSelectedUser] =
    useState<SystemUser | null>(null);
  const [form, setForm] =
    useState<SystemUserInput>(emptySystemUserForm);
  const [formError, setFormError] = useState("");
  const [deleteError, setDeleteError] = useState("");
  const [resetPasswordError, setResetPasswordError] =
    useState("");
  const [newPassword, setNewPassword] =
    useState("");
  const [confirmPassword, setConfirmPassword] =
    useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const {
    showToast,
  } = useToast();

  const isSuperAdmin =
    currentUserRole === "SUPER_ADMIN";

  const availableRoles: SystemUserRole[] =
    isSuperAdmin
      ? [
          "Operador",
          "Administrador",
        ]
      : [
          "Operador",
        ];

  const currentSelectedUser = useMemo(
    () =>
      selectedUser
        ? users.find((user) => user.id === selectedUser.id) ??
          selectedUser
        : null,
    [selectedUser, users]
  );

  const filteredUsers = users.filter((user) => {
    if (user.id === currentUserId) {
      return false;
    }

    if (!isSuperAdmin && user.role !== "Operador") {
      return false;
    }

    const normalizedSearch = searchTerm.trim().toLowerCase();

    const matchesSearch =
      normalizedSearch.length === 0 ||
      user.name.toLowerCase().includes(normalizedSearch) ||
      user.username.toLowerCase().includes(normalizedSearch) ||
      user.role.toLowerCase().includes(normalizedSearch);

    return matchesSearch;
  });

  function updateForm(
    field: keyof SystemUserInput,
    value: string
  ) {
    setFormError("");

    setForm((current) => {
      switch (field) {
        case "status":
          return {
            ...current,
            status: value as SystemUserStatus,
          };

        case "role":
          return {
            ...current,
            role: value as SystemUserRole,
          };

        default:
          return {
            ...current,
            [field]: value,
          };
      }
    });
  }

  function openCreateUserModal() {
    setManageUserModalOpen(false);
    setDeleteUserModalOpen(false);
    setResetPasswordModalOpen(false);
    setResetPasswordUser(null);
    setSelectedUser(null);
    setForm(emptySystemUserForm);
    setFormError("");
    setDeleteError("");
    setResetPasswordError("");
    setCreateUserModalOpen(true);
  }

  function closeCreateUserModal() {
    setCreateUserModalOpen(false);
    setForm(emptySystemUserForm);
    setFormError("");
  }

  function openManageUserModal(user: SystemUser) {
    setCreateUserModalOpen(false);
    setDeleteUserModalOpen(false);
    setResetPasswordModalOpen(false);
    setResetPasswordUser(null);
    setSelectedUser(user);
    setForm({
      ...getSystemUserForm(user),
      role: isSuperAdmin
        ? user.role
        : "Operador",
    });
    setFormError("");
    setDeleteError("");
    setResetPasswordError("");
    setManageUserModalOpen(true);
  }

  function closeManageUserModal() {
    setManageUserModalOpen(false);
    setSelectedUser(null);
    setForm(emptySystemUserForm);
    setFormError("");
  }

  function openDeleteUserModal(user: SystemUser) {
    setCreateUserModalOpen(false);
    setManageUserModalOpen(false);
    setResetPasswordModalOpen(false);
    setResetPasswordUser(null);
    setSelectedUser(user);
    setDeleteError("");
    setDeleteUserModalOpen(true);
  }

  function closeDeleteUserModal() {
    setDeleteUserModalOpen(false);
    setSelectedUser(null);
    setDeleteError("");
  }

  function openResetPasswordModal(user: SystemUser) {
    setCreateUserModalOpen(false);
    setDeleteUserModalOpen(false);
    setResetPasswordUser(user);
    setNewPassword("");
    setConfirmPassword("");
    setResetPasswordError("");
    setResetPasswordModalOpen(true);
  }

  function closeResetPasswordModal() {
    setResetPasswordModalOpen(false);
    setResetPasswordUser(null);
    setNewPassword("");
    setConfirmPassword("");
    setResetPasswordError("");
  }

  function validateForm(requirePassword: boolean) {
    const normalizedForm =
      normalizeSystemUserInput(form);

    if (!normalizedForm.name) {
      setFormError("Informe o nome.");
      return null;
    }

    if (!normalizedForm.username) {
      setFormError("Informe um nome de usuário.");
      return null;
    }

    if (normalizedForm.username.length > USERNAME_MAX_LENGTH) {
      setFormError(
        "O nome de usuário deve possuir no máximo 255 caracteres."
      );
      return null;
    }

    if (requirePassword && !normalizedForm.password) {
      setFormError("Informe a senha.");
      return null;
    }

    if (
      requirePassword &&
      !isPasswordAccepted(normalizedForm.password)
    ) {
      setFormError(PASSWORD_POLICY_MESSAGE);
      return null;
    }

    if (
      normalizedForm.name &&
      normalizedForm.username &&
      (!requirePassword || normalizedForm.password)
    ) {
      return normalizedForm;
    }

    setFormError("Preencha todos os campos obrigatórios antes de salvar.");
    return null;
  }

  async function createUser() {
    const normalizedForm = validateForm(true);

    if (!normalizedForm) {
      return;
    }

    const created =
      await onCreate({
        ...normalizedForm,
        status: "Ativo",
      });

    if (!created) {
      setFormError(
        "Não foi possível cadastrar o usuário."
      );
      return;
    }


    showToast({
      title: `Usuário ${normalizedForm.name} criado.`,
      variant: "success",
      dedupeKey: `user-created|${normalizedForm.username}`,
    });

    closeCreateUserModal();
  }

  async function saveUserChanges() {
    if (!currentSelectedUser) {
      setFormError("Usuário não encontrado.");
      return;
    }

    const normalizedForm = validateForm(false);

    if (!normalizedForm) {
      return;
    }

    const updated = await onUpdate(
      currentSelectedUser.id,
      normalizedForm
    );

    if (!updated) {
      setFormError(
        "Não foi possível atualizar o usuário."
      );
      return;
    }


    showToast({
      title: `Usuário ${normalizedForm.name} editado.`,
      variant: "success",
      dedupeKey: `user-updated|${currentSelectedUser.id}`,
    });

    closeManageUserModal();
  }

  async function resetPassword() {
    if (!resetPasswordUser) {
      setResetPasswordError("Usuário não encontrado.");
      return;
    }

    if (newPassword.length === 0) {
      setResetPasswordError("Informe a nova senha.");
      return;
    }

    if (!isPasswordAccepted(newPassword)) {
      setResetPasswordError(
        PASSWORD_POLICY_MESSAGE
      );
      return;
    }

    if (newPassword !== confirmPassword) {
      setResetPasswordError("As senhas não coincidem.");
      return;
    }

    const reset =
      await onResetPassword(
        resetPasswordUser.id,
        newPassword
      );

    if (!reset) {
      setResetPasswordError(
        "Não foi possível redefinir a senha."
      );
      return;
    }


    closeResetPasswordModal();
  }

  async function toggleUserStatus(user: SystemUser) {
    const nextStatus =
      user.status === "Ativo"
        ? "Bloqueado"
        : "Ativo";

    const updated =
      await onSetStatus(user.id, nextStatus);

    if (!updated) {
      return;
    }


    setForm((current) => ({
      ...current,
      status: nextStatus,
    }));

    showToast({
      title:
        nextStatus === "Ativo"
          ? `Usuário ${user.name} desbloqueado.`
          : `Usuário ${user.name} bloqueado.`,
      variant:
        nextStatus === "Ativo"
          ? "success"
          : "warning",
      dedupeKey: `user-status|${user.id}|${nextStatus}`,
    });
  }

  async function deleteUser() {
    if (!currentSelectedUser) {
      setDeleteError("Usuário não encontrado.");
      return;
    }

    const removed =
      await onRemove(currentSelectedUser.id);

    if (!removed) {
      setDeleteError(
        "Não foi possível excluir o usuário."
      );
      return;
    }


    showToast({
      title: `Usuário ${currentSelectedUser.name} excluído.`,
      variant: "warning",
      dedupeKey: `user-removed|${currentSelectedUser.id}`,
    });

    closeDeleteUserModal();
  }

  return (
    <>
      <PageActions>
        <Button
          variant="primary"
          leadingIcon={<Plus />}
          onClick={openCreateUserModal}
        >
          Novo usuário
        </Button>
      </PageActions>

      <PremiumCard className="v7-card-fill" contentClassName="v7-card-content">
        <div className="v7-card-header">
          <SearchField
            value={searchTerm}
            onChange={setSearchTerm}
            placeholder="Buscar por nome, usuário ou perfil..."
          />
        </div>

        <div className="clean-inner-list v7-list-scroll premium-scroll mt-4 pr-1">
          {filteredUsers.length === 0 && (
            <EmptyState
              message="Nenhum usuário encontrado."
            />
          )}

          {filteredUsers.map((user) => (
            <div
              key={user.id}
              className="grid min-h-[68px] w-full grid-cols-[minmax(0,1fr)_auto] items-center gap-4 border-b border-[var(--border-subtle)] py-3 text-left last:border-b-0"
            >
              <span className="min-w-0">
                <span className="block truncate text-sm font-semibold text-[var(--text-base)]">
                  {user.name}
                </span>

                <span className="mt-1 block truncate text-sm text-[var(--text-muted)]">
                  Usuário: {user.username}
                </span>

                <span className="mt-1 block text-xs text-[var(--text-subtle)]">
                  {user.role}
                </span>
              </span>

              <Button
                size="compact"
                variant="secondary"
                onClick={() =>
                  openManageUserModal(user)
                }
              >
                Gerenciar
              </Button>
            </div>
          ))}
        </div>
      </PremiumCard>

      <UserFormModal
        open={createUserModalOpen}
        title="Novo usuário"
        description="Cadastre um novo acesso administrativo."
        form={form}
        formError={formError}
        submitLabel="Criar usuário"
        onClose={closeCreateUserModal}
        onSubmit={() => void createUser()}
        onUpdateForm={updateForm}
        showPassword
        availableRoles={availableRoles}
      />

      <AnimatedModal
        open={manageUserModalOpen}
        onClose={closeManageUserModal}
        labelledBy="admin-user-manage-title"
        describedBy="admin-user-manage-description"
        backdropClassName="z-[220] p-4"
        panelClassName="w-full max-w-[680px] overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
      >
        {currentSelectedUser ? (
          <>
            <div className="flex items-start justify-between gap-4">
              <SectionTitle
                compact
                title="Gerenciar usuário"
                subtitle="Atualize dados, status e acesso do usuário selecionado."
              />

              <Button
                size="icon"
                variant="ghost"
                onClick={closeManageUserModal}
                leadingIcon={<X />}
                aria-label="Fechar modal"
                title="Fechar"
              />
            </div>

            <div
              id="admin-user-manage-description"
              className="mt-4 rounded-[var(--panel-radius)] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 py-3"
            >
              <h2
                id="admin-user-manage-title"
                className="text-[var(--font-size-section-title)] font-semibold text-[var(--text-base)]"
              >
                {currentSelectedUser.name}
              </h2>

              <p className="mt-1 text-sm text-[var(--text-muted)]">
                Usuário: {currentSelectedUser.username}
              </p>
            </div>

            <div className="mt-5 grid gap-5 lg:grid-cols-[minmax(0,1fr)_220px]">
              <div className="min-w-0">
                <UserFormFields
                  form={form}
                  onUpdateForm={updateForm}
                  showPassword={false}
                  availableRoles={availableRoles}
                />

                {formError ? (
                  <FormErrorMessage message={formError} />
                ) : null}
              </div>

              <aside className="border-t border-[var(--border-subtle)] pt-4 lg:border-l lg:border-t-0 lg:pl-4 lg:pt-0">
                <SectionTitle compact title="Ações" />

                <div className="mt-4 grid gap-2">
                  <Button
                    size="compact"
                    variant="secondary"
                    fullWidth
                    onClick={() =>
                      void toggleUserStatus(
                        currentSelectedUser
                      )
                    }
                  >
                    {currentSelectedUser.status === "Ativo"
                      ? "Bloquear usuário"
                      : "Ativar usuário"}
                  </Button>

                  <Button
                    size="compact"
                    variant="secondary"
                    fullWidth
                    onClick={() =>
                      openResetPasswordModal(
                        currentSelectedUser
                      )
                    }
                  >
                    Redefinir senha
                  </Button>

                  <Button
                    size="compact"
                    variant="danger"
                    fullWidth
                    onClick={() =>
                      openDeleteUserModal(
                        currentSelectedUser
                      )
                    }
                  >
                    Excluir usuário
                  </Button>
                </div>
              </aside>
            </div>

            <div className="mt-5 flex flex-col-reverse gap-2 border-t border-[var(--border-subtle)] pt-5 sm:flex-row sm:justify-end">
              <Button
                variant="secondary"
                onClick={closeManageUserModal}
              >
                Cancelar
              </Button>

              <Button
                variant="primary"
                onClick={() =>
                  void saveUserChanges()
                }
              >
                Salvar alterações
              </Button>
            </div>
          </>
        ) : null}
      </AnimatedModal>

      <AnimatedModal
        open={deleteUserModalOpen}
        onClose={closeDeleteUserModal}
        labelledBy="admin-user-delete-title"
        describedBy="admin-user-delete-description"
        backdropClassName="z-[230] p-4"
        panelClassName="w-full max-w-md overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
      >
        {currentSelectedUser ? (
          <>
            <div className="flex items-start justify-between gap-4">
              <SectionTitle
                compact
                title="Excluir usuário"
                subtitle="Confirme antes de remover este acesso."
              />

              <Button
                size="icon"
                variant="ghost"
                onClick={closeDeleteUserModal}
                leadingIcon={<X />}
                aria-label="Fechar modal"
                title="Fechar"
              />
            </div>

            <div
              id="admin-user-delete-description"
              className="mt-5 rounded-[var(--panel-radius)] border border-[var(--color-danger-border)] bg-[var(--color-danger-soft)] px-3 py-3"
            >
              <h2
                id="admin-user-delete-title"
                className="text-sm font-semibold text-[var(--color-danger)]"
              >
                {currentSelectedUser.name}
              </h2>

              <p className="mt-1 text-sm text-[var(--text-muted)]">
                Usuário: {currentSelectedUser.username}
              </p>
            </div>

            {deleteError ? (
              <FormErrorMessage message={deleteError} />
            ) : null}

            <div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <Button
                variant="secondary"
                onClick={closeDeleteUserModal}
              >
                Cancelar
              </Button>

              <Button
                variant="danger"
                onClick={() => void deleteUser()}
              >
                Excluir
              </Button>
            </div>
          </>
        ) : null}
      </AnimatedModal>

      <AnimatedModal
        open={resetPasswordModalOpen}
        onClose={closeResetPasswordModal}
        labelledBy="admin-user-reset-password-title"
        describedBy="admin-user-reset-password-description"
        backdropClassName="z-[230] p-4"
        panelClassName="w-full max-w-[460px] overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
      >
        {resetPasswordUser ? (
          <>
            <div className="flex items-start justify-between gap-4">
              <SectionTitle
                compact
                title="Redefinir senha"
                subtitle="Defina uma nova senha para o usuário."
              />

              <Button
                size="icon"
                variant="ghost"
                onClick={closeResetPasswordModal}
                leadingIcon={<X />}
                aria-label="Fechar modal"
                title="Fechar"
              />
            </div>

            <div
              id="admin-user-reset-password-description"
              className="mt-4 rounded-[var(--panel-radius)] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 py-3"
            >
              <h2
                id="admin-user-reset-password-title"
                className="text-[var(--font-size-section-title)] font-semibold text-[var(--text-base)]"
              >
                {resetPasswordUser.name}
              </h2>

              <p className="mt-1 text-sm text-[var(--text-muted)]">
                Usuário: {resetPasswordUser.username}
              </p>
            </div>

            <div className="mt-5 grid gap-4">
              <TextField
                label="Nova senha"
                type="password"
                value={newPassword}
                onChange={(value) => {
                  setResetPasswordError("");
                  setNewPassword(value);
                }}
              />

              <TextField
                label="Confirmar nova senha"
                type="password"
                value={confirmPassword}
                onChange={(value) => {
                  setResetPasswordError("");
                  setConfirmPassword(value);
                }}
              />

              {resetPasswordError ? (
                <FormErrorMessage message={resetPasswordError} />
              ) : null}
            </div>

            <div className="mt-5 flex flex-col-reverse gap-2 border-t border-[var(--border-subtle)] pt-5 sm:flex-row sm:justify-end">
              <Button
                variant="secondary"
                onClick={closeResetPasswordModal}
              >
                Cancelar
              </Button>

              <Button
                variant="primary"
                onClick={() => void resetPassword()}
              >
                Salvar nova senha
              </Button>
            </div>
          </>
        ) : null}
      </AnimatedModal>
    </>
  );
}

function UserFormModal({
  open,
  title,
  description,
  form,
  formError,
  submitLabel,
  onClose,
  onSubmit,
  onUpdateForm,
  showPassword,
  availableRoles,
}: {
  open: boolean;
  title: string;
  description: string;
  form: SystemUserInput;
  formError: string;
  submitLabel: string;
  onClose: () => void;
  onSubmit: () => void;
  onUpdateForm: (
    field: keyof SystemUserInput,
    value: string
  ) => void;
  showPassword: boolean;
  availableRoles: SystemUserRole[];
}) {
  return (
    <AnimatedModal
      open={open}
      onClose={onClose}
      labelledBy="admin-user-create-title"
      describedBy="admin-user-create-description"
      backdropClassName="z-[220] p-4"
      panelClassName="w-full max-w-[560px] overflow-hidden rounded-[var(--panel-radius)] border border-[var(--border-soft)] bg-[var(--surface-card)] p-[var(--panel-padding)] shadow-[var(--shadow-modal)]"
    >
      <div className="flex items-start justify-between gap-4">
        <SectionTitle
          compact
          title={title}
          subtitle={description}
        />

        <Button
          size="icon"
          variant="ghost"
          onClick={onClose}
          leadingIcon={<X />}
          aria-label="Fechar modal"
          title="Fechar"
        />
      </div>

      <h2 id="admin-user-create-title" className="sr-only">
        {title}
      </h2>

      <p id="admin-user-create-description" className="sr-only">
        {description}
      </p>

      <div className="mt-5">
        <UserFormFields
          form={form}
          onUpdateForm={onUpdateForm}
          showPassword={showPassword}
          showStatus={false}
          availableRoles={availableRoles}
        />

        {formError ? (
          <FormErrorMessage message={formError} />
        ) : null}
      </div>

      <div className="mt-5 flex flex-col-reverse gap-2 border-t border-[var(--border-subtle)] pt-5 sm:flex-row sm:justify-end">
        <Button
          variant="secondary"
          onClick={onClose}
        >
          Cancelar
        </Button>

        <Button
          variant="primary"
          onClick={onSubmit}
        >
          {submitLabel}
        </Button>
      </div>
    </AnimatedModal>
  );
}

function UserFormFields({
  form,
  onUpdateForm,
  showPassword = true,
  showStatus = true,
  availableRoles,
}: {
  form: SystemUserInput;
  onUpdateForm: (
    field: keyof SystemUserInput,
    value: string
  ) => void;
  showPassword?: boolean;
  showStatus?: boolean;
  availableRoles: SystemUserRole[];
}) {
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <TextField
        label="Nome"
        value={form.name}
        onChange={(value) =>
          onUpdateForm("name", value)
        }
      />

      <TextField
        label="Usuário"
        value={form.username}
        onChange={(value) =>
          onUpdateForm("username", value)
        }
      />

      {showPassword ? (
        <TextField
          label="Senha"
          type="password"
          value={form.password}
          onChange={(value) =>
            onUpdateForm("password", value)
          }
        />
      ) : null}

      <AdminSelect
        label="Perfil"
        value={form.role}
        options={availableRoles}
        onChange={(value) =>
          onUpdateForm("role", value)
        }
      />

      {showStatus ? (
        <AdminSelect
          label="Status"
          value={form.status}
          options={[
            "Ativo",
            "Bloqueado",
          ]}
          onChange={(value) =>
            onUpdateForm(
              "status",
              value
            )
          }
        />
      ) : null}
    </div>
  );
}

function FormErrorMessage({
  message,
}: {
  message: string;
}) {
  return (
    <div
      className="mt-4 rounded-[var(--control-radius)] border border-[var(--color-danger-border)] bg-[var(--color-danger-soft)] px-3 py-2 text-sm text-[var(--color-danger)]"
      role="alert"
    >
      {message}
    </div>
  );
}

