import { createTranslator, normalizeLocale, DEFAULT_LOCALE, LOCALES } from "./translate.js";
import en from "./locales/en.js";
import vi from "./locales/vi.js";
import communityManagementMessages from "./communityManagementMessages.js";

export { DEFAULT_LOCALE, LOCALES };

export const LOCALE_STORAGE_KEY = "chatapp.locale";

export const messagesByLocale = {
  en: { ...en, ...communityManagementMessages.en },
  vi: { ...vi, ...communityManagementMessages.vi },
};

const translatorCache = new Map();

export function getTranslator(locale) {
  const resolved = LOCALES.includes(locale) ? locale : DEFAULT_LOCALE;

  if (!translatorCache.has(resolved)) {
    translatorCache.set(
      resolved,
      createTranslator(resolved, messagesByLocale[resolved], messagesByLocale.en),
    );
  }

  return translatorCache.get(resolved);
}

function getStorage() {
  if (typeof window === "undefined") return null;

  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

/** Returns the explicit user choice, or null when the browser should decide. */
export function readStoredLocale() {
  try {
    return normalizeLocale(getStorage()?.getItem(LOCALE_STORAGE_KEY));
  } catch {
    return null;
  }
}

export function storeLocale(locale) {
  try {
    getStorage()?.setItem(LOCALE_STORAGE_KEY, locale);
  } catch {
    // Persisting the preference is best effort under strict privacy settings.
  }
}

export function getBrowserLocale() {
  if (typeof navigator === "undefined") return DEFAULT_LOCALE;

  const candidates = navigator.languages?.length ? navigator.languages : [navigator.language];

  for (const candidate of candidates) {
    const locale = normalizeLocale(candidate);
    if (locale) return locale;
  }

  return DEFAULT_LOCALE;
}

export function resolveInitialLocale() {
  return readStoredLocale() ?? getBrowserLocale();
}

export function applyLocale(locale) {
  if (typeof document === "undefined") return;
  document.documentElement.lang = locale;
}
