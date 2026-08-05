import type {
  SystemUserInput,
} from "./user.types";

export const USERNAME_MAX_LENGTH = 255;

export const PASSWORD_MIN_LENGTH = 6;

export const PASSWORD_POLICY_MESSAGE =
  "A senha deve ter pelo menos 6 caracteres.";

export function isPasswordAccepted(
  password: string
): boolean {
  return password.length >= PASSWORD_MIN_LENGTH;
}

export function normalizeSystemUserInput(
  input: SystemUserInput
): SystemUserInput {
  return {
    name: input.name.trim(),
    username: input.username.trim(),
    password: input.password,
    role: input.role,
  };
}
