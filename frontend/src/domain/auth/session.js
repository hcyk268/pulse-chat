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

export function isAccessTokenUsable(session, now = Date.now(), expirySkewMs = 30_000) {
  if (!session?.accessToken) return false;

  const skewMs = Math.max(0, Number(expirySkewMs) || 0);
  const explicitAccessExpiry = parseTimestamp(session.accessTokenExpiresAt);
  if (explicitAccessExpiry !== null) {
    return explicitAccessExpiry - skewMs > now;
  }

  const savedAt = parseTimestamp(session.savedAt);
  const accessDuration = Number(session.accessTokenExpiresInMs);
  if (savedAt !== null && Number.isFinite(accessDuration) && accessDuration > 0) {
    return savedAt + accessDuration - skewMs > now;
  }

  // Older sessions did not store access-token expiry metadata.
  return true;
}
