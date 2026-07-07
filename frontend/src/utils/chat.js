export function isSameId(left, right) {
  return left != null && right != null && String(left) === String(right);
}

export function isSameMessageDay(left, right) {
  if (!left || !right) return false;

  return new Date(left).toDateString() === new Date(right).toDateString();
}

export function getPinnedPreview(message) {
  if (!message) return "Pinned message";
  if (message.deletedAt) return "Message deleted";

  return message.content || "Pinned message";
}

function getConversationSearchText(conversation) {
  const participant = conversation.otherParticipant;

  return [
    participant?.displayName,
    participant?.username,
    participant?.email,
    conversation.lastMessage?.contentPreview,
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
}

export function filterConversations(conversations, query) {
  const normalizedQuery = query.trim().toLowerCase();

  if (!normalizedQuery) {
    return conversations;
  }

  return conversations.filter((conversation) =>
    getConversationSearchText(conversation).includes(normalizedQuery),
  );
}
