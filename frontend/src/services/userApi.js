import { apiRequest } from "./apiClient";

export function getMe() {
  return apiRequest("/api/v1/users/me");
}

