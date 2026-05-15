import { apiRequest } from "./apiClient";

export function listMessages({ conversationId, limit = 20, cursor = null }) {
  const params = new URLSearchParams({
    conversationId: String(conversationId),
    limit: String(limit),
  });

  if (cursor) params.set("cursor", cursor);

  return apiRequest(`/api/v1/messages?${params.toString()}`);
}

export function sendMessage({ conversationId, clientMessageId, content, messageType = "TEXT" }) {
  return apiRequest("/api/v1/messages", {
    method: "POST",
    body: JSON.stringify({
      conversationId,
      clientMessageId,
      content,
      messageType,
    }),
  });
}

export function markMessagesRead({ conversationId, lastReadMessageId }) {
  return apiRequest("/api/v1/messages/read", {
    method: "POST",
    body: JSON.stringify({
      conversationId,
      lastReadMessageId,
    }),
  });
}

