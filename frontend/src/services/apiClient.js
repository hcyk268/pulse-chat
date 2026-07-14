import axios from "axios";
import {
  clearAuthSession,
  getAccessToken,
  getRefreshToken,
  isPersistentSession,
  saveAuthSession,
} from "../utils/authStorage";
import { API_BASE_URL } from "./apiConfig";

const AUTH_ERROR_MESSAGES = {
  ACCOUNT_INACTIVE: "Your account is inactive. Please contact support.",
  ACCOUNT_LOCKED: "Your account is locked or suspended. Please contact support.",
};

const httpClient = axios.create({
  baseURL: API_BASE_URL,
  validateStatus: () => true,
});

let authFailureHandler = null;
let authRefreshHandler = null;

export class ApiError extends Error {
  constructor(message, { status = 0, code = null, fieldErrors = [] } = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

export function setAuthEventHandlers({ onAuthFailure = null, onAuthRefresh = null } = {}) {
  authFailureHandler = onAuthFailure;
  authRefreshHandler = onAuthRefresh;
}

function buildErrorMessage(data, path = "") {
  if (AUTH_ERROR_MESSAGES[data?.code]) {
    return AUTH_ERROR_MESSAGES[data.code];
  }

  if (data?.code === "UNAUTHORIZED") {
    if (path.includes("/api/v1/auth/login")) {
      return "Invalid username/email or password.";
    }

    return "Your session has expired. Please sign in again.";
  }

  if (Array.isArray(data?.fieldErrors) && data.fieldErrors.length > 0) {
    return data.fieldErrors.map((fieldError) => fieldError.message).join(" ");
  }

  if (typeof data?.message === "string" && data.message.trim()) {
    return data.message;
  }

  return "Request failed. Please try again.";
}

function notifyAuthFailure(message) {
  clearAuthSession();
  authFailureHandler?.(message);
}

function notifyAuthRefresh(authSession) {
  authRefreshHandler?.(authSession);
}

function normalizeHeaders(headers = {}) {
  if (headers instanceof Headers) {
    return Object.fromEntries(headers.entries());
  }

  return { ...headers };
}

function buildHeaders(options, includeAuth) {
  const headers = normalizeHeaders(options.headers);

  if (!headers.Accept && !headers.accept) {
    headers.Accept = "application/json";
  }

  if (options.body && !headers["Content-Type"] && !headers["content-type"]) {
    headers["Content-Type"] = "application/json";
  }

  if (includeAuth && !headers.Authorization && !headers.authorization) {
    const accessToken = getAccessToken();

    if (accessToken) {
      headers.Authorization = `Bearer ${accessToken}`;
    }
  }

  return headers;
}

async function refreshAuthSession() {
  const refreshToken = getRefreshToken();

  if (!refreshToken) return false;

  try {
    const response = await httpClient.request({
      url: "/api/v1/auth/refresh",
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      data: { refreshToken },
    });

    if (response.status < 200 || response.status >= 300) {
      notifyAuthFailure(buildErrorMessage(response.data, "/api/v1/auth/refresh"));
      return false;
    }

    saveAuthSession(response.data, isPersistentSession());
    notifyAuthRefresh(response.data);
    return true;
  } catch {
    return false;
  }
}

async function sendRequest(path, options, includeAuth) {
  try {
    return await httpClient.request({
      url: path,
      method: options.method || "GET",
      headers: buildHeaders(options, includeAuth),
      data: options.body,
      signal: options.signal,
      params: options.params,
    });
  } catch {
    throw new ApiError(`Cannot connect to backend at ${API_BASE_URL}.`);
  }
}

export async function apiRequest(path, options = {}) {
  const { auth = true, retryOnUnauthorized = true, ...axiosOptions } = options;
  let response = await sendRequest(path, axiosOptions, auth);

  if (auth && retryOnUnauthorized && response.status === 401) {
    const refreshed = await refreshAuthSession();

    if (refreshed) {
      response = await sendRequest(path, axiosOptions, auth);
    }
  }

  if (response.status < 200 || response.status >= 300) {
    const message = buildErrorMessage(response.data, path);

    if (auth && response.status === 401) {
      notifyAuthFailure(message);
    }

    throw new ApiError(message, {
      status: response.status,
      code: response.data?.code ?? null,
      fieldErrors: response.data?.fieldErrors ?? [],
    });
  }

  return response.data === "" || response.data === undefined ? null : response.data;
}
