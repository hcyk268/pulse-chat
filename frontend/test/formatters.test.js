import test from "node:test";
import assert from "node:assert/strict";
import {
  clampPreview,
  formatCount,
  formatCurrency,
  formatDayLabel,
  formatPercent,
  formatPresence,
  formatRelativeTime,
  getDayOffset,
  marketTrend,
  setFormatterLocale,
} from "../src/utils/formatters.js";

const NOW = Date.parse("2026-07-26T12:00:00.000Z");
const labels = { today: "Today", yesterday: "Yesterday" };

test.afterEach(() => setFormatterLocale("en"));

test("describes relative timestamps in plain language", () => {
  assert.equal(formatRelativeTime(new Date(NOW - 5_000).toISOString(), NOW), "now");
  assert.equal(formatRelativeTime(new Date(NOW - 4 * 60_000).toISOString(), NOW), "4 minutes ago");
  assert.equal(formatRelativeTime(new Date(NOW - 3 * 3_600_000).toISOString(), NOW), "3 hours ago");
  assert.equal(formatRelativeTime("", NOW), "");
  assert.equal(formatRelativeTime("not-a-date", NOW), "");
});

test("labels message day separators from caller-supplied words", () => {
  const today = new Date();
  const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000);

  assert.equal(getDayOffset(today.toISOString(), today), 0);
  assert.equal(getDayOffset(yesterday.toISOString(), today), 1);
  assert.equal(getDayOffset("nonsense"), null);

  assert.equal(formatDayLabel(today.toISOString(), labels), "Today");
  assert.equal(formatDayLabel(yesterday.toISOString(), labels), "Yesterday");
  assert.equal(formatDayLabel("", labels), "");
});

test("formats market values and guards missing numbers", () => {
  assert.equal(formatCurrency(null), "--");
  assert.equal(formatCurrency(1234.5, { compact: true }), "$1.23K");
  assert.equal(formatPercent(2.5), "+2.50%");
  assert.equal(formatPercent(-2.5), "-2.50%");
  assert.equal(formatPercent(undefined), "--");
  assert.equal(marketTrend(0), "flat");
  assert.equal(marketTrend("abc"), "flat");
  assert.equal(marketTrend(-1), "down");
});

test("number formatting follows the active locale", () => {
  setFormatterLocale("vi");

  // Vietnamese uses a comma as the decimal separator.
  assert.match(formatPercent(2.5), /^\+2,50%$/);
  assert.match(formatCurrency(1234.5), /1\.234,50/);
});

test("formats social counters compactly", () => {
  assert.equal(formatCount(0), "0");
  assert.equal(formatCount(999), "999");
  assert.equal(formatCount(1240), "1.2K");
  assert.equal(formatCount("oops"), "0");
});

test("summarises presence through the translator and clamps previews", () => {
  const t = (key, vars) => (vars?.time ? `${key}:${vars.time}` : key);

  assert.equal(formatPresence(null, t), "presence.offline");
  assert.equal(formatPresence({ isOnline: true }, t), "presence.online");
  assert.equal(formatPresence({ isOnline: false }, t), "presence.offline");
  assert.match(formatPresence({ isOnline: false, lastActiveAt: NOW }, t), /^presence\.lastActive:/);
  assert.equal(clampPreview("short"), "short");
  assert.equal(clampPreview("abcdef", 4), "abc…");
});
