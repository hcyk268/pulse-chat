/**
 * A deliberately small translator: flat dot-separated keys, `{name}` holes, and
 * plural forms driven by Intl.PluralRules rather than a hand-written rule per
 * language. The signature matches i18next's `t(key, vars)` so swapping in a
 * library later would not touch call sites.
 */
export const LOCALES = ["en", "vi"];
export const DEFAULT_LOCALE = "en";

const INTERPOLATION_PATTERN = /\{(\w+)\}/g;

export function interpolate(template, vars) {
  if (!vars) return template;

  return template.replace(INTERPOLATION_PATTERN, (match, name) =>
    Object.prototype.hasOwnProperty.call(vars, name) ? String(vars[name]) : match,
  );
}

function selectPluralForm(entry, pluralRules, vars) {
  const count = Number(vars?.count);
  const category = Number.isFinite(count) ? pluralRules.select(count) : "other";

  return entry[category] ?? entry.other ?? entry.one ?? "";
}

export function createTranslator(locale, messages, fallbackMessages = {}) {
  const pluralRules = new Intl.PluralRules(locale);

  return function t(key, vars) {
    const entry = messages[key] ?? fallbackMessages[key];

    // Surfacing the key beats rendering an empty string when copy is missing.
    if (entry == null) return key;

    const template =
      typeof entry === "object" ? selectPluralForm(entry, pluralRules, vars) : entry;

    return interpolate(template, vars);
  };
}

export function normalizeLocale(value) {
  if (typeof value !== "string") return null;

  const base = value.toLowerCase().split("-")[0];
  return LOCALES.includes(base) ? base : null;
}
