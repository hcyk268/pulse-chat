import { useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { logout } from "../../api/authApi.js";
import { resetRealtimeConnection } from "../../services/realtimeClient.js";
import { useAppDispatch } from "../../store/hooks";
import { signedOut } from "../../store/slices/authSlice";
import { clearFavorites } from "../../store/slices/marketSlice";
import { clearActiveConversation } from "../../store/slices/workspaceSlice";
import { clearAuthSession, getRefreshToken } from "../../utils/authStorage.js";

export function useSignOut() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  return useCallback(async () => {
    const refreshToken = getRefreshToken();

    if (refreshToken) {
      try {
        await logout(refreshToken);
      } catch {
        // Revoking server-side is best effort; the local session is cleared either way.
      }
    }

    clearAuthSession();
    dispatch(signedOut());
    dispatch(clearFavorites());
    dispatch(clearActiveConversation());
    resetRealtimeConnection();
    navigate("/login", { replace: true });
  }, [dispatch, navigate]);
}

export default useSignOut;
