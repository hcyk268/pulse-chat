import { Navigate, useLocation } from "react-router-dom";
import { useChatStore } from "../../hooks/useChatStore";

function AuthLoadingScreen() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-[#0e1621] p-6 text-white">
      <div className="flex animate-scale-in items-center gap-3 rounded-xl border border-white/5 bg-[#17212b] px-5 py-4 shadow-panel">
        <span className="relative flex h-3 w-3">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-[#2aabee] opacity-75" />
          <span className="relative inline-flex h-3 w-3 rounded-full bg-[#2aabee]" />
        </span>
        <span className="text-sm font-medium text-slate-200">Checking session...</span>
      </div>
    </main>
  );
}

export function RequireAuth({ children }) {
  const location = useLocation();
  const { authMessage, isAuthenticated, isAuthLoading } = useChatStore();

  if (isAuthLoading) {
    return <AuthLoadingScreen />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location, authMessage }} />;
  }

  return children;
}

export function PublicOnlyRoute({ children }) {
  const location = useLocation();
  const { isAuthenticated, isAuthLoading } = useChatStore();

  if (isAuthLoading) {
    return <AuthLoadingScreen />;
  }

  if (isAuthenticated) {
    const redirectTo = location.state?.from?.pathname || "/chat";
    return <Navigate to={redirectTo} replace />;
  }

  return children;
}
