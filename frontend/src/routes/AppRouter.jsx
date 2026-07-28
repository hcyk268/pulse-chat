import { Suspense, lazy } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import RequireAuth from "../components/auth/RequireAuth";
import LoginPage from "../pages/LoginPage";
import NotFoundPage from "../pages/NotFoundPage";

const CHUNK_RELOAD_KEY = "chatapp.chunkReload";

function readReloadFlag() {
  try {
    return window.sessionStorage.getItem(CHUNK_RELOAD_KEY);
  } catch {
    return null;
  }
}

function writeReloadFlag(value) {
  try {
    if (value === null) window.sessionStorage.removeItem(CHUNK_RELOAD_KEY);
    else window.sessionStorage.setItem(CHUNK_RELOAD_KEY, value);
  } catch {
    // Without storage we simply fall through to the error boundary.
  }
}

/**
 * A chunk that 404s almost always means a deploy replaced the assets while the
 * tab was open. Reload once to pick up the new build; if that also fails, let
 * the error boundary handle it instead of looping.
 */
function lazyPage(loader) {
  return lazy(() =>
    loader()
      .then((module) => {
        writeReloadFlag(null);
        return module;
      })
      .catch((error) => {
        if (readReloadFlag()) throw error;

        writeReloadFlag("1");
        window.location.reload();
        // Keep the promise pending so nothing renders during the reload.
        return new Promise(() => {});
      }),
  );
}

// Heavier screens (charting, chat workspace) load on demand.
const MarketPage = lazyPage(() => import("../pages/MarketPage"));
const CoinDetailPage = lazyPage(() => import("../pages/CoinDetailPage"));
const WatchlistPage = lazyPage(() => import("../pages/WatchlistPage"));
const CommunityPage = lazyPage(() => import("../pages/CommunityPage"));
const CommunityDetailPage = lazyPage(() => import("../pages/CommunityDetailPage"));
const ChatPage = lazyPage(() => import("../pages/ChatPage"));
const NotificationPage = lazyPage(() => import("../pages/NotificationPage"));
const ProfilePage = lazyPage(() => import("../pages/ProfilePage"));
const AdminOverviewPage = lazyPage(() => import("../pages/admin/AdminOverviewPage"));
const VerifyEmailPage = lazyPage(() =>
  import("../features/auth/AuthActionPages.jsx").then(({ VerifyEmailPage: page }) => ({ default: page })),
);
const ForgotPasswordPage = lazyPage(() =>
  import("../features/auth/AuthActionPages.jsx").then(({ ForgotPasswordPage: page }) => ({ default: page })),
);
const ResetPasswordPage = lazyPage(() =>
  import("../features/auth/AuthActionPages.jsx").then(({ ResetPasswordPage: page }) => ({ default: page })),
);
const ChangePasswordPage = lazyPage(() =>
  import("../features/auth/AuthActionPages.jsx").then(({ ChangePasswordPage: page }) => ({ default: page })),
);
const AdminUsersPage = lazyPage(() => import("../pages/admin/AdminUsersPage"));
const AdminModerationPage = lazyPage(() => import("../pages/admin/AdminModerationPage"));
const AdminCommunitiesPage = lazyPage(() => import("../pages/admin/AdminCommunitiesPage"));
const AdminAuditPage = lazyPage(() => import("../pages/admin/AdminAuditPage"));

function RouteFallback() {
  return (
    <main className="page-shell">
      <div className="market-page-state">Loading...</div>
    </main>
  );
}

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Suspense fallback={<RouteFallback />}>
        <Routes>
          <Route path="/" element={<Navigate to="/market" replace />} />
          <Route path="/home" element={<Navigate to="/market" replace />} />
          <Route path="/market" element={<MarketPage />} />
          <Route path="/coins/:symbol" element={<CoinDetailPage />} />
          <Route path="/community" element={<CommunityPage />} />
          <Route path="/community/:slug" element={<CommunityDetailPage />} />
          <Route
            path="/watchlist"
            element={
              <RequireAuth>
                <WatchlistPage />
              </RequireAuth>
            }
          />
          <Route
            path="/chat"
            element={
              <RequireAuth>
                <ChatPage />
              </RequireAuth>
            }
          />
          <Route
            path="/notifications"
            element={
              <RequireAuth>
                <NotificationPage />
              </RequireAuth>
            }
          />
          <Route
            path="/profile"
            element={
              <RequireAuth>
                <ProfilePage />
              </RequireAuth>
            }
          />
          <Route
            path="/change-password"
            element={
              <RequireAuth>
                <ChangePasswordPage />
              </RequireAuth>
            }
          />
          {[
            { path: "/admin", element: <AdminOverviewPage /> },
            { path: "/admin/users", element: <AdminUsersPage /> },
            { path: "/admin/moderation", element: <AdminModerationPage /> },
            { path: "/admin/communities", element: <AdminCommunitiesPage /> },
            { path: "/admin/audit", element: <AdminAuditPage /> },
          ].map((route) => (
            <Route
              key={route.path}
              path={route.path}
              element={<RequireAuth>{route.element}</RequireAuth>}
            />
          ))}
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<LoginPage />} />
          <Route path="/settings" element={<Navigate to="/profile" replace />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
