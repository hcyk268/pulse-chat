import assert from "node:assert/strict";
import test from "node:test";
import { getApiErrorMessage } from "../src/api/httpClient.js";
import en from "../src/i18n/locales/en.js";

const t = (key) => en[key] ?? key;

test("maps proxy rate-limit and service-outage statuses to user-facing copy", () => {
  assert.equal(
    getApiErrorMessage({ response: { status: 429, data: { message: "rate.limit.exceeded" } } }, t),
    en["errors.rateLimit"],
  );
  assert.equal(
    getApiErrorMessage({ response: { status: 503, data: null } }, t),
    en["errors.serviceUnavailable"],
  );
});

test("maps timeouts and explicit client-side upload errors", () => {
  assert.equal(
    getApiErrorMessage({ code: "ECONNABORTED" }, t),
    en["errors.timeout"],
  );
  assert.equal(
    getApiErrorMessage(
      { userMessageKey: "errors.uploadEtagMissing" },
      t,
    ),
    en["errors.uploadEtagMissing"],
  );
});
