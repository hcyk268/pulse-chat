import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { PublicOnlyRoute, RequireAuth } from "../components/auth/AuthRoutes";
import ChatPage from "../pages/ChatPage";
import LoginPage from "../pages/LoginPage";
import NotFoundPage from "../pages/NotFoundPage";
import RegisterPage from "../pages/RegisterPage";

const publicRoutes = [
  { path: "/login", element: <LoginPage /> },
  { path: "/register", element: <RegisterPage /> },
];

const protectedRoutes = [
  { path: "/chat", element: <ChatPage /> },
  { path: "/chat/:conversationId", element: <ChatPage /> },
  { path: "/profile", element: <Navigate to="/chat" replace /> },
];

export default function AppRouter() {
  return (
    <BrowserRouter>
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
    </BrowserRouter>
  );
}
