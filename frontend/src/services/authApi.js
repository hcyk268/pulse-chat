import { apiRequest } from "./apiClient";

export function login({ usernameOrEmail, password }) {
  return apiRequest("/api/v1/auth/login", {
    method: "POST",
    auth: false,
    body: JSON.stringify({
      usernameOrEmail,
      password,
    }),
  });
}

export function register({ username, email, displayName, password, confirmPassword }) {
  return apiRequest("/api/v1/auth/register", {
    method: "POST",
    auth: false,
    body: JSON.stringify({
      username,
      email,
      displayName,
      password,
      confirmPassword,
    }),
  });
}

export function refreshToken(refreshTokenValue) {
  return apiRequest("/api/v1/auth/refresh", {
    method: "POST",
    auth: false,
    body: JSON.stringify({
      refreshToken: refreshTokenValue,
    }),
  });
}
