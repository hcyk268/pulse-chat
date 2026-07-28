import { useEffect } from "react";
import { UNAUTHORIZED_EVENT, setApiLocale } from "./api/httpClient";
import ErrorBoundary from "./components/system/ErrorBoundary";
import AppRouter from "./routes/AppRouter";
import { resetRealtimeConnection } from "./services/realtimeClient";
import { useAppDispatch, useAppSelector } from "./store/hooks";
import { signedOut } from "./store/slices/authSlice";
import { clearFavorites } from "./store/slices/marketSlice";
import { selectLocale, selectTheme, setTheme } from "./store/slices/uiSlice";
import { clearActiveConversation } from "./store/slices/workspaceSlice";
import { applyLocale } from "./i18n/index.js";
import { setFormatterLocale } from "./utils/formatters.js";
import { applyTheme, readStoredTheme } from "./utils/theme.js";

export default function App() {
  const dispatch = useAppDispatch();
  const theme = useAppSelector(selectTheme);
  const locale = useAppSelector(selectLocale);

  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  // Numbers, dates and server messages all follow the UI language together.
  useEffect(() => {
    setFormatterLocale(locale);
    setApiLocale(locale);
    applyLocale(locale);
  }, [locale]);

  // Follow the OS while the user has not made an explicit choice.
  useEffect(() => {
    if (typeof window === "undefined" || !window.matchMedia) return undefined;

    const media = window.matchMedia("(prefers-color-scheme: dark)");
    const handleChange = (event) => {
      if (readStoredTheme()) return;
      dispatch(setTheme(event.matches ? "dark" : "light"));
    };

    media.addEventListener("change", handleChange);
    return () => media.removeEventListener("change", handleChange);
  }, [dispatch]);

  // The HTTP layer clears storage when a refresh attempt fails; mirror that in the store.
  useEffect(() => {
    function handleSessionExpired() {
      dispatch(signedOut({ expired: true }));
      dispatch(clearFavorites());
      dispatch(clearActiveConversation());
      resetRealtimeConnection();
    }

    window.addEventListener(UNAUTHORIZED_EVENT, handleSessionExpired);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, handleSessionExpired);
  }, [dispatch]);

  return (
    <ErrorBoundary>
      <AppRouter />
    </ErrorBoundary>
  );
}
