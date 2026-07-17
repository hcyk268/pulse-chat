import test, { after, beforeEach } from "node:test";
import assert from "node:assert/strict";
import {
  clearAuthSession,
  getAuthSession,
  isPersistentSession,
  saveAuthSession,
} from "../src/utils/authStorage.js";

class MemoryStorage {
  values = new Map();

  getItem(key) {
    return this.values.get(key) ?? null;
  }

  setItem(key, value) {
    this.values.set(key, value);
  }

  removeItem(key) {
    this.values.delete(key);
  }
}

const localStorage = new MemoryStorage();
const sessionStorage = new MemoryStorage();

globalThis.window = {
  localStorage,
  sessionStorage,
};

beforeEach(() => {
  localStorage.values.clear();
  sessionStorage.values.clear();
});

test("persists and reads a remembered session with expiry metadata", () => {
  const response = {
    accessToken: "access",
    refreshToken: "refresh",
    user: { id: 1 },
    accessTokenExpiresInMs: 60_000,
    refreshTokenExpiresInMs: 120_000,
  };

  assert.equal(saveAuthSession(response, true), true);
  assert.equal(isPersistentSession(), true);
  assert.equal(getAuthSession().refreshToken, "refresh");
  assert.ok(getAuthSession().refreshTokenExpiresAt);
  assert.equal(sessionStorage.getItem("chatapp.auth.session"), null);
});

test("keeps session-only auth out of local storage", () => {
  saveAuthSession(
    {
      accessToken: "access",
      refreshToken: "refresh",
      user: { id: 1 },
    },
    false,
  );

  assert.equal(localStorage.getItem("chatapp.auth.session"), null);
  assert.equal(isPersistentSession(), false);
  assert.equal(getAuthSession().accessToken, "access");
});

test("clears both storage locations", () => {
  saveAuthSession(
    {
      accessToken: "access",
      refreshToken: "refresh",
      user: { id: 1 },
    },
    true,
  );
  saveAuthSession(
    {
      accessToken: "session-access",
      refreshToken: "session-refresh",
      user: { id: 2 },
    },
    false,
  );

  clearAuthSession();

  assert.equal(getAuthSession(), null);
});

after(() => {
  delete globalThis.window;
});
