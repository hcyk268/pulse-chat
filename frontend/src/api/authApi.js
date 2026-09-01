import httpClient, { unwrap } from "./httpClient.js";

const publicAuthConfig = { skipAuthRefresh: true };

export async function login(request) {
  return unwrap(await httpClient.post("/api/v1/auth/login", request, publicAuthConfig));
}

export async function register(request) {
  return unwrap(await httpClient.post("/api/v1/auth/register", request, publicAuthConfig));
}

export async function verifyEmail(token) {
  return unwrap(
    await httpClient.post("/api/v1/auth/verify-email", { token }, publicAuthConfig),
  );
}

export async function resendVerification(email) {
  return unwrap(
    await httpClient.post("/api/v1/auth/resend-verification", { email }, publicAuthConfig),
  );
}

export async function forgotPassword(email) {
  return unwrap(
    await httpClient.post("/api/v1/auth/forgot-password", { email }, publicAuthConfig),
  );
}

export async function resetPassword(request) {
  return unwrap(
    await httpClient.post("/api/v1/auth/reset-password", request, publicAuthConfig),
  );
}

export async function changePassword(request) {
  return unwrap(await httpClient.post("/api/v1/auth/change-password", request));
}

export async function logout(refreshToken) {
  return unwrap(
    await httpClient.post(
      "/api/v1/auth/logout",
      { refreshToken },
      publicAuthConfig,
    ),
  );
}
