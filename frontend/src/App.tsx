import {
  useEffect,
  useState,
} from "react";

import {
  BarChart3,
  ClipboardList,
  WalletCards,
  History,
  Menu,
  NotebookTabs,
  Truck,
  UserCog,
  Users,
  X,
} from "lucide-react";

import type {
  TabId,
} from "./types";
import {
  AdminPanel,
} from "./features/admin/AdminPanel";
import {
  AppProviders,
} from "./features/app";
import type {
  AppPermission,
  AppSession,
} from "./features/access";
import {
  useAccessControl,
} from "./features/access";
import { BarContent } from "./features/bar/BarContent";
import { Header } from "./features/layout/Header";
import { ClientsContent } from "./features/clients";
import { SuppliersContent } from "./features/suppliers";
import { EmployeesContent } from "./features/employees";
import { CashClosingContent } from "./features/cash-closing";
import { LoginScreen } from "./features/login/LoginScreen";
import {
  AUTH_SESSION_EXPIRED_EVENT,
  HttpError,
} from "./shared/http";
import {
  httpAuthRepository,
} from "./data/http/httpAuthRepository";

type AuthStatus =
  | "checking"
  | "authenticated"
  | "unauthenticated";

type AppSection =
  | "commands"
  | "history"
  | "catalog"
  | "clients"
  | "employees"
  | "suppliers"
  | "reports"
  | "cash-closing"
  | "users";

type NavigationItem = {
  id: AppSection;
  label: string;
  icon: typeof ClipboardList;
  permission: AppPermission;
};

const navigationItems: NavigationItem[] = [
  {
    id: "commands",
    label: "Comandas",
    icon: ClipboardList,
    permission: "bar:access",
  },
  {
    id: "history",
    label: "Histórico",
    icon: History,
    permission: "bar:access",
  },
  {
    id: "catalog",
    label: "Cadastro",
    icon: NotebookTabs,
    permission: "bar:manage-catalog",
  },
  {
    id: "clients",
    label: "Clientes",
    icon: Users,
    permission: "clients:manage",
  },
  {
    id: "employees",
    label: "Vale/Consumo",
    icon: Users,
    permission: "clients:manage",
  },
  {
    id: "suppliers",
    label: "Fornecedores",
    icon: Truck,
    permission: "bar:manage-catalog",
  },
  {
    id: "reports",
    label: "Relatórios",
    icon: BarChart3,
    permission: "reports:access",
  },
  {
    id: "cash-closing",
    label: "Fechamento de Caixa",
    icon: WalletCards,
    permission: "reports:access",
  },
  {
    id: "users",
    label: "Usuários",
    icon: UserCog,
    permission: "admin:users",
  },
];

const barTabBySection: Record<
  Exclude<
    AppSection,
    "clients" | "employees" | "suppliers" | "cash-closing" | "users"
  >,
  TabId
> = {
  commands: "bar-order",
  history: "bar-finance",
  catalog: "bar-menu",
  reports: "bar-reports",
};

function AppContent({
  onLogout,
}: {
  onLogout: () => Promise<void>;
}) {
  const [activeSection, setActiveSection] =
    useState<AppSection>("commands");

  const [
    sidebarOpen,
    setSidebarOpen,
  ] = useState(true);

  const {
    can,
  } = useAccessControl();

  const activeNavigation =
    activeSection;

  const visibleNavigationItems =
    navigationItems.filter((item) =>
      can(item.permission)
    );

  function renderActiveContent() {
    switch (activeSection) {
      case "clients":
        return <ClientsContent />;

      case "employees":
        return <EmployeesContent />;

      case "suppliers":
        return <SuppliersContent />;

      case "cash-closing":
        return <CashClosingContent />;

      case "users":
        return <AdminPanel />;

      default:
        return (
          <BarContent
            activeTab={
              barTabBySection[
                activeSection
              ]
            }
          />
        );
    }
  }

  const activeContent =
    renderActiveContent();

  function handleNavigation(
    itemId: NavigationItem["id"]
  ) {
    setActiveSection(itemId);
  }

  return (
    <main className="app-auth-shell app-background-subtle h-dvh overflow-hidden text-[var(--text-base)]">
      <div
        className="grid h-full min-h-0 transition-[grid-template-columns] duration-300 ease-out motion-reduce:transition-none"
        style={{
          gridTemplateColumns: sidebarOpen
            ? "224px minmax(0, 1fr)"
            : "76px minmax(0, 1fr)",
        }}
      >
        <aside
          id="main-sidebar"
          className="flex min-h-0 min-w-0 flex-col overflow-hidden border-r border-[var(--border-subtle)] bg-[#0b1118]/90"
        >
          <div
            className={[
              "flex h-[82px] shrink-0 items-center border-b border-[var(--border-subtle)] transition-[padding-left] duration-300 ease-out motion-reduce:transition-none",
              sidebarOpen
                ? "pl-7"
                : "pl-[18px]",
            ].join(" ")}
          >
            <button
              type="button"
              aria-controls="main-sidebar"
              aria-expanded={sidebarOpen}
              aria-label={
                sidebarOpen
                  ? "Fechar menu lateral"
                  : "Abrir menu lateral"
              }
              title={
                sidebarOpen
                  ? "Fechar menu lateral"
                  : "Abrir menu lateral"
              }
              onClick={() =>
                setSidebarOpen(
                  (current) => !current
                )
              }
              className="relative flex h-10 w-10 shrink-0 items-center justify-center rounded-[5px] text-[var(--text-base)] transition-[background-color,transform] duration-200 hover:bg-[var(--surface-hover)] hover:text-[var(--text-base)] active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-accent-border)]"
            >
              <Menu
                aria-hidden="true"
                className={[
                  "absolute left-1/2 top-1/2 h-6 w-6 -translate-x-1/2 -translate-y-1/2 transition-[opacity,transform] duration-300 ease-out motion-reduce:transition-none",
                  sidebarOpen
                    ? "rotate-90 scale-75 opacity-0"
                    : "rotate-0 scale-100 opacity-100",
                ].join(" ")}
              />

              <X
                aria-hidden="true"
                className={[
                  "absolute left-1/2 top-1/2 h-6 w-6 -translate-x-1/2 -translate-y-1/2 transition-[opacity,transform] duration-300 ease-out motion-reduce:transition-none",
                  sidebarOpen
                    ? "rotate-0 scale-100 opacity-100"
                    : "-rotate-90 scale-75 opacity-0",
                ].join(" ")}
              />
            </button>
          </div>

          <nav
            className={[
              "premium-scroll flex min-h-0 flex-1 flex-col gap-2 overflow-x-hidden overflow-y-auto py-4 transition-[padding] duration-300 ease-out motion-reduce:transition-none",
              sidebarOpen
                ? "px-3"
                : "px-2",
            ].join(" ")}
            aria-label="Navegação principal"
          >
            {visibleNavigationItems.map(
              (item) => {
                const Icon = item.icon;
                const active =
                  activeNavigation === item.id;

                return (
                  <button
                    key={item.id}
                    type="button"
                    aria-current={
                      active
                        ? "page"
                        : undefined
                    }
                    title={
                      sidebarOpen
                        ? undefined
                        : item.label
                    }
                    onClick={() =>
                      handleNavigation(item.id)
                    }
                    className={[
                      "flex min-h-[66px] w-full items-center overflow-hidden rounded-[6px] border text-sm font-medium transition-[gap,padding,background-color,border-color,color] duration-300 ease-out motion-reduce:transition-none",
                      sidebarOpen
                        ? "gap-4 px-5"
                        : "gap-0 pl-[18px] pr-0",
                      active
                        ? "border-[var(--border-subtle)] bg-[var(--surface-hover)] text-[var(--color-accent)]"
                        : "border-transparent text-[var(--text-base)] hover:bg-[var(--surface-raised)] hover:text-[var(--text-base)]",
                    ].join(" ")}
                  >
                    <Icon
                      className={[
                        "h-6 w-6 shrink-0",
                        active
                          ? "text-[var(--color-accent)]"
                          : "text-[var(--text-base)]",
                      ].join(" ")}
                      aria-hidden="true"
                    />

                    <span
                      aria-hidden={!sidebarOpen}
                      className={[
                        "min-w-0 overflow-hidden whitespace-nowrap transition-[max-width,opacity,transform] duration-300 ease-out motion-reduce:transition-none",
                        sidebarOpen
                          ? "max-w-[150px] translate-x-0 opacity-100"
                          : "max-w-0 -translate-x-2 opacity-0",
                      ].join(" ")}
                    >
                      {item.label}
                    </span>
                  </button>
                );
              }
            )}
          </nav>
        </aside>

        <div className="flex min-h-0 min-w-0 flex-col">
          <Header
            onLogout={onLogout}
          />

          <section className="min-h-0 flex-1 overflow-hidden">
            <div className="premium-scroll h-full overflow-y-auto px-4 py-5 sm:px-6 lg:px-8 lg:py-6">
              {activeContent}
            </div>
          </section>
        </div>
      </div>

    </main>
  );
}

export default function App() {
  const [
    authStatus,
    setAuthStatus,
  ] = useState<AuthStatus>(
    "checking"
  );
  const [
    session,
    setSession,
  ] = useState<AppSession | null>(
    null
  );
  const [
    loginError,
    setLoginError,
  ] = useState("");

  useEffect(() => {
    function handleSessionExpired() {
      setSession(null);
      setAuthStatus("unauthenticated");
    }

    window.addEventListener(
      AUTH_SESSION_EXPIRED_EVENT,
      handleSessionExpired
    );

    return () => {
      window.removeEventListener(
        AUTH_SESSION_EXPIRED_EVENT,
        handleSessionExpired
      );
    };
  }, []);

  useEffect(() => {
    let isMounted = true;

    async function loadCurrentSession() {
      try {
        const currentSession =
          await httpAuthRepository.getCurrentSession();

        if (!isMounted) {
          return;
        }

        setSession(currentSession);
        setAuthStatus("authenticated");
      } catch (error) {
        if (!isMounted) {
          return;
        }

        setSession(null);

        if (
          error instanceof HttpError &&
          error.status !== 401
        ) {
          setLoginError(error.message);
        }

        setAuthStatus("unauthenticated");
      }
    }

    void loadCurrentSession();

    return () => {
      isMounted = false;
    };
  }, []);

  async function handleLogin(
    username: string,
    password: string
  ) {
    setLoginError("");

    const currentSession =
      await httpAuthRepository.login({
        username:
          username.trim(),
        password,
      });

    setSession(currentSession);
    setAuthStatus("authenticated");
  }

  async function handleLogout() {
    try {
      await httpAuthRepository.logout();
    } finally {
      setSession(null);
      setAuthStatus("unauthenticated");
    }
  }

  if (authStatus === "checking") {
    return (
      <main className="login-background-subtle grid min-h-screen place-items-center px-4 text-sm text-[var(--text-muted)]">
        Carregando sessão...
      </main>
    );
  }

  if (
    authStatus !== "authenticated" ||
    session === null
  ) {
    return (
      <LoginScreen
        error={loginError}
        onLogin={handleLogin}
      />
    );
  }

  return (
    <AppProviders session={session}>
      <AppContent onLogout={handleLogout} />
    </AppProviders>
  );
}
