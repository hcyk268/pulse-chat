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

export function createGroupConversation({ name, avatarUrl = null, memberIds }) {
  return apiRequest("/api/v1/conversations/group", {
    method: "POST",
    body: JSON.stringify({
      name,
      avatarUrl,
      memberIds,
    }),
  });
}

export function addGroupMembers(conversationId, memberIds) {
  return apiRequest(`/api/v1/conversations/${conversationId}/members`, {
    method: "POST",
    body: JSON.stringify({
      memberIds,
    }),
  });
}

export function acceptGroupInvitation(conversationId) {
  return apiRequest(`/api/v1/conversations/${conversationId}/invitations/accept`, {
    method: "POST",
  });
}

export function rejectGroupInvitation(conversationId) {
  return apiRequest(`/api/v1/conversations/${conversationId}/invitations/reject`, {
    method: "POST",
  });
}

export function removeGroupMember(conversationId, memberId) {
  return apiRequest(`/api/v1/conversations/${conversationId}/members/${memberId}`, {
    method: "DELETE",
  });
}

export function leaveGroup(conversationId) {
  return apiRequest(`/api/v1/conversations/${conversationId}/leave`, {
    method: "POST",
  });
}

export function updateGroupProfile(conversationId, { name, avatarUrl = null }) {
  return apiRequest(`/api/v1/conversations/${conversationId}/group-profile`, {
    method: "PATCH",
    body: JSON.stringify({
      name,
      avatarUrl,
    }),
  });
}

export function updateGroupMemberRole(conversationId, memberId, role) {
  return apiRequest(`/api/v1/conversations/${conversationId}/members/${memberId}/role`, {
    method: "PATCH",
    body: JSON.stringify({
      role,
    }),
  });
}
