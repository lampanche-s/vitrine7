import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";

vi.mock("../config", () => ({
  appEnv: {
    apiBaseUrl: "http://localhost:8080/api/v1",
    httpTimeoutMs: 10_000,
  },
}));

describe("httpClient session activity", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.restoreAllMocks();
    const events = new EventTarget();

    vi.stubGlobal("document", {
      cookie: "",
    });
    vi.stubGlobal("window", {
      setTimeout,
      clearTimeout,
      addEventListener:
        events.addEventListener.bind(events),
      removeEventListener:
        events.removeEventListener.bind(events),
      dispatchEvent:
        events.dispatchEvent.bind(events),
    });
  });

  it("marca requisicoes normais como atividade do usuario", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        new Response("{}", {
          status: 200,
          headers: {
            "Content-Type": "application/json",
          },
        })
      );

    vi.stubGlobal("fetch", fetchMock);

    const {
      httpClient,
    } = await import("./http-client");

    await httpClient.get("/auth/me");

    const request = fetchMock.mock.calls[0]?.[1] as
      | RequestInit
      | undefined;

    expect(
      (request?.headers as Headers).get(
        "X-User-Activity"
      )
    ).toBe("true");
  });

  it("permite requisicoes silenciosas sem renovar atividade", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        new Response("{}", {
          status: 200,
          headers: {
            "Content-Type": "application/json",
          },
        })
      );

    vi.stubGlobal("fetch", fetchMock);

    const {
      httpClient,
    } = await import("./http-client");

    await httpClient.get("/health", {
      userActivity: false,
    });

    const request = fetchMock.mock.calls[0]?.[1] as
      | RequestInit
      | undefined;

    expect(
      (request?.headers as Headers).has(
        "X-User-Activity"
      )
    ).toBe(false);
  });

  it("dispara evento quando a sessao expira", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        new Response(
          JSON.stringify({
            message: "Faça login para continuar.",
          }),
          {
            status: 401,
            headers: {
              "Content-Type": "application/json",
            },
          }
        )
      );

    vi.stubGlobal("fetch", fetchMock);

    const listener = vi.fn();
    const {
      AUTH_SESSION_EXPIRED_EVENT,
      httpClient,
    } = await import("./http-client");

    window.addEventListener(
      AUTH_SESSION_EXPIRED_EVENT,
      listener
    );

    await expect(
      httpClient.get("/auth/me")
    ).rejects.toMatchObject({
      status: 401,
    });

    expect(listener).toHaveBeenCalledTimes(1);

    window.removeEventListener(
      AUTH_SESSION_EXPIRED_EVENT,
      listener
    );
  });

  it("mostra mensagem adequada quando o servidor está indisponível", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockRejectedValue(
        new TypeError("Failed to fetch")
      )
    );

    const {
      httpClient,
    } = await import("./http-client");

    await expect(
      httpClient.get("/auth/me")
    ).rejects.toMatchObject({
      status: 0,
      message:
        "Não foi possível conectar ao servidor. Verifique se o sistema está disponível e tente novamente.",
    });
  });
});
