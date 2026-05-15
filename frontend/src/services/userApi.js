import { apiRequest } from "./apiClient";

export function getMe() {
  return apiRequest("/api/v1/users/me");
}

export function updateMe({ displayName, avatarUrl = null, bio = null }) {
  return apiRequest("/api/v1/users/me", {
    method: "PATCH",
    body: JSON.stringify({
      displayName,
      avatarUrl,
      bio,
    }),
  });
}

export function searchUsers(query, { limit = 10 } = {}) {
  const params = new URLSearchParams({
    q: query,
    limit: String(limit),
  });

  return apiRequest(`/api/v1/users/search?${params.toString()}`);
}
