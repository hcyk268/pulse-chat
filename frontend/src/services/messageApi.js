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

export function pinMessage(messageId) {
  return apiRequest(`/api/v1/messages/${messageId}/pin`, {
    method: "POST",
  });
}

export function unpinMessage(messageId) {
  return apiRequest(`/api/v1/messages/${messageId}/pin`, {
    method: "DELETE",
  });
}

export function getMessageReactions(messageId) {
  return apiRequest(`/api/v1/messages/${messageId}/reactions`);
}

export function reactToMessage(messageId, emoji) {
  return apiRequest(`/api/v1/messages/${messageId}/reactions`, {
    method: "POST",
    body: JSON.stringify({ emoji }),
  });
}

export function removeMessageReaction(messageId, emoji) {
  return apiRequest(`/api/v1/messages/${messageId}/reactions/${encodeURIComponent(emoji)}`, {
    method: "DELETE",
  });
}
