import axios from "axios";
import { runtimeConfig } from "../config/runtimeConfig.js";
import {
  clearAuthSession,
  getAccessToken,
  getAuthSession,
  isPersistentSession,
  saveAuthSession,
} from "../utils/authStorage.js";

export const UNAUTHORIZED_EVENT = "chatapp:unauthorized";

/**
 * The backend resolves its message bundle from Accept-Language and already
 * ships an English and a Vietnamese catalogue, so server-side copy follows the
 * UI language once we send the header.
 */
let apiLocale = "en";

export function setApiLocale(locale) {
  apiLocale = locale || "en";
}

const defaultHeaders = { "Content-Type": "application/json" };
const REQUEST_TIMEOUT_MS = 15000;
const REFRESH_TIMEOUT_MS = 10000;

export const httpClient = axios.create({
  baseURL: runtimeConfig.absoluteApiBaseUrl,
  headers: defaultHeaders,
  timeout: REQUEST_TIMEOUT_MS,
});

// A dedicated instance keeps the refresh call out of the retry interceptor below.
const refreshClient = axios.create({
  baseURL: runtimeConfig.absoluteApiBaseUrl,
  headers: defaultHeaders,
  timeout: REFRESH_TIMEOUT_MS,
});

let pendingRefresh = null;

function notifySessionExpired() {
  clearAuthSession();

  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
  }
}

export function refreshAuthSession() {
  const refreshToken = getAuthSession()?.refreshToken;
  if (!refreshToken) return Promise.resolve(null);

  pendingRefresh =
    pendingRefresh ??
    refreshClient
      .post(
        "/api/v1/auth/refresh",
        { refreshToken },
        { headers: { "Accept-Language": apiLocale } },
      )
      .then((response) => {
        const session = response.data?.data ?? null;
        if (!session?.accessToken) return null;

        saveAuthSession(session, isPersistentSession());
        return session;
      })
      .finally(() => {
        pendingRefresh = null;
      });

  return pendingRefresh;
}

httpClient.interceptors.request.use((config) => {
  const accessToken = getAccessToken();
  config.headers = config.headers ?? {};
  config.headers["Accept-Language"] = apiLocale;

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config ?? {};
    const isUnauthorized = error.response?.status === 401;
    const canRecover = isUnauthorized && !config.skipAuthRefresh && !config.hasRetriedAuth;

    if (!canRecover || !getAuthSession()?.refreshToken) {
      // Only signal an expired session when one actually existed.
      if (isUnauthorized && !config.skipAuthRefresh && getAuthSession()) {
        notifySessionExpired();
      }

      return Promise.reject(error);
    }

    try {
      const session = await refreshAuthSession();
      if (!session?.accessToken) {
        notifySessionExpired();
        return Promise.reject(error);
      }

      return httpClient({
        ...config,
        hasRetriedAuth: true,
        headers: { ...config.headers, Authorization: `Bearer ${session.accessToken}` },
      });
    } catch (refreshError) {
      const refreshStatus = refreshError?.response?.status;
      const refreshTokenRejected = [400, 401, 403].includes(refreshStatus);

      if (refreshTokenRejected) {
        notifySessionExpired();
      }

      // Transient refresh failures must not destroy a still-valid refresh token.
      return Promise.reject(refreshError);
    }
  },
);

export function unwrap(response) {
  return response.data?.data ?? null;
}

export function getApiErrorCode(error) {
  return error?.response?.data?.code ?? null;
}

function resolveServerMessage(message, t) {
  if (!message) return null;

  const translated = t(message);
  if (translated !== message) return translated;

  // Do not leak internal message-bundle keys to the UI.
  return /^[a-z][a-z\d]*(?:[._-][a-z\d]+)+$/i.test(message) ? null : message;
}

/**
 * Prefer translated field/server messages and always fall back to UI copy
 * instead of leaking an Axios message or an internal message key.
 */
export function getApiErrorMessage(error, t, fallbackKey = "errors.generic") {
  if (error?.userMessageKey) return t(error.userMessageKey);

  const payload = error?.response?.data;
  const serverMessage = resolveServerMessage(
    payload?.fieldErrors?.[0]?.message ?? payload?.message,
    t,
  );

  if (serverMessage) return serverMessage;
  if (error?.code === "ECONNABORTED") return t("errors.timeout");
  if (error?.code === "ERR_NETWORK") return t("errors.network");
  if (error?.response?.status === 429) return t("errors.rateLimit");
  if (error?.response?.status === 503) return t("errors.serviceUnavailable");
  if (error?.response?.status === 401) return t("errors.session");

  return t(fallbackKey);
}

export default httpClient;
