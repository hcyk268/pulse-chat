import test from "node:test";
import assert from "node:assert/strict";
import {
  createStoredAuthSession,
  isAuthSessionUsable,
} from "../src/domain/auth/session.js";

const baseSession = {
  accessToken: "access",
  refreshToken: "refresh",
  user: { id: 1 },
  accessTokenExpiresInMs: 60_000,
  refreshTokenExpiresInMs: 120_000,
};

test("stores absolute token expiry timestamps", () => {
  const session = createStoredAuthSession(
    baseSession,
    new Date("2026-01-01T00:00:00.000Z"),
  );

  assert.equal(session.accessTokenExpiresAt, "2026-01-01T00:01:00.000Z");
  assert.equal(session.refreshTokenExpiresAt, "2026-01-01T00:02:00.000Z");
});

test("rejects incomplete or expired sessions", () => {
  assert.equal(isAuthSessionUsable({ accessToken: "only-access" }), false);

  const session = createStoredAuthSession(
    baseSession,
    new Date("2026-01-01T00:00:00.000Z"),
  );

  assert.equal(isAuthSessionUsable(session, Date.parse("2026-01-01T00:01:59.000Z")), true);
  assert.equal(isAuthSessionUsable(session, Date.parse("2026-01-01T00:02:00.000Z")), false);
});

test("keeps backward compatibility for sessions without expiry metadata", () => {
  assert.equal(
    isAuthSessionUsable({
      accessToken: "access",
      refreshToken: "refresh",
      user: { id: 1 },
    }),
    true,
  );
});
