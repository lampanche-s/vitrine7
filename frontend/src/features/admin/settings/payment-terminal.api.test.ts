import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";

const httpClient = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  patch: vi.fn(),
  delete: vi.fn(),
};

vi.mock("../../../shared/http", () => ({
  httpClient,
}));

describe("payment-terminal api", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.resetAllMocks();
  });

  it("usa os endpoints reais de providers e devices", async () => {
    httpClient.get
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce({
        providerCode: "SIMULATOR",
      })
      .mockResolvedValueOnce({
        code: "PAGBANK",
      })
      .mockResolvedValueOnce([]);

    const api = await import(
      "./payment-terminal.api"
    );

    await api.listPaymentTerminalProviders();
    await api.getPaymentTerminalProviderStatus();
    await api.getPaymentTerminalProviderProfile(
      "PAGBANK"
    );
    await api.listPaymentTerminalDevices();

    expect(httpClient.get).toHaveBeenNthCalledWith(
      1,
      "/payment-terminal/providers"
    );
    expect(httpClient.get).toHaveBeenNthCalledWith(
      2,
      "/payment-terminal/provider-status"
    );
    expect(httpClient.get).toHaveBeenNthCalledWith(
      3,
      "/payment-terminal/providers/PAGBANK/profile"
    );
    expect(httpClient.get).toHaveBeenNthCalledWith(
      4,
      "/payment-terminal/devices"
    );
  });

  it("cria device, gera código e revoga sem expor token", async () => {
    const api = await import(
      "./payment-terminal.api"
    );

    httpClient.post
      .mockResolvedValueOnce({
        id: "device-1",
        providerCode: "PAGBANK",
      })
      .mockResolvedValueOnce({
        deviceId: "device-1",
        pairingCode: "PAIR-123",
        expiresAt: "2026-07-19T13:10:00Z",
      })
      .mockResolvedValueOnce(undefined);

    await api.createPaymentTerminalDevice({
      providerCode: "PAGBANK",
      displayName: "Agente PagBank local",
      externalTerminalReference: null,
      platform: "LOCAL",
    });
    await api.createPaymentTerminalPairingCode(
      "device-1"
    );
    await api.revokePaymentTerminalDevice(
      "device-1"
    );

    expect(httpClient.post).toHaveBeenNthCalledWith(
      1,
      "/payment-terminal/devices",
      {
        providerCode: "PAGBANK",
        displayName: "Agente PagBank local",
        externalTerminalReference: null,
        platform: "LOCAL",
      }
    );
    expect(httpClient.post).toHaveBeenNthCalledWith(
      2,
      "/payment-terminal/devices/device-1/pairing-code"
    );
    expect(httpClient.post).toHaveBeenNthCalledWith(
      3,
      "/payment-terminal/devices/device-1/revoke"
    );
  });
});
