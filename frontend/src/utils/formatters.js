/**
 * Number and date formatting follows the active UI locale. The locale is set
 * once from the store instead of threaded through every call site, the way
 * date libraries do it.
 */
let activeLocale = "en";

export function setFormatterLocale(locale) {
  activeLocale = locale || "en";
}

export function getFormatterLocale() {
  return activeLocale;
}

const DAY_MS = 24 * 60 * 60 * 1000;

export function formatShortTime(value) {
  if (!value) return "";

  const date = new Date(value);
  const now = new Date();

  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString(activeLocale, { hour: "2-digit", minute: "2-digit" });
  }

  return date.toLocaleDateString(activeLocale, { month: "short", day: "numeric" });
}

export function formatLongTime(value) {
  if (!value) return "";

  return new Date(value).toLocaleString(activeLocale, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatChatTime(value) {
  if (!value) return "";

  return new Date(value).toLocaleTimeString(activeLocale, {
    hour: "numeric",
    minute: "2-digit",
  });
}

/** 0 = today, 1 = yesterday, otherwise the number of calendar days back. */
export function getDayOffset(value, now = new Date()) {
  if (!value) return null;

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;

  const startOfDay = (input) => new Date(input.getFullYear(), input.getMonth(), input.getDate());

  return Math.round((startOfDay(now) - startOfDay(date)) / DAY_MS);
}

/** Day separators need translated words, so the caller supplies them. */
export function formatDayLabel(value, { today, yesterday } = {}) {
  const offset = getDayOffset(value);
  if (offset === null) return "";
  if (offset === 0 && today) return today;
  if (offset === 1 && yesterday) return yesterday;

  return new Date(value).toLocaleDateString(activeLocale, { month: "short", day: "numeric" });
}

const RELATIVE_UNITS = [
  { limit: 60, divisor: 1, unit: "second" },
  { limit: 3600, divisor: 60, unit: "minute" },
  { limit: 86400, divisor: 3600, unit: "hour" },
  { limit: 604800, divisor: 86400, unit: "day" },
  { limit: 2629800, divisor: 604800, unit: "week" },
  { limit: 31557600, divisor: 2629800, unit: "month" },
];

export function formatRelativeTime(value, now = Date.now()) {
  if (!value) return "";

  const timestamp = new Date(value).getTime();
  if (!Number.isFinite(timestamp)) return "";

  const elapsedSeconds = Math.round((timestamp - now) / 1000);
  const magnitude = Math.abs(elapsedSeconds);
  const formatter = new Intl.RelativeTimeFormat(activeLocale, { numeric: "auto" });

  if (magnitude < 45) return formatter.format(0, "second");

  const match = RELATIVE_UNITS.find((entry) => magnitude < entry.limit) ?? {
    divisor: 31557600,
    unit: "year",
  };

  return formatter.format(Math.round(elapsedSeconds / match.divisor), match.unit);
}

/** Presence copy is translated, so the caller passes its translator in. */
export function formatPresence(presence, t) {
  if (!presence?.isOnline && !presence?.lastActiveAt) return t("presence.offline");
  if (presence.isOnline) return t("presence.online");

  return t("presence.lastActive", { time: formatRelativeTime(presence.lastActiveAt) });
}

export function clampPreview(text = "", max = 74) {
  if (text.length <= max) return text;

  return `${text.slice(0, max - 1).trim()}…`;
}

export function formatCurrency(value, { compact = false, maximumFractionDigits } = {}) {
  if (value === null || value === undefined || value === "") return "--";
  const number = Number(value);
  if (!Number.isFinite(number)) return "--";

  const digits = maximumFractionDigits ?? (Math.abs(number) >= 1 ? 2 : 8);
  return new Intl.NumberFormat(activeLocale, {
    style: "currency",
    currency: "USD",
    notation: compact ? "compact" : "standard",
    maximumFractionDigits: digits,
  }).format(number);
}

export function formatCompactNumber(value) {
  if (value === null || value === undefined || value === "") return "--";
  const number = Number(value);
  if (!Number.isFinite(number)) return "--";

  return new Intl.NumberFormat(activeLocale, {
    notation: "compact",
    maximumFractionDigits: 2,
  }).format(number);
}

export function formatCount(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) return "0";
  if (number < 1000) return new Intl.NumberFormat(activeLocale).format(number);

  return new Intl.NumberFormat(activeLocale, {
    notation: "compact",
    maximumFractionDigits: 1,
  }).format(number);
}

export function formatPercent(value, { signed = true } = {}) {
  if (value === null || value === undefined || value === "") return "--";
  const number = Number(value);
  if (!Number.isFinite(number)) return "--";

  const formatted = new Intl.NumberFormat(activeLocale, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
    signDisplay: signed ? "exceptZero" : "auto",
  }).format(number);

  return `${formatted}%`;
}

export function marketTrend(value) {
  const number = Number(value);
  if (!Number.isFinite(number) || number === 0) return "flat";
  return number > 0 ? "up" : "down";
}
