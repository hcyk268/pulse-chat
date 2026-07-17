function getExpiresAt(savedAtMs, durationMs) {
  const duration = Number(durationMs);
  if (!Number.isFinite(duration) || duration <= 0) return null;

  return new Date(savedAtMs + duration).toISOString();
}

function parseTimestamp(value) {
  if (!value) return null;

  const timestamp = Date.parse(value);
  return Number.isFinite(timestamp) ? timestamp : null;
}

export function createStoredAuthSession(authResponse, savedAt = new Date()) {
  const savedAtMs = savedAt.getTime();
  const savedAtIso = savedAt.toISOString();

  return {
    ...authResponse,
    savedAt: savedAtIso,
    accessTokenExpiresAt:
      authResponse.accessTokenExpiresAt ??
      getExpiresAt(savedAtMs, authResponse.accessTokenExpiresInMs),
    refreshTokenExpiresAt:
      authResponse.refreshTokenExpiresAt ??
      getExpiresAt(savedAtMs, authResponse.refreshTokenExpiresInMs),
  };
}

export function isAuthSessionUsable(session, now = Date.now()) {
  if (!session?.accessToken || !session?.refreshToken || !session?.user) {
    return false;
  }

  const explicitRefreshExpiry = parseTimestamp(session.refreshTokenExpiresAt);
  if (explicitRefreshExpiry !== null) {
    return explicitRefreshExpiry > now;
  }

  const savedAt = parseTimestamp(session.savedAt);
  const refreshDuration = Number(session.refreshTokenExpiresInMs);
  if (savedAt !== null && Number.isFinite(refreshDuration) && refreshDuration > 0) {
    return savedAt + refreshDuration > now;
  }

  // Older sessions did not store expiry metadata; the backend verification remains authoritative.
  return true;
}
