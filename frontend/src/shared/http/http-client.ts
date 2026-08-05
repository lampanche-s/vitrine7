import {
  appEnv,
} from "../config";

import {
  HttpError,
} from "./http-error";

type HttpRequestOptions = Omit<
  RequestInit,
  "body" | "signal"
> & {
  json?: unknown;
  userActivity?: boolean;
  timeoutMs?: number;
};

const CSRF_COOKIE_NAME = "XSRF-TOKEN";
const CSRF_HEADER_NAME = "X-XSRF-TOKEN";
const USER_ACTIVITY_HEADER_NAME =
  "X-User-Activity";
export const AUTH_SESSION_EXPIRED_EVENT =
  "vitrine7:auth-session-expired";
const SERVER_UNAVAILABLE_MESSAGE =
  "Não foi possível conectar ao servidor. Verifique se o sistema está disponível e tente novamente.";

type CsrfResponse = {
  token?: string;
};

type FieldErrorResponse = {
  field?: string;
  message?: string;
};

function buildUrl(
  path: string
): string {
  const normalizedPath =
    path.startsWith("/")
      ? path
      : `/${path}`;

  return (
    appEnv.apiBaseUrl +
    normalizedPath
  );
}

function isUnsafeMethod(
  method: string | undefined
): boolean {
  const normalizedMethod =
    method?.toUpperCase() ?? "GET";

  return ![
    "GET",
    "HEAD",
    "OPTIONS",
    "TRACE",
  ].includes(normalizedMethod);
}

function readCookie(
  name: string
): string | null {
  const cookie = document.cookie
    .split("; ")
    .find((item) =>
      item.startsWith(`${name}=`)
    );

  if (!cookie) {
    return null;
  }

  return decodeURIComponent(
    cookie.slice(name.length + 1)
  );
}

async function ensureCsrfToken(): Promise<string | null> {
  const response = await fetch(buildUrl("/auth/csrf"), {
    method: "GET",
    credentials: "include",
    headers: {
      Accept: "application/json",
    },
  });

  if (!response.ok) {
    throw new Error(
      "Não foi possível obter o token de segurança."
    );
  }

  const payload =
    (await response.json()) as CsrfResponse;

  return (
    readCookie(CSRF_COOKIE_NAME) ??
    payload.token ??
    null
  );
}

async function parseResponse(
  response: Response
): Promise<unknown> {
  if (response.status === 204) {
    return undefined;
  }

  const contentType =
    response.headers.get(
      "content-type"
    ) ?? "";

  if (
    contentType.includes(
      "application/json"
    )
  ) {
    return response.json();
  }

  const text =
    await response.text();

  return text || undefined;
}

function getErrorMessage(
  payload: unknown,
  fallback: string
): string {
  if (
    typeof payload === "object" &&
    payload !== null &&
    "message" in payload &&
    typeof payload.message === "string"
  ) {
    return payload.message;
  }

  if (
    typeof payload === "string" &&
    payload.trim()
  ) {
    return payload;
  }

  return fallback;
}

function getFieldErrorMessages(payload: unknown): string[] {
  if (
    typeof payload !== "object" ||
    payload === null ||
    !("fieldErrors" in payload) ||
    !Array.isArray(payload.fieldErrors)
  ) {
    return [];
  }

  return payload.fieldErrors
    .map((fieldError: FieldErrorResponse) => {
      const message =
        typeof fieldError.message === "string"
          ? fieldError.message.trim()
          : "";
      const field =
        typeof fieldError.field === "string"
          ? fieldError.field.trim()
          : "";

      if (!message) {
        return "";
      }

      return field
        ? `${field}: ${message}`
        : message;
    })
    .filter((message): message is string =>
      Boolean(message)
    );
}

function getDetailedErrorMessage(
  payload: unknown,
  fallback: string
): string {
  const message =
    getErrorMessage(payload, fallback);
  const fieldMessages =
    getFieldErrorMessages(payload);

  if (fieldMessages.length === 0) {
    return message;
  }

  return `${message} ${fieldMessages.join(" ")}`;
}

async function request<T>(
  path: string,
  options: HttpRequestOptions = {}
): Promise<T> {
  const controller =
    new AbortController();

  const timeoutId =
    window.setTimeout(
      () => controller.abort(),
      options.timeoutMs ??
        appEnv.httpTimeoutMs
    );

  const headers =
    new Headers(options.headers);

  headers.set(
    "Accept",
    "application/json"
  );

  if (options.userActivity !== false) {
    headers.set(
      USER_ACTIVITY_HEADER_NAME,
      "true"
    );
  }

  if (
    options.json !== undefined
  ) {
    headers.set(
      "Content-Type",
      "application/json"
    );
  }

    try {
    if (isUnsafeMethod(options.method)) {
      const csrfToken =
        await ensureCsrfToken();

      if (csrfToken) {
        headers.set(
          CSRF_HEADER_NAME,
          csrfToken
        );
      }
    }

    const response = await fetch(
      buildUrl(path),
      {
        ...options,
        headers,
        credentials: "include",
        signal: controller.signal,

        body:
          options.json === undefined
            ? undefined
            : JSON.stringify(
                options.json
              ),
      }
    );

    const payload =
      await parseResponse(response);

    if (!response.ok) {
      if (response.status === 401) {
        window.dispatchEvent(
          new CustomEvent(
            AUTH_SESSION_EXPIRED_EVENT
          )
        );
      }

      throw new HttpError(
        getDetailedErrorMessage(
          payload,
          `A requisição falhou com status ${response.status}.`
        ),
        response.status,
        payload
      );
    }

    return payload as T;
  } catch (error) {
    if (
      error instanceof HttpError
    ) {
      throw error;
    }

    if (
      error instanceof Error &&
      error.name === "AbortError"
    ) {
      throw new HttpError(
        "A requisição excedeu o tempo limite.",
        408
      );
    }

    if (error instanceof TypeError) {
      throw new HttpError(
        SERVER_UNAVAILABLE_MESSAGE,
        0,
        error
      );
    }

    throw new HttpError(
      error instanceof Error
        ? error.message
        : "Não foi possível acessar o servidor.",
      0,
      error
    );
  } finally {
    window.clearTimeout(
      timeoutId
    );
  }
}

export const httpClient = {
  get<T>(
    path: string,
    options?: HttpRequestOptions
  ): Promise<T> {
    return request<T>(
      path,
      {
        ...options,
        method: "GET",
      }
    );
  },

  post<T>(
    path: string,
    json?: unknown,
    options?: HttpRequestOptions
  ): Promise<T> {
    return request<T>(
      path,
      {
        ...options,
        method: "POST",
        json,
      }
    );
  },

  put<T>(
    path: string,
    json?: unknown,
    options?: HttpRequestOptions
  ): Promise<T> {
    return request<T>(
      path,
      {
        ...options,
        method: "PUT",
        json,
      }
    );
  },

  patch<T>(
    path: string,
    json?: unknown,
    options?: HttpRequestOptions
  ): Promise<T> {
    return request<T>(
      path,
      {
        ...options,
        method: "PATCH",
        json,
      }
    );
  },

  delete<T = void>(
    path: string,
    options?: HttpRequestOptions
  ): Promise<T> {
    return request<T>(
      path,
      {
        ...options,
        method: "DELETE",
      }
    );
  },
};
