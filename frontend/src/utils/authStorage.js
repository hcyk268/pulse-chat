import {
  createStoredAuthSession,
  isAuthSessionUsable,
} from "../domain/auth/session.js";

const AUTH_SESSION_KEY = "chatapp.auth.session";

function getBrowserStorage(name) {
  if (typeof window === "undefined") return null;

  try {
    return window[name];
  } catch {
    return null;
  }
}

function safeGetItem(storage) {
  try {
    return storage?.getItem(AUTH_SESSION_KEY) ?? null;
  } catch {
    return null;
  }
}

function safeSetItem(storage, value) {
  if (!storage) return false;

  try {
    storage.setItem(AUTH_SESSION_KEY, value);
    return true;
  } catch {
    return false;
  }
}

function safeRemoveItem(storage) {
  try {
    storage?.removeItem(AUTH_SESSION_KEY);
  } catch {
    // Storage cleanup is best-effort when browser privacy settings block access.
  }
}

function hasAuthSession(storage) {
  return Boolean(safeGetItem(storage));
}

function readFromStorage(storage) {
  const raw = safeGetItem(storage);
  if (!raw) return null;

  try {
    return JSON.parse(raw);
  } catch {
    safeRemoveItem(storage);
    return null;
  }
}

function readUsableSession(storage) {
  const session = readFromStorage(storage);
  if (!session || isAuthSessionUsable(session)) return session;

  safeRemoveItem(storage);
  return null;
}

export function isPersistentSession() {
  return hasAuthSession(getBrowserStorage("localStorage"));
}

export function saveAuthSession(authResponse, rememberSession = true) {
  const localStorage = getBrowserStorage("localStorage");
  const sessionStorage = getBrowserStorage("sessionStorage");
  const targetStorage = rememberSession ? localStorage : sessionStorage;
  const staleStorage = rememberSession ? sessionStorage : localStorage;
  const serializedSession = JSON.stringify(createStoredAuthSession(authResponse));
  let saved = safeSetItem(targetStorage, serializedSession);

  if (!saved && rememberSession) {
    saved = safeSetItem(sessionStorage, serializedSession);
  }

  if (saved) {
    safeRemoveItem(staleStorage);
  }

  return saved;
}

export function getAuthSession() {
  const persistentSession = readUsableSession(getBrowserStorage("localStorage"));
  if (persistentSession) return persistentSession;

  return readUsableSession(getBrowserStorage("sessionStorage"));
}

export function getAccessToken() {
  return getAuthSession()?.accessToken ?? null;
}

export function getRefreshToken() {
  return getAuthSession()?.refreshToken ?? null;
}

export function hasValidAuthSession() {
  return Boolean(getAuthSession());
}

export function clearAuthSession() {
  safeRemoveItem(getBrowserStorage("localStorage"));
  safeRemoveItem(getBrowserStorage("sessionStorage"));
}
