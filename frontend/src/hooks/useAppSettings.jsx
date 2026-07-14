import {
  createContext,
  useCallback,
  useContext,
  useLayoutEffect,
  useMemo,
  useState,
} from "react";

const STORAGE_KEY = "pulse.app-settings.v1";

export const DEFAULT_APP_SETTINGS = Object.freeze({
  density: "comfortable",
  enterToSend: true,
  reduceMotion: false,
  showMessagePreviews: true,
  showOnlineStatus: true,
});

const AppSettingsContext = createContext(null);

function normalizeSettings(value) {
  const next = {
    ...DEFAULT_APP_SETTINGS,
    ...(value && typeof value === "object" ? value : {}),
  };

  if (!["comfortable", "compact"].includes(next.density)) {
    next.density = DEFAULT_APP_SETTINGS.density;
  }

  for (const key of [
    "enterToSend",
    "reduceMotion",
    "showMessagePreviews",
    "showOnlineStatus",
  ]) {
    next[key] = Boolean(next[key]);
  }

  return next;
}

function readStoredSettings() {
  if (typeof window === "undefined") return { ...DEFAULT_APP_SETTINGS };

  try {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    return normalizeSettings(stored ? JSON.parse(stored) : null);
  } catch {
    return { ...DEFAULT_APP_SETTINGS };
  }
}

export function AppSettingsProvider({ children }) {
  const [settings, setSettings] = useState(readStoredSettings);

  useLayoutEffect(() => {
    const root = document.documentElement;
    root.dataset.density = settings.density;
    root.dataset.reduceMotion = String(settings.reduceMotion);

    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
    } catch {
      // The preferences still work for the current session when storage is unavailable.
    }
  }, [settings]);

  const updateSetting = useCallback((key, value) => {
    setSettings((previous) => normalizeSettings({ ...previous, [key]: value }));
  }, []);

  const resetSettings = useCallback(() => {
    setSettings({ ...DEFAULT_APP_SETTINGS });
  }, []);

  const value = useMemo(
    () => ({ resetSettings, settings, updateSetting }),
    [resetSettings, settings, updateSetting],
  );

  return <AppSettingsContext.Provider value={value}>{children}</AppSettingsContext.Provider>;
}

export function useAppSettings() {
  const value = useContext(AppSettingsContext);

  if (!value) {
    throw new Error("useAppSettings must be used inside AppSettingsProvider.");
  }

  return value;
}
