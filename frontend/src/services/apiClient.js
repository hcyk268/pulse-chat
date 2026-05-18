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

async function parseResponseBody(response) {
  const text = await response.text();
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
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

function buildHeaders(options, includeAuth) {
  const headers = new Headers(options.headers);

  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }

  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (includeAuth && !headers.has("Authorization")) {
    const accessToken = getAccessToken();

    if (accessToken) {
      headers.set("Authorization", `Bearer ${accessToken}`);
    }
  }

  return headers;
}

async function refreshAuthSession() {
  const refreshToken = getRefreshToken();

  if (!refreshToken) return false;

  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ refreshToken }),
    });

    const data = await parseResponseBody(response);

    if (!response.ok) {
      notifyAuthFailure(buildErrorMessage(data, "/api/v1/auth/refresh"));
      return false;
    }

    saveAuthSession(data, isPersistentSession());
    notifyAuthRefresh(data);
    return true;
  } catch {
    return false;
  }
}

async function sendRequest(path, options, includeAuth) {
  let response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: buildHeaders(options, includeAuth),
    });
  } catch {
    throw new ApiError(`Cannot connect to backend at ${API_BASE_URL}.`);
  }

  return response;
}

export async function apiRequest(path, options = {}) {
  const { auth = true, retryOnUnauthorized = true, ...fetchOptions } = options;
  let response = await sendRequest(path, fetchOptions, auth);

  if (auth && retryOnUnauthorized && response.status === 401) {
    const refreshed = await refreshAuthSession();

    if (refreshed) {
      response = await sendRequest(path, fetchOptions, auth);
    }
  }

  const data = await parseResponseBody(response);

  if (!response.ok) {
    const message = buildErrorMessage(data, path);

    if (auth && response.status === 401) {
      notifyAuthFailure(message);
    }

    throw new ApiError(message, {
      status: response.status,
      code: data?.code ?? null,
      fieldErrors: data?.fieldErrors ?? [],
    });
  }

  return data;
}
