export type SystemUserRole =
  | "Administrador"
  | "Operador";

export type SystemUserStatus =
  | "Ativo"
  | "Bloqueado";

export type SystemUser = {
  id: number;
  name: string;
  username: string;
  role: SystemUserRole;
  status: SystemUserStatus;
};

export type SystemUserInput = {
  name: string;
  username: string;
  password: string;
  role: SystemUserRole;
};
