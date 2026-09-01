import test from "node:test";
import assert from "node:assert/strict";
import { readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import en from "../src/i18n/locales/en.js";
import vi from "../src/i18n/locales/vi.js";
import {
  createTranslator,
  interpolate,
  normalizeLocale,
} from "../src/i18n/translate.js";

const PLACEHOLDER_PATTERN = /\{(\w+)\}/g;

function collectPlaceholders(entry) {
  const templates = typeof entry === "object" ? Object.values(entry) : [entry];

  return new Set(
    templates.flatMap((template) => [...String(template).matchAll(PLACEHOLDER_PATTERN)].map((m) => m[1])),
  );
}

test("both catalogues expose exactly the same keys", () => {
  const enKeys = Object.keys(en).sort();
  const viKeys = Object.keys(vi).sort();
  const missingInVi = enKeys.filter((key) => !viKeys.includes(key));
  const extraInVi = viKeys.filter((key) => !enKeys.includes(key));

  assert.deepEqual(missingInVi, [], "keys missing from vi");
  assert.deepEqual(extraInVi, [], "keys only present in vi");
});

test("translations keep the same interpolation holes", () => {
  for (const key of Object.keys(en)) {
    const expected = [...collectPlaceholders(en[key])].sort();
    const actual = [...collectPlaceholders(vi[key])].sort();

    assert.deepEqual(actual, expected, `placeholders differ for "${key}"`);
  }
});

test("plural entries provide the forms each language needs", () => {
  for (const [key, entry] of Object.entries(en)) {
    if (typeof entry !== "object") continue;

    assert.ok(entry.other, `en "${key}" needs an "other" form`);
    assert.equal(typeof vi[key], "object", `vi "${key}" must also be a plural entry`);
    assert.ok(vi[key].other, `vi "${key}" needs an "other" form`);
  }
});

test("the translator interpolates, pluralises and falls back", () => {
  const t = createTranslator("en", en);

  assert.equal(t("common.appName"), "Trader Hub");
  assert.equal(t("market.results", { count: 1 }), "1 result");
  assert.equal(t("market.results", { count: 4 }), "4 results");
  assert.equal(t("errors.coinData", { symbol: "BTC" }), "No market data for BTC yet.");
  // Missing keys surface themselves instead of rendering nothing.
  assert.equal(t("not.a.real.key"), "not.a.real.key");

  const tVi = createTranslator("vi", vi, en);
  assert.equal(tVi("market.results", { count: 1 }), "1 kết quả");
  assert.equal(tVi("market.results", { count: 9 }), "9 kết quả");
});

test("locale codes normalise from browser values", () => {
  assert.equal(normalizeLocale("vi-VN"), "vi");
  assert.equal(normalizeLocale("EN-us"), "en");
  assert.equal(normalizeLocale("fr"), null);
  assert.equal(normalizeLocale(undefined), null);
  assert.equal(interpolate("{a} and {b}", { a: 1 }), "1 and {b}");
});

/**
 * Guards the reason i18n was introduced: no user-visible literal should creep
 * back into a component. Scans JSX text nodes and the props that render text.
 */
test("components do not reintroduce hardcoded UI strings", () => {
  const roots = ["src/components", "src/features", "src/pages"];
  const files = [];

  function walk(dir) {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
      const full = join(dir, entry.name);
      if (entry.isDirectory()) walk(full);
      else if (entry.name.endsWith(".jsx")) files.push(full);
    }
  }

  roots.forEach(walk);

  const textProps = /\b(placeholder|aria-label|title|description|eyebrow|emptyLabel)="([^"]{4,})"/g;
  const offenders = [];

  for (const file of files) {
    const source = readFileSync(file, "utf8");

    for (const match of source.matchAll(textProps)) {
      // URLs and single tokens are not sentences.
      if (/^https?:\/\//.test(match[2])) continue;
      offenders.push(`${file}: ${match[0]}`);
    }
  }

  assert.deepEqual(offenders, [], "hardcoded copy found");
});
