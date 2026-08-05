function getHttpTimeout(): number {
  const rawValue =
    import.meta.env
      .VITE_HTTP_TIMEOUT_MS;

  if (!rawValue) {
    return 15_000;
  }

  const timeout = Number(rawValue);

  if (
    !Number.isFinite(timeout) ||
    timeout <= 0
  ) {
    throw new Error(
      "VITE_HTTP_TIMEOUT_MS deve ser um número positivo."
    );
  }

  return timeout;
}

function normalizeBaseUrl(
  value: string
): string {
  const normalized =
    value.trim().replace(/\/+$/, "");

  return normalized || "/api/v1";
}

export const appEnv = Object.freeze({
  apiBaseUrl: normalizeBaseUrl(
    import.meta.env
      .VITE_API_BASE_URL ??
      "/api/v1"
  ),

  httpTimeoutMs:
    getHttpTimeout(),
});
