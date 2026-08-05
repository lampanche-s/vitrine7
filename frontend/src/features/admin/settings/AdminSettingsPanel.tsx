import {
  useCallback,
  useEffect,
  useState,
} from "react";

import {
  AlertTriangle,
  CheckCircle2,
  KeyRound,
  LoaderCircle,
  Unplug,
} from "lucide-react";

import {
  PremiumCard,
  SectionTitle,
} from "../../../components/ui";

import {
  useAccessControl,
} from "../../access";
import {
  HttpError,
} from "../../../shared/http";
import {
  useToast,
} from "../../../components/ui";
import {
  activatePaymentTerminalProvider,
  createPaymentTerminalDevice,
  createPaymentTerminalPairingCode,
  createPaymentTerminalProviderProfile,
  getPaymentTerminalProviderProfile,
  getPaymentTerminalProviderStatus,
  listPaymentTerminalProviders,
  listPaymentTerminalDevices,
  revokePaymentTerminalDevice,
  type PaymentTerminalDevice,
  type PaymentTerminalProviderCatalogItem,
  type PaymentTerminalProviderCode,
  type PaymentTerminalProviderProfile,
  type PaymentTerminalProviderStatus,
} from "./payment-terminal.api";

const paymentProviderOrder: PaymentTerminalProviderCode[] = [
  "SIMULATOR",
  "PAGBANK",
];

const paymentProviderFallbacks: Record<
  PaymentTerminalProviderCode,
  {
    displayName: string;
    logoText: string;
    logoSrc?: string;
  }
> = {
  SIMULATOR: {
    displayName: "Simulador",
    logoText: "SIM",
  },
  PAGBANK: {
    displayName: "PagBank",
    logoText: "PagBank",
    logoSrc: "/payment-providers/pagbank.svg",
  },
};

type PaymentProviderView = {
  code: PaymentTerminalProviderCode;
  displayName: string;
  implementationStatus: string;
  adapterAvailable: boolean;
  canBeActivated: boolean;
  active: boolean;
  configured: boolean;
  environment: string | null;
  profile: PaymentTerminalProviderProfile | null;
};

export function AdminSettingsPanel() {
  return (
    <PremiumCard
      className="v7-card-fill"
      contentClassName="v7-card-content"
    >
      <PaymentIntegrationSection />
    </PremiumCard>
  );
}

function PaymentIntegrationSection() {
  const {
    can,
  } = useAccessControl();
  const {
    showToast,
    showErrorToast,
  } = useToast();
  const [providers, setProviders] =
    useState<PaymentProviderView[]>([]);
  const [activeProvider, setActiveProvider] =
    useState<PaymentTerminalProviderStatus | null>(null);
  const [devices, setDevices] =
    useState<PaymentTerminalDevice[]>([]);
  const [pairingCodes, setPairingCodes] =
    useState<Record<string, {
      code: string;
      expiresAt: string;
    }>>({});
  const [isLoading, setIsLoading] =
    useState(true);
  const [isActivating, setIsActivating] =
    useState<PaymentTerminalProviderCode | null>(null);
  const [isGeneratingPairingCode, setIsGeneratingPairingCode] =
    useState(false);
  const [isTestingAgent, setIsTestingAgent] =
    useState(false);
  const [revokingDeviceId, setRevokingDeviceId] =
    useState<string | null>(null);
  const [error, setError] =
    useState<string | null>(null);

  const canManagePaymentConfig =
    can("admin:payment-config");

  const loadProviders = useCallback(async () => {
    if (!canManagePaymentConfig) {
      setProviders([]);
      setActiveProvider(null);
      setDevices([]);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const catalog =
        await listPaymentTerminalProviders();

      const visibleCatalog =
        paymentProviderOrder
          .map((code) =>
            catalog.find(
              (provider) =>
                provider.code === code
            )
          )
          .filter(
            (
              provider
            ): provider is PaymentTerminalProviderCatalogItem =>
              Boolean(provider)
          );

      const [statusResult, profileResults, deviceResults] =
        await Promise.all([
          getPaymentTerminalProviderStatus()
            .then((status) => status)
            .catch((currentError) => {
              if (
                currentError instanceof HttpError &&
                currentError.status === 404
              ) {
                return null;
              }

              throw currentError;
            }),
          Promise.all(
            visibleCatalog.map((provider) =>
              getPaymentTerminalProviderProfile(
                provider.code
              ).catch(() => null)
            )
          ),
          listPaymentTerminalDevices(),
        ]);

      setActiveProvider(statusResult);
      setDevices(deviceResults);
      setProviders(
        visibleCatalog.map((provider, index) =>
          toPaymentProviderView(
            provider,
            profileResults[index]
          )
        )
      );
    } catch (currentError) {
      const message =
        getPaymentTerminalErrorMessage(currentError);

      setError(message);
      showErrorToast(message, {
        title: "Não foi possível carregar a maquininha",
        dedupeKey: `payment-terminal-load|${message}`,
      });
    } finally {
      setIsLoading(false);
    }
  }, [canManagePaymentConfig, showErrorToast]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadProviders();
    });
  }, [loadProviders]);

  async function handleActivate(
    providerCode: PaymentTerminalProviderCode
  ) {
    if (
      isActivating ||
      !canManagePaymentConfig
    ) {
      return;
    }

    setIsActivating(providerCode);

    try {
      const currentProvider = providers.find(
        (provider) =>
          provider.code === providerCode
      );

      if (
        providerCode === "PAGBANK" &&
        !currentProvider?.profile?.profile
      ) {
        const pagBankDevice = devices.find(
          (device) =>
            device.providerCode === "PAGBANK" &&
            device.status !== "REVOKED"
        );

        await createPaymentTerminalProviderProfile({
          providerCode: "PAGBANK",
          displayName: "PagBank",
          environment: "LOCAL",
          enabled: true,
          active: false,
          merchantReference: null,
          terminalReference:
            pagBankDevice?.externalTerminalReference ??
            null,
          publicConfiguration: {},
          credentials: {},
          version: null,
        });
      }

      await activatePaymentTerminalProvider(
        providerCode
      );

      await loadProviders();
    } catch (currentError) {
      const message =
        getPaymentTerminalErrorMessage(currentError);

      showErrorToast(message, {
        title: "Não foi possível ativar",
        dedupeKey: `payment-terminal-activate|${message}`,
      });
    } finally {
      setIsActivating(null);
    }
  }

  async function handleCreatePairingCode() {
    if (
      isGeneratingPairingCode ||
      !canManagePaymentConfig
    ) {
      return;
    }

    setIsGeneratingPairingCode(true);

    try {
      const pagBankDevices = devices.filter(
        (device) =>
          device.providerCode === "PAGBANK" &&
          device.status !== "REVOKED"
      );
      let targetDevice = pagBankDevices[0];

      if (!targetDevice) {
        targetDevice =
          await createPaymentTerminalDevice({
            providerCode: "PAGBANK",
            displayName: "Agente PagBank local",
            externalTerminalReference: null,
            platform: "LOCAL",
          });
      }
      const pairing =
        await createPaymentTerminalPairingCode(
          targetDevice.id
        );

      setPairingCodes((current) => ({
        ...current,
        [pairing.deviceId]: {
          code: pairing.pairingCode,
          expiresAt: pairing.expiresAt,
        },
      }));

      await loadProviders();
    } catch (currentError) {
      const message =
        getPaymentTerminalErrorMessage(currentError);

      showErrorToast(message, {
        title: "Não foi possível gerar o código",
        dedupeKey: `payment-terminal-pairing|${message}`,
      });
    } finally {
      setIsGeneratingPairingCode(false);
    }
  }

  async function handleTestPagBankAgent() {
    if (
      isTestingAgent ||
      !canManagePaymentConfig
    ) {
      return;
    }

    setIsTestingAgent(true);

    try {
      const currentDevices =
        await listPaymentTerminalDevices();

      setDevices(currentDevices);

      showToast({
        title: "Status atualizado",
        description:
          "A situação do agente PagBank foi consultada novamente.",
        variant: "success",
        duration: 4000,
        dedupeKey: "payment-terminal-status-updated",
      });
    } catch (currentError) {
      const message =
        getPaymentTerminalErrorMessage(currentError);

      showErrorToast(message, {
        title: "Não foi possível testar o agente",
        dedupeKey: `payment-terminal-test|${message}`,
      });
    } finally {
      setIsTestingAgent(false);
    }
  }

  async function handleRevokeDevice(
    deviceId: string
  ) {
    if (
      revokingDeviceId ||
      !canManagePaymentConfig
    ) {
      return;
    }

    setRevokingDeviceId(deviceId);

    try {
      await revokePaymentTerminalDevice(deviceId);
      setPairingCodes((current) => {
        const next = {
          ...current,
        };
        delete next[deviceId];
        return next;
      });
      await loadProviders();
    } catch (currentError) {
      const message =
        getPaymentTerminalErrorMessage(currentError);

      showErrorToast(message, {
        title: "Não foi possível revogar",
        dedupeKey: `payment-terminal-revoke|${message}`,
      });
    } finally {
      setRevokingDeviceId(null);
    }
  }

  return (
    <div className="flex min-h-0 flex-1 flex-col gap-4">
      <div className="v7-card-header">
        <div>
          <SectionTitle compact title="Integrações" />
          <p className="mt-1 text-sm text-zinc-600">
            Maquininha
          </p>
        </div>
      </div>

      {!canManagePaymentConfig ? (
        <PaymentTerminalNotice
          title="Permissão necessária"
          text="Sua sessão não possui permissão para administrar a integração de maquininha."
        />
      ) : null}

      {isLoading ? (
        <PaymentTerminalLoading />
      ) : error ? (
        null
      ) : (
        <div className="v7-list-fill min-w-0 flex-1">
          <div className="v7-list-scroll premium-scroll grid gap-2 pr-1">
            {providers.map((provider) => (
              <PaymentTerminalProviderRow
                key={provider.code}
                provider={provider}
                active={
                  activeProvider?.providerCode ===
                    provider.code ||
                  provider.active
                }
                disabled={!canManagePaymentConfig}
                activating={
                  isActivating === provider.code
                }
                devices={devices.filter(
                  (device) =>
                    device.providerCode ===
                    provider.code
                )}
                pairingCodes={pairingCodes}
                generatingPairingCode={
                  isGeneratingPairingCode
                }
                testingAgent={isTestingAgent}
                revokingDeviceId={revokingDeviceId}
                onActivate={() =>
                  void handleActivate(
                    provider.code
                  )
                }
                onCreatePairingCode={() =>
                  void handleCreatePairingCode()
                }
                onTestAgent={() =>
                  void handleTestPagBankAgent()
                }
                onRevokeDevice={(deviceId) =>
                  void handleRevokeDevice(deviceId)
                }
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function PaymentProviderLogo({
  provider,
}: {
  provider: PaymentProviderView;
}) {
  const fallback =
    paymentProviderFallbacks[provider.code];

  return (
    <span className="grid h-12 w-12 shrink-0 place-items-center rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-card-strong)] p-2 text-xs font-semibold text-zinc-300">
      {fallback.logoSrc ? (
        <img
          src={fallback.logoSrc}
          alt={provider.displayName}
          className="max-h-full max-w-full object-contain"
          onError={(event) => {
            event.currentTarget.style.display = "none";
            const parent =
              event.currentTarget.parentElement;

            if (parent) {
              parent.textContent =
                fallback.logoText;
            }
          }}
        />
      ) : (
        fallback.logoText
      )}
    </span>
  );
}

function PaymentTerminalProviderRow({
  provider,
  active,
  disabled,
  activating,
  devices,
  pairingCodes,
  generatingPairingCode,
  testingAgent,
  revokingDeviceId,
  onActivate,
  onCreatePairingCode,
  onTestAgent,
  onRevokeDevice,
}: {
  provider: PaymentProviderView;
  active: boolean;
  disabled: boolean;
  activating: boolean;
  devices: PaymentTerminalDevice[];
  pairingCodes: Record<string, {
    code: string;
    expiresAt: string;
  }>;
  generatingPairingCode: boolean;
  testingAgent: boolean;
  revokingDeviceId: string | null;
  onActivate: () => void;
  onCreatePairingCode: () => void;
  onTestAgent: () => void;
  onRevokeDevice: (deviceId: string) => void;
}) {
  const canActivate =
    provider.canBeActivated &&
    provider.adapterAvailable &&
    !active &&
    !disabled;

  return (
    <article className="rounded-[4px] border border-[var(--border-subtle)] bg-[var(--surface-control)] px-3 py-3">
      <div className="grid gap-3 md:grid-cols-[52px_minmax(0,1fr)_minmax(160px,auto)_auto] md:items-center">
        <PaymentProviderLogo provider={provider} />

        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <p className="truncate text-sm font-semibold text-white">
              {provider.displayName}
            </p>

            {active ? (
              <span className="inline-flex h-6 items-center gap-1 rounded-[4px] border border-emerald-500/20 bg-emerald-500/[0.08] px-2 text-xs font-medium text-emerald-200">
                <CheckCircle2
                  className="h-3.5 w-3.5"
                  aria-hidden="true"
                />
                Ativo
              </span>
            ) : null}
          </div>

          <p className="mt-1 text-xs text-zinc-600">
            {getProviderDescription(provider)}
          </p>
        </div>

        <div className="min-w-0">
          <p className="text-[11px] font-medium uppercase tracking-normal text-zinc-600">
            Estado
          </p>

          <p className="mt-1 text-sm font-medium text-zinc-300">
            {getProviderStateLabel(provider)}
          </p>
        </div>

        <div className="flex md:justify-end">
          {active ? (
            <span className="inline-flex h-9 items-center rounded-[4px] border border-white/[0.08] px-3 text-sm font-medium text-zinc-500">
              Selecionado
            </span>
          ) : canActivate ? (
            <button
              type="button"
              onClick={onActivate}
              disabled={activating}
              className="inline-flex h-9 items-center justify-center gap-2 rounded-[4px] border border-[#F2C94C]/30 bg-[#F2C94C]/10 px-3 text-sm font-medium text-[#F2C94C] transition hover:bg-[#F2C94C]/15 disabled:cursor-wait disabled:opacity-65"
            >
              {activating ? (
                <LoaderCircle
                  className="h-4 w-4 animate-spin"
                  aria-hidden="true"
                />
              ) : null}
              Ativar
            </button>
          ) : (
            <span className="inline-flex h-9 items-center rounded-[4px] border border-white/[0.08] px-3 text-sm font-medium text-zinc-600">
              Indisponível
            </span>
          )}
        </div>
      </div>

      {provider.code === "PAGBANK" ? (
        <PagBankBridgePanel
          devices={devices}
          pairingCodes={pairingCodes}
          disabled={disabled}
          generatingPairingCode={
            generatingPairingCode
          }
          testingAgent={testingAgent}
          revokingDeviceId={revokingDeviceId}
          onCreatePairingCode={
            onCreatePairingCode
          }
          onTestAgent={onTestAgent}
          onRevokeDevice={onRevokeDevice}
        />
      ) : null}
    </article>
  );
}

function PagBankBridgePanel({
  devices,
  pairingCodes,
  disabled,
  generatingPairingCode,
  testingAgent,
  revokingDeviceId,
  onCreatePairingCode,
  onTestAgent,
  onRevokeDevice,
}: {
  devices: PaymentTerminalDevice[];
  pairingCodes: Record<string, {
    code: string;
    expiresAt: string;
  }>;
  disabled: boolean;
  generatingPairingCode: boolean;
  testingAgent: boolean;
  revokingDeviceId: string | null;
  onCreatePairingCode: () => void;
  onTestAgent: () => void;
  onRevokeDevice: (deviceId: string) => void;
}) {
  const hasOnlineAgent =
    devices.some(
      (device) =>
        device.status === "ACTIVE"
    );

  return (
    <div className="mt-3 border-t border-white/[0.06] pt-3">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-normal text-zinc-500">
            Agente local
          </p>
          <p className="mt-1 text-sm text-zinc-400">
            {hasOnlineAgent
              ? "Agente PagBank conectado ao bridge."
              : "Nenhum agente PagBank online no momento."}
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={onTestAgent}
            disabled={disabled || testingAgent}
            className="inline-flex h-9 items-center justify-center gap-2 rounded-[4px] border border-white/[0.08] bg-white/[0.035] px-3 text-xs font-medium text-zinc-300 transition hover:bg-white/[0.055] disabled:cursor-not-allowed disabled:opacity-60"
          >
            {testingAgent ? (
              <LoaderCircle
                className="h-4 w-4 animate-spin"
                aria-hidden="true"
              />
            ) : (
              <CheckCircle2
                className="h-4 w-4"
                aria-hidden="true"
              />
            )}
            Testar agente
          </button>

          <button
            type="button"
            onClick={onCreatePairingCode}
            disabled={disabled || generatingPairingCode}
            className="inline-flex h-9 items-center justify-center gap-2 rounded-[4px] border border-[#F2C94C]/30 bg-[#F2C94C]/10 px-3 text-xs font-medium text-[#F2C94C] transition hover:bg-[#F2C94C]/15 disabled:cursor-wait disabled:opacity-65"
          >
            {generatingPairingCode ? (
              <LoaderCircle
                className="h-4 w-4 animate-spin"
                aria-hidden="true"
              />
            ) : (
              <KeyRound
                className="h-4 w-4"
                aria-hidden="true"
              />
            )}
            Gerar código
          </button>
        </div>
      </div>

      <div className="mt-3 grid gap-2">
        {devices.length === 0 ? (
          <div className="rounded-[4px] border border-white/[0.06] bg-white/[0.025] px-3 py-3 text-sm text-zinc-500">
            Nenhum device PagBank pareado.
          </div>
        ) : (
          devices.map((device) => (
            <PagBankDeviceRow
              key={device.id}
              device={device}
              pairingCode={pairingCodes[device.id]}
              revoking={
                revokingDeviceId === device.id
              }
              disabled={disabled}
              onRevoke={() =>
                onRevokeDevice(device.id)
              }
            />
          ))
        )}
      </div>
    </div>
  );
}

function PagBankDeviceRow({
  device,
  pairingCode,
  revoking,
  disabled,
  onRevoke,
}: {
  device: PaymentTerminalDevice;
  pairingCode?: {
    code: string;
    expiresAt: string;
  };
  revoking: boolean;
  disabled: boolean;
  onRevoke: () => void;
}) {
  const canRevoke =
    device.status !== "REVOKED" &&
    !disabled;

  return (
    <div className="grid gap-3 rounded-[4px] border border-white/[0.06] bg-white/[0.025] p-3 md:grid-cols-[minmax(0,1.4fr)_minmax(150px,0.9fr)_auto] md:items-center">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <p className="truncate text-sm font-medium text-zinc-200">
            {device.displayName ||
              "Agente PagBank"}
          </p>
          <PaymentTerminalDeviceStatusBadge
            status={device.status}
          />
        </div>

        <p className="mt-1 truncate text-xs text-zinc-500">
          {device.externalTerminalReference ||
            "Sem referência do terminal"}
        </p>

        {pairingCode ? (
          <p className="mt-2 inline-flex rounded-[4px] border border-[#F2C94C]/25 bg-[#F2C94C]/10 px-2 py-1 font-mono text-sm font-semibold tracking-normal text-[#F2C94C]">
            {pairingCode.code}
          </p>
        ) : null}
      </div>

      <div className="min-w-0">
        <p className="text-[11px] font-medium uppercase tracking-normal text-zinc-600">
          Último heartbeat
        </p>
        <p className="mt-1 text-sm text-zinc-300">
          {formatPaymentTerminalDateTime(
            device.lastSeenAt
          )}
        </p>
        {pairingCode ? (
          <p className="mt-1 text-xs text-zinc-500">
            Código expira em{" "}
            {formatPaymentTerminalDateTime(
              pairingCode.expiresAt
            )}
          </p>
        ) : null}
      </div>

      <div className="flex md:justify-end">
        <button
          type="button"
          onClick={onRevoke}
          disabled={!canRevoke || revoking}
          className="inline-flex h-9 items-center justify-center gap-2 rounded-[4px] border border-red-500/20 bg-red-500/[0.07] px-3 text-xs font-medium text-red-200 transition hover:bg-red-500/10 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {revoking ? (
            <LoaderCircle
              className="h-4 w-4 animate-spin"
              aria-hidden="true"
            />
          ) : (
            <Unplug
              className="h-4 w-4"
              aria-hidden="true"
            />
          )}
          Revogar
        </button>
      </div>
    </div>
  );
}

function PaymentTerminalDeviceStatusBadge({
  status,
}: {
  status: PaymentTerminalDevice["status"];
}) {
  const className =
    status === "ACTIVE"
      ? "border-emerald-500/20 bg-emerald-500/[0.08] text-emerald-200"
      : status === "OFFLINE"
        ? "border-amber-500/20 bg-amber-500/[0.08] text-amber-100"
        : status === "REVOKED"
          ? "border-red-500/20 bg-red-500/[0.07] text-red-200"
          : "border-white/[0.08] bg-white/[0.035] text-zinc-400";

  return (
    <span
      className={[
        "inline-flex h-6 items-center rounded-[4px] border px-2 text-xs font-medium",
        className,
      ].join(" ")}
    >
      {getDeviceStatusLabel(status)}
    </span>
  );
}

function PaymentTerminalLoading() {
  return (
    <div
      role="status"
      aria-live="polite"
      className="flex items-center gap-3 rounded-[4px] border border-white/[0.08] bg-white/[0.025] px-4 py-3"
    >
      <LoaderCircle
        className="h-4 w-4 animate-spin text-[#F2C94C]"
        aria-hidden="true"
      />

      <p className="text-sm font-medium text-zinc-300">
        Carregando providers de maquininha
      </p>
    </div>
  );
}

function PaymentTerminalNotice({
  title,
  text,
}: {
  title: string;
  text: string;
}) {
  return (
    <div
      role="alert"
      className="flex items-start gap-3 rounded-[4px] border border-amber-500/20 bg-amber-500/[0.07] p-4"
    >
      <AlertTriangle
        className="mt-0.5 h-5 w-5 shrink-0 text-amber-300"
        aria-hidden="true"
      />

      <div className="min-w-0">
        <p className="text-sm font-semibold text-amber-100">
          {title}
        </p>

        <p className="mt-1 text-sm leading-5 text-amber-100/65">
          {text}
        </p>
      </div>
    </div>
  );
}

function toPaymentProviderView(
  provider: PaymentTerminalProviderCatalogItem,
  profile: PaymentTerminalProviderProfile | null
): PaymentProviderView {
  const fallback =
    paymentProviderFallbacks[provider.code];

  return {
    code: provider.code,
    displayName:
      provider.code === "SIMULATOR"
        ? fallback.displayName
        : provider.displayName || fallback.displayName,
    implementationStatus:
      provider.implementationStatus,
    adapterAvailable:
      provider.adapterAvailable,
    canBeActivated:
      provider.canBeActivated,
    active:
      provider.active ||
      Boolean(profile?.profile?.active),
    configured:
      provider.configured ||
      Boolean(profile?.configured),
    environment:
      provider.environment ??
      profile?.profile?.environment ??
      null,
    profile,
  };
}

function getProviderStateLabel(
  provider: PaymentProviderView
): string {
  if (
    provider.implementationStatus ===
    "IMPLEMENTATION_PENDING"
  ) {
    return "Integração pendente";
  }

  if (
    provider.canBeActivated &&
    provider.adapterAvailable
  ) {
    return "Disponível";
  }

  return "Indisponível";
}

function getProviderDescription(
  provider: PaymentProviderView
): string {
  if (provider.code === "SIMULATOR") {
    return "Provider local para testes administrativos.";
  }

  if (provider.code === "PAGBANK") {
    return "Integração local com maquininha PagBank via agente PlugPag.";
  }

  return "Provider de pagamento.";
}

function getDeviceStatusLabel(
  status: PaymentTerminalDevice["status"]
): string {
  if (status === "ACTIVE") {
    return "Conectado";
  }

  if (status === "OFFLINE") {
    return "Offline";
  }

  if (status === "REVOKED") {
    return "Revogado";
  }

  return "Aguardando pareamento";
}

function formatPaymentTerminalDateTime(
  value: string | null
): string {
  if (!value) {
    return "Ainda não recebido";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Data indisponível";
  }

  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

function getPaymentTerminalErrorMessage(
  error: unknown
): string {
  if (error instanceof HttpError) {
    if (error.status === 403) {
      return "Sua sessão não possui permissão para administrar a maquininha.";
    }

    return error.message;
  }

  return error instanceof Error
    ? error.message
    : "Não foi possível concluir a operação.";
}
