const DEFAULT_DEVELOPMENT_API_URL = "http://localhost:8080";
const LOCAL_HOSTNAMES = new Set(["localhost", "127.0.0.1", "[::1]"]);

function resolveUrl(value, baseUrl, label) {
  try {
    return new URL(value, baseUrl);
  } catch {
    throw new Error(`${label} must be a valid URL.`);
  }
}

function assertProtocol(url, protocols, label) {
  if (!protocols.includes(url.protocol)) {
    throw new Error(`${label} must use ${protocols.join(" or ")}.`);
  }
}

export function buildRuntimeConfig({
  apiBaseUrl,
  websocketUrl,
  appOrigin,
  isProduction = false,
}) {
  const origin = resolveUrl(appOrigin, "http://localhost", "Application origin");
  assertProtocol(origin, ["http:", "https:"], "Application origin");

  const configuredApiBaseUrl = apiBaseUrl?.trim();
  if (isProduction && !configuredApiBaseUrl) {
    throw new Error("VITE_API_BASE_URL is required for a production build.");
  }

  const absoluteApiUrl = resolveUrl(
    configuredApiBaseUrl || DEFAULT_DEVELOPMENT_API_URL,
    origin,
    "VITE_API_BASE_URL",
  );
  assertProtocol(absoluteApiUrl, ["http:", "https:"], "VITE_API_BASE_URL");

  if (isProduction && LOCAL_HOSTNAMES.has(absoluteApiUrl.hostname)) {
    throw new Error("VITE_API_BASE_URL cannot point to localhost in production.");
  }

  const apiBase = absoluteApiUrl.toString().replace(/\/$/, "");
  const derivedRealtimeUrl = `${apiBase.replace(/^http/i, "ws")}/ws`;
  const absoluteRealtimeUrl = resolveUrl(
    websocketUrl?.trim() || derivedRealtimeUrl,
    origin,
    "VITE_WS_URL",
  );
  assertProtocol(absoluteRealtimeUrl, ["ws:", "wss:"], "VITE_WS_URL");

  if (isProduction && LOCAL_HOSTNAMES.has(absoluteRealtimeUrl.hostname)) {
    throw new Error("VITE_WS_URL cannot point to localhost in production.");
  }

  return Object.freeze({
    apiBaseUrl: apiBase,
    absoluteApiBaseUrl: apiBase,
    realtimeUrl: absoluteRealtimeUrl.toString(),
    realtimeHost: absoluteRealtimeUrl.host,
  });
}
