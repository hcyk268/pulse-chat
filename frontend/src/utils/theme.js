export const THEME_STORAGE_KEY = "chatapp.theme";
export const THEMES = ["light", "dark"];

const THEME_COLORS = { light: "#f8fafc", dark: "#0a0f1a" };

function getStorage() {
  if (typeof window === "undefined") return null;

  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

/** Returns the explicit user choice, or null when the OS should decide. */
export function readStoredTheme() {
  try {
    const value = getStorage()?.getItem(THEME_STORAGE_KEY);
    return THEMES.includes(value) ? value : null;
  } catch {
    return null;
  }
}

export function storeTheme(theme) {
  try {
    getStorage()?.setItem(THEME_STORAGE_KEY, theme);
  } catch {
    // Persisting the preference is best effort under strict privacy settings.
  }
}

export function getSystemTheme() {
  if (typeof window === "undefined" || !window.matchMedia) return "light";

  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

export function resolveInitialTheme() {
  return readStoredTheme() ?? getSystemTheme();
}

export function applyTheme(theme) {
  if (typeof document === "undefined") return;

  document.documentElement.dataset.theme = theme;
  document
    .querySelector('meta[name="theme-color"]')
    ?.setAttribute("content", THEME_COLORS[theme] ?? THEME_COLORS.light);
}
