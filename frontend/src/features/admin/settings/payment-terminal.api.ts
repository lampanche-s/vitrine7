import {
  httpClient,
} from "../../../shared/http";

export type PaymentTerminalProviderCode =
  | "SIMULATOR"
  | "PAGBANK";

export type PaymentTerminalProviderCatalogItem = {
  code: PaymentTerminalProviderCode;
  displayName: string;
  implementationStatus: string;
  adapterAvailable: boolean;
  canBeActivated: boolean;
  active: boolean;
  configured: boolean;
  environment: string | null;
  capabilities?: unknown;
};

export type PaymentTerminalPublicProfile = {
  id: string;
  providerCode: PaymentTerminalProviderCode;
  displayName: string;
  environment: string;
  enabled: boolean;
  active: boolean;
  merchantReference: string | null;
  terminalReference: string | null;
  publicConfiguration: unknown;
  credentialsConfigured: boolean;
  configurationVersion: number;
  updatedAt: string;
  version: number;
};

export type PaymentTerminalCreateProviderProfileRequest = {
  providerCode: PaymentTerminalProviderCode;
  displayName: string;
  environment: "LOCAL";
  enabled: boolean;
  active: boolean;
  merchantReference: string | null;
  terminalReference: string | null;
  publicConfiguration: Record<string, unknown>;
  credentials: Record<string, string>;
  version: null;
};

export type PaymentTerminalProviderStatus = {
  activeProfileId: string;
  providerCode: PaymentTerminalProviderCode;
  displayName: string;
  implementationStatus: string;
  environment: string;
  enabled: boolean;
  configurationVersion: number;
  adapterAvailable: boolean;
  profile: PaymentTerminalPublicProfile;
};

export type PaymentTerminalProviderProfile = {
  code: PaymentTerminalProviderCode;
  displayName: string;
  implementationStatus: string;
  adapterAvailable: boolean;
  canBeActivated: boolean;
  configured: boolean;
  profile: PaymentTerminalPublicProfile | null;
  capabilities?: unknown;
};

export type PaymentTerminalDeviceStatus =
  | "PENDING_PAIRING"
  | "ACTIVE"
  | "OFFLINE"
  | "REVOKED";

export type PaymentTerminalDevice = {
  id: string;
  providerProfileId: string | null;
  providerCode: PaymentTerminalProviderCode;
  displayName: string;
  externalTerminalReference: string | null;
  status: PaymentTerminalDeviceStatus;
  platform: string;
  agentVersion: string | null;
  capabilities?: unknown;
  pairedAt: string | null;
  lastSeenAt: string | null;
  revokedAt: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type PaymentTerminalCreateDeviceRequest = {
  providerProfileId?: string | null;
  providerCode: PaymentTerminalProviderCode;
  displayName: string;
  externalTerminalReference?: string | null;
  platform: string;
};

export type PaymentTerminalPairingCode = {
  deviceId: string;
  pairingCode: string;
  expiresAt: string;
};

export async function listPaymentTerminalProviders() {
  return httpClient.get<
    PaymentTerminalProviderCatalogItem[]
  >("/payment-terminal/providers");
}

export async function getPaymentTerminalProviderStatus() {
  return httpClient.get<
    PaymentTerminalProviderStatus
  >("/payment-terminal/provider-status");
}

export async function getPaymentTerminalProviderProfile(
  providerCode: PaymentTerminalProviderCode
) {
  return httpClient.get<
    PaymentTerminalProviderProfile
  >(
    `/payment-terminal/providers/${providerCode}/profile`
  );
}

export async function createPaymentTerminalProviderProfile(
  request: PaymentTerminalCreateProviderProfileRequest
) {
  return httpClient.post<
    PaymentTerminalPublicProfile
  >(
    "/payment-terminal/provider-profiles",
    request
  );
}

export async function activatePaymentTerminalProvider(
  providerCode: PaymentTerminalProviderCode
) {
  return httpClient.post<
    PaymentTerminalPublicProfile
  >(
    `/payment-terminal/providers/${providerCode}/activate`
  );
}

export async function listPaymentTerminalDevices() {
  return httpClient.get<
    PaymentTerminalDevice[]
  >("/payment-terminal/devices");
}

export async function createPaymentTerminalDevice(
  request: PaymentTerminalCreateDeviceRequest
) {
  return httpClient.post<
    PaymentTerminalDevice
  >(
    "/payment-terminal/devices",
    request
  );
}

export async function createPaymentTerminalPairingCode(
  deviceId: string
) {
  return httpClient.post<
    PaymentTerminalPairingCode
  >(
    `/payment-terminal/devices/${deviceId}/pairing-code`
  );
}

export async function revokePaymentTerminalDevice(
  deviceId: string
) {
  return httpClient.post<void>(
    `/payment-terminal/devices/${deviceId}/revoke`
  );
}
