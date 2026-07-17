import {
  clearAuthSession,
  getAuthSession,
  hasValidAuthSession,
  isPersistentSession,
  saveAuthSession,
} from "../../utils/authStorage";
import { logout as logoutApi } from "../../services/authApi";
import { EMPTY_CURRENT_USER, toCurrentUser } from "./normalizers";

export const SESSION_EXPIRED_MESSAGE = "Your session has expired. Please sign in again.";

export function getStoredAuthSession() {
  if (!hasValidAuthSession()) {
    clearAuthSession();
    return null;
  }

  return getAuthSession();
}

export function createSessionDomain({
  authSession,
  setAuthSession,
  setCurrentUser,
  setAuthStatus,
  setAuthMessage,
  resetChatState,
}) {
  function applyAuthenticatedSession(nextAuthSession, rememberSession = true) {
    saveAuthSession(nextAuthSession, rememberSession);
    setAuthSession(nextAuthSession);
    setCurrentUser(toCurrentUser(nextAuthSession.user));
    setAuthStatus("authenticated");
    setAuthMessage("");
  }

  function clearAuthenticatedSession(message = "") {
    clearAuthSession();
    setAuthSession(null);
    setCurrentUser(EMPTY_CURRENT_USER);
    resetChatState();
    setAuthStatus("unauthenticated");
    setAuthMessage(message);
  }

  function setAuthenticatedUser(user) {
    setCurrentUser(toCurrentUser(user));
  }

  function setAuthenticatedSession(nextAuthSession, rememberSession = true) {
    applyAuthenticatedSession(nextAuthSession, rememberSession);
  }

  function signOut(message = "") {
    const refreshToken = authSession?.refreshToken;

    if (refreshToken) {
      logoutApi(refreshToken).catch(() => {
        // Local sign-out must still complete if the backend session is already gone.
      });
    }

    clearAuthenticatedSession(message);
  }

  return {
    applyAuthenticatedSession,
    clearAuthenticatedSession,
    setAuthenticatedSession,
    setAuthenticatedUser,
    signOut,
    isPersistentSession,
  };
}
