import { useCallback } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAppSelector } from "../../store/hooks.js";
import { selectIsAuthenticated } from "../../store/slices/authSlice.js";

export function useAuthGate() {
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const location = useLocation();
  const navigate = useNavigate();

  const openAuth = useCallback((request = {}, action = null) => {
    if (isAuthenticated) {
      void Promise.resolve().then(() => action?.()).catch(() => {});
      return;
    }

    const from = `${location.pathname}${location.search}${location.hash}`;
    navigate(request.mode === "register" ? "/register" : "/login", {
      state: {
        from,
        authIntent: {
          kind: request.kind ?? "generic",
          payload: request.payload ?? null,
        },
      },
    });
  }, [isAuthenticated, location.hash, location.pathname, location.search, navigate]);

  return { openAuth };
}