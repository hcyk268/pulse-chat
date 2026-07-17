import test from "node:test";
import assert from "node:assert/strict";
import { buildRuntimeConfig } from "../src/config/runtimeConfig.js";

test("uses local defaults only in development", () => {
  const config = buildRuntimeConfig({
    appOrigin: "http://localhost:5173",
    isProduction: false,
  });

  assert.equal(config.apiBaseUrl, "http://localhost:8080");
  assert.equal(config.realtimeUrl, "ws://localhost:8080/ws");
});

test("requires an API URL in production", () => {
  assert.throws(
    () =>
      buildRuntimeConfig({
        appOrigin: "https://chat.example.com",
        isProduction: true,
      }),
    /VITE_API_BASE_URL is required/,
  );
});

test("rejects localhost endpoints in production", () => {
  assert.throws(
    () =>
      buildRuntimeConfig({
        apiBaseUrl: "http://localhost:8080",
        appOrigin: "https://chat.example.com",
        isProduction: true,
      }),
    /cannot point to localhost/,
  );
});

test("normalizes production API and realtime endpoints", () => {
  const config = buildRuntimeConfig({
    apiBaseUrl: "https://api.example.com/",
    websocketUrl: "wss://realtime.example.com/socket",
    appOrigin: "https://chat.example.com",
    isProduction: true,
  });

  assert.deepEqual(config, {
    apiBaseUrl: "https://api.example.com",
    absoluteApiBaseUrl: "https://api.example.com",
    realtimeUrl: "wss://realtime.example.com/socket",
    realtimeHost: "realtime.example.com",
  });
});
