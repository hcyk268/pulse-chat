import { apiRequest } from "./apiClient";

export function listConversations({ limit = 20, cursor = null, snapshotAt = null } = {}) {
  const params = new URLSearchParams({
    limit: String(limit),
  });

  if (cursor) params.set("cursor", cursor);
  if (snapshotAt) params.set("snapshotAt", snapshotAt);

  return apiRequest(`/api/v1/conversations?${params.toString()}`);
}

export function getConversation(conversationId) {
  return apiRequest(`/api/v1/conversations/${conversationId}`);
}

export function listConversationPins(conversationId) {
  return apiRequest(`/api/v1/conversations/${conversationId}/pins`);
}

export function createDirectConversation(targetUserId) {
  return apiRequest("/api/v1/conversations/direct", {
    method: "POST",
    body: JSON.stringify({
      targetUserId,
    }),
  });
}
