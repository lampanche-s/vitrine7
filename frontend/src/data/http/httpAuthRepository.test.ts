import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";

import {
  HttpError,
} from "../../shared/http/http-error";

const mocks = vi.hoisted(() => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

vi.mock("../../shared/http", async () => {
  const actual = await vi.importActual<
    typeof import("../../shared/http")
  >("../../shared/http");

  return {
    ...actual,
    httpClient: mocks.httpClient,
  };
});

describe("httpAuthRepository login errors", () => {
  beforeEach(() => {
    mocks.httpClient.get.mockReset();
    mocks.httpClient.post.mockReset();
  });

  it("mapeia usuario bloqueado pelo codigo estavel da API", async () => {
    mocks.httpClient.post.mockRejectedValue(
      new HttpError(
        "Este usuário está bloqueado.",
        403,
        {
          code: "ACCOUNT_BLOCKED",
          message: "Este usuário está bloqueado.",
        }
      )
    );

    const {
      ACCOUNT_BLOCKED_LOGIN_MESSAGE,
      httpAuthRepository,
    } = await import("./httpAuthRepository");

    await expect(
      httpAuthRepository.login({
        username: "operador",
        password: "senha-segura",
      })
    ).rejects.toThrow(ACCOUNT_BLOCKED_LOGIN_MESSAGE);
    expect(mocks.httpClient.get).not.toHaveBeenCalled();
  });

  it("preserva a mensagem atual para credenciais invalidas", async () => {
    mocks.httpClient.post.mockRejectedValue(
      new HttpError(
        "Usuário ou senha inválidos.",
        401,
        {
          code: "INVALID_CREDENTIALS",
          message: "Usuário ou senha inválidos.",
        }
      )
    );

    const {
      httpAuthRepository,
    } = await import("./httpAuthRepository");

    await expect(
      httpAuthRepository.login({
        username: "operador",
        password: "senha-errada",
      })
    ).rejects.toMatchObject({
      message: "Usuário ou senha inválidos.",
      status: 401,
    });
    expect(mocks.httpClient.get).not.toHaveBeenCalled();
  });
});
