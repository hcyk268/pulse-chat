import { lazy, Suspense } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { PublicOnlyRoute, RequireAuth } from "../components/auth/AuthRoutes";

const ChatPage = lazy(() => import("../pages/ChatPage"));
const LoginPage = lazy(() => import("../pages/LoginPage"));
const NotFoundPage = lazy(() => import("../pages/NotFoundPage"));
const RegisterPage = lazy(() => import("../pages/RegisterPage"));
const SettingsPage = lazy(() => import("../pages/SettingsPage"));

const publicRoutes = [
  { path: "/login", element: <LoginPage /> },
  { path: "/register", element: <RegisterPage /> },
];

const protectedRoutes = [
  { path: "/chat", element: <ChatPage /> },
  { path: "/chat/:conversationId", element: <ChatPage /> },
  { path: "/settings", element: <SettingsPage /> },
  { path: "/profile", element: <Navigate to="/chat" replace /> },
];

function RouteLoadingScreen() {
  return (
    <main
      aria-busy="true"
      aria-label="Loading page"
      className="flex min-h-screen items-center justify-center bg-[#0a0f1a] text-slate-300"
    >
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-slate-700 border-t-cyan-300" />
    </main>
  );
}

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Suspense fallback={<RouteLoadingScreen />}>
        <Routes>
          <Route path="/" element={<Navigate to="/chat" replace />} />
          {publicRoutes.map((route) => (
            <Route
              key={route.path}
              path={route.path}
              element={<PublicOnlyRoute>{route.element}</PublicOnlyRoute>}
            />
          ))}
          {protectedRoutes.map((route) => (
            <Route
              key={route.path}
              path={route.path}
              element={<RequireAuth>{route.element}</RequireAuth>}
            />
          ))}
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
