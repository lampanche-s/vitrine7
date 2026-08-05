import { useState } from "react";
import type { FormEvent } from "react";

export function LoginScreen({
  error: externalError = "",
  onLogin,
}: {
  error?: string;
  onLogin: (
    username: string,
    password: string
  ) => Promise<void>;
}) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [logoLoaded, setLogoLoaded] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setError("");
    setIsSubmitting(true);

    try {
      await onLogin(
        username,
        password
      );
    } catch (currentError) {
      setError(
        currentError instanceof Error
          ? currentError.message
          : "Não foi possível entrar."
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="login-background-subtle relative grid min-h-screen place-items-center overflow-hidden px-4 py-8 text-[var(--text-base)]">
      <section className="login-panel-in relative z-10 w-full max-w-[430px] -translate-y-12">
        <div className="mb-7 flex justify-center">
          <div className="relative aspect-[1872/1446] w-[min(430px,76vw)] max-w-none">
            <span
              aria-hidden="true"
              className={[
                "absolute inset-0 flex items-center justify-center text-center text-2xl font-semibold tracking-normal text-[var(--text-muted)]",
                logoLoaded ? "opacity-0" : "opacity-100",
              ].join(" ")}
            >
              Vitrine 7
            </span>

            <img
              src="/vitrine-slogan.png"
              alt="Vitrine 7"
              width={1872}
              height={1446}
              loading="eager"
              decoding="async"
              onLoad={() => setLogoLoaded(true)}
              className="relative z-10 h-full w-full object-contain"
            />
          </div>
        </div>

        <form
          onSubmit={handleSubmit}
          className="rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-card)] p-5 shadow-[var(--shadow-modal)]"
        >
          <div className="mb-5 border-b border-[var(--border-subtle)] pb-5">
            <p className="text-[11px] font-medium uppercase tracking-normal text-[var(--text-subtle)]">
              Acesso administrativo
            </p>

            <h2 className="mt-2 text-2xl font-semibold tracking-[-0.05em] text-[var(--text-base)]">
              Entrar no sistema
            </h2>

            <p className="mt-2 text-sm leading-6 text-[var(--text-muted)]">
              Informe suas credenciais para continuar.
            </p>
          </div>

          <div className="space-y-4">
            <LoginField
              label="Usuário"
              type="text"
              value={username}
              onChange={setUsername}
              placeholder="Digite seu usuário"
            />

            <LoginField
              label="Senha"
              type="password"
              value={password}
              onChange={setPassword}
              placeholder="Digite sua senha"
            />
          </div>

          {(error || externalError) && (
            <div className="mt-4 rounded-[3px] border border-red-500/10 bg-[var(--color-danger-soft)] px-3 py-3 text-xs text-[var(--color-danger)]">
              {error || externalError}
            </div>
          )}

          <button
            type="submit"
            disabled={isSubmitting}
            className="v7-motion-fast v7-pressable mt-5 h-11 w-full rounded-[4px] border border-[var(--border-subtle)] bg-white/[0.045] text-sm font-semibold text-[var(--text-base)] hover:border-[var(--border-hover)] hover:bg-white/[0.065]"
          >
            {isSubmitting
              ? "Entrando..."
              : "Acessar painel"}
          </button>
        </form>
      </section>
    </main>
  );
}

function LoginField({
  label,
  type,
  value,
  onChange,
  placeholder,
}: {
  label: string;
  type: "text" | "password";
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
}) {
  return (
    <label className="block">
      <span className="mb-2 block text-xs font-medium uppercase tracking-normal text-[var(--text-subtle)]">
        {label}
      </span>

      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        className="v7-motion-fast h-11 w-full rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 text-sm text-[var(--text-base)] outline-none placeholder:text-[var(--text-subtle)] hover:border-[var(--border-hover)] focus:border-[var(--border-hover)]"
      />
    </label>
  );
}
