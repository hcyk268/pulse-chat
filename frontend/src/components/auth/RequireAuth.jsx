import { Navigate, useLocation } from "react-router-dom";
import { useAppSelector } from "../../store/hooks";
import { selectIsAuthenticated } from "../../store/slices/authSlice";

export default function RequireAuth({ children }) {
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const location = useLocation();

  if (!isAuthenticated) {
    return (
      <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />
    );
  }

  return children;
}
