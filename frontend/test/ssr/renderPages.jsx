/**
 * Render smoke test.
 *
 * Unit tests cover pure logic and the build proves the modules compile, but
 * neither actually renders a screen. This bundles the real pages with the real
 * store and router and renders every route in every language. Effects do not
 * run under `renderToStaticMarkup`, so what is asserted is the first paint:
 * loading and empty states, which is exactly where null-data crashes live.
 *
 * Run with `npm run test:render`.
 */
import { renderToStaticMarkup } from "react-dom/server";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { Provider } from "react-redux";
import { configureStore } from "@reduxjs/toolkit";

import adminReducer from "../../src/store/slices/adminSlice.js";
import authReducer from "../../src/store/slices/authSlice.js";
import communityReducer from "../../src/store/slices/communitySlice.js";
import marketReducer from "../../src/store/slices/marketSlice.js";
import notificationsReducer from "../../src/store/slices/notificationsSlice.js";
import uiReducer from "../../src/store/slices/uiSlice.js";
import workspaceReducer from "../../src/store/slices/workspaceSlice.js";

import AiPage from "../../src/pages/AiPage.jsx";
import ChatPage from "../../src/pages/ChatPage.jsx";
import CoinDetailPage from "../../src/pages/CoinDetailPage.jsx";
import CommunityDetailPage from "../../src/pages/CommunityDetailPage.jsx";
import CommunityPage from "../../src/pages/CommunityPage.jsx";
import LoginPage from "../../src/pages/LoginPage.jsx";
import LandingPage from "../../src/pages/LandingPage.jsx";
import {
  ChangePasswordPage,
  ForgotPasswordPage,
  ResetPasswordPage,
  VerifyEmailPage,
} from "../../src/features/auth/AuthActionPages.jsx";
import MarketPage from "../../src/pages/MarketPage.jsx";
import NotFoundPage from "../../src/pages/NotFoundPage.jsx";
import NotificationPage from "../../src/pages/NotificationPage.jsx";
import ProfilePage from "../../src/pages/ProfilePage.jsx";
import WatchlistPage from "../../src/pages/WatchlistPage.jsx";
import AdminAuditPage from "../../src/pages/admin/AdminAuditPage.jsx";
import AdminCommunitiesPage from "../../src/pages/admin/AdminCommunitiesPage.jsx";
import AdminModerationPage from "../../src/pages/admin/AdminModerationPage.jsx";
import AdminOverviewPage from "../../src/pages/admin/AdminOverviewPage.jsx";
import AdminUsersPage from "../../src/pages/admin/AdminUsersPage.jsx";

import { LOCALES, messagesByLocale } from "../../src/i18n/index.js";

// react-router calls useLayoutEffect internally; the warning is inherent to
// server rendering and says nothing about this app.
const originalError = console.error;
console.error = (...args) => {
  if (typeof args[0] === "string" && args[0].includes("useLayoutEffect does nothing")) return;
  originalError(...args);
};

const routes = [
  { name: "landing", path: "/", pattern: "/", element: <LandingPage /> },
  { name: "market", path: "/market", pattern: "/market", element: <MarketPage /> },
  { name: "coin", path: "/coins/btc", pattern: "/coins/:symbol", element: <CoinDetailPage /> },
  { name: "watchlist", path: "/watchlist", pattern: "/watchlist", element: <WatchlistPage /> },
  { name: "community", path: "/community", pattern: "/community", element: <CommunityPage /> },
  {
    name: "community-detail",
    path: "/community/daily-strategy",
    pattern: "/community/:slug",
    element: <CommunityDetailPage />,
  },
  {
    name: "community-missing",
    path: "/community/does-not-exist",
    pattern: "/community/:slug",
    element: <CommunityDetailPage />,
  },
  { name: "chat", path: "/chat", pattern: "/chat", element: <ChatPage /> },
  { name: "ai", path: "/ai", pattern: "/ai", element: <AiPage /> },
  {
    name: "notifications",
    path: "/notifications",
    pattern: "/notifications",
    element: <NotificationPage />,
  },
  { name: "profile", path: "/profile", pattern: "/profile", element: <ProfilePage /> },
  { name: "login", path: "/login", pattern: "/login", element: <LoginPage />, redirectsWhenSignedIn: true },
  { name: "register", path: "/register", pattern: "/register", element: <LoginPage />, redirectsWhenSignedIn: true },
  { name: "verify-email", path: "/verify-email?token=render-token", pattern: "/verify-email", element: <VerifyEmailPage />, redirectsWhenSignedIn: true },
  { name: "forgot-password", path: "/forgot-password", pattern: "/forgot-password", element: <ForgotPasswordPage />, redirectsWhenSignedIn: true },
  { name: "reset-password", path: "/reset-password?token=render-token", pattern: "/reset-password", element: <ResetPasswordPage />, redirectsWhenSignedIn: true },
  { name: "change-password", path: "/change-password", pattern: "/change-password", element: <ChangePasswordPage /> },
  { name: "admin-overview", path: "/admin", pattern: "/admin", element: <AdminOverviewPage /> },
  { name: "admin-users", path: "/admin/users", pattern: "/admin/users", element: <AdminUsersPage /> },
  {
    name: "admin-moderation",
    path: "/admin/moderation",
    pattern: "/admin/moderation",
    element: <AdminModerationPage />,
  },
  {
    name: "admin-communities",
    path: "/admin/communities",
    pattern: "/admin/communities",
    element: <AdminCommunitiesPage />,
  },
  { name: "admin-audit", path: "/admin/audit", pattern: "/admin/audit", element: <AdminAuditPage /> },
  { name: "not-found", path: "/nope", pattern: "*", element: <NotFoundPage /> },
];

const signedInUser = {
  id: 1,
  username: "trader",
  displayName: "Trader One",
  email: "trader@example.com",
  createdAt: "2026-01-01T00:00:00.000Z",
};

function createTestStore({ locale, authenticated }) {
  const store = configureStore({
    reducer: {
      admin: adminReducer,
      auth: authReducer,
      community: communityReducer,
      market: marketReducer,
      notifications: notificationsReducer,
      ui: uiReducer,
      workspace: workspaceReducer,
    },
  });

  store.dispatch({ type: "ui/setLocale", payload: locale });
  if (authenticated) store.dispatch({ type: "auth/signedIn", payload: signedInUser });

  return store;
}

function renderRoute(route, store) {
  return renderToStaticMarkup(
    <Provider store={store}>
      <MemoryRouter initialEntries={[route.path]}>
        <Routes>
          <Route path={route.pattern} element={route.element} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  );
}

/**
 * A missing key renders as the key itself, so any bare dot-path left in the
 * markup means a translation hole that unit tests could not see.
 */
function findUntranslatedKeys(markup, locale) {
  const known = Object.keys(messagesByLocale[locale]);
  const text = markup.replace(/<[^>]*>/g, " ");

  return known.filter((key) => text.includes(key));
}

const failures = [];
let renderCount = 0;

for (const locale of LOCALES) {
  for (const authenticated of [false, true]) {
    for (const route of routes) {
      const label = `${route.name} [${locale}${authenticated ? ", signed in" : ""}]`;

      try {
        const markup = renderRoute(route, createTestStore({ locale, authenticated }));
        renderCount += 1;

        // A signed-in visitor is redirected away from the auth screens, so an
        // empty render is the correct result there.
        if (authenticated && route.redirectsWhenSignedIn) {
          if (markup !== "") failures.push(`${label}: expected a redirect, got markup`);
          continue;
        }

        if (!markup || markup.length < 80) {
          failures.push(`${label}: rendered ${markup.length} chars`);
          continue;
        }

        const leaked = findUntranslatedKeys(markup, locale);
        if (leaked.length > 0) {
          failures.push(`${label}: untranslated key(s) ${leaked.slice(0, 3).join(", ")}`);
        }
      } catch (error) {
        failures.push(`${label}: ${error.message}`);
      }
    }
  }
}

if (failures.length > 0) {
  console.error(`\n${failures.length} render failure(s):`);
  failures.forEach((failure) => console.error(`  ✖ ${failure}`));
  process.exit(1);
}

console.log(`✔ ${renderCount} page renders across ${LOCALES.length} languages, no failures`);
