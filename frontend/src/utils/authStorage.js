const AUTH_SESSION_KEY = "chatapp.auth.session";

function hasAuthSession(storage) {
  return Boolean(storage.getItem(AUTH_SESSION_KEY));
}

function readFromStorage(storage) {
  const raw = storage.getItem(AUTH_SESSION_KEY);
  if (!raw) return null;

  try {
    return JSON.parse(raw);
  } catch {
    storage.removeItem(AUTH_SESSION_KEY);
    return null;
  }
}

export function isPersistentSession() {
  if (typeof window === "undefined") return true;

  return hasAuthSession(window.localStorage);
}

export function saveAuthSession(authResponse, rememberSession = true) {
  if (typeof window === "undefined") return;

  const targetStorage = rememberSession ? window.localStorage : window.sessionStorage;
  const staleStorage = rememberSession ? window.sessionStorage : window.localStorage;

  targetStorage.setItem(
    AUTH_SESSION_KEY,
    JSON.stringify({
      ...authResponse,
      savedAt: new Date().toISOString(),
    }),
  );
  staleStorage.removeItem(AUTH_SESSION_KEY);
}

export function getAuthSession() {
  if (typeof window === "undefined") return null;

  return readFromStorage(window.localStorage) ?? readFromStorage(window.sessionStorage);
}

export function getAccessToken() {
  return getAuthSession()?.accessToken ?? null;
}

export function getRefreshToken() {
  return getAuthSession()?.refreshToken ?? null;
}

export function hasValidAuthSession() {
  const session = getAuthSession();

  return Boolean(session?.accessToken && session?.refreshToken && session?.user);
}

export function clearAuthSession() {
  if (typeof window === "undefined") return;

  window.localStorage.removeItem(AUTH_SESSION_KEY);
  window.sessionStorage.removeItem(AUTH_SESSION_KEY);
}
