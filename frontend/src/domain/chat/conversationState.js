import { getMessageIndex, getMessagePreview } from "./normalizers.js";
import { isSameId } from "../../utils/chat.js";

function toLastMessage(message) {
  return {
    id: message.id,
    senderId: message.senderId,
    contentPreview: getMessagePreview(message),
    status: message.status,
    createdAt: message.createdAt,
    deletedAt: message.deletedAt,
  };
}

export function mergeConversationList(previous, nextItems) {
  const byId = new Map(
    previous.map((conversation) => [String(conversation.id), conversation]),
  );

  nextItems.forEach((conversation) => {
    const existing = byId.get(String(conversation.id));
    byId.set(String(conversation.id), {
      ...existing,
      ...conversation,
      messages: conversation.messages?.length
        ? conversation.messages
        : existing?.messages ?? [],
    });
  });

  return Array.from(byId.values()).sort((left, right) => {
    const leftTimestamp = new Date(
      left.lastMessageAt ?? left.updatedAt ?? left.createdAt,
    ).getTime();
    const rightTimestamp = new Date(
      right.lastMessageAt ?? right.updatedAt ?? right.createdAt,
    ).getTime();

    return rightTimestamp - leftTimestamp;
  });
}

export function updateConversationMessagesInList(
  conversations,
  conversationId,
  updater,
) {
  return conversations.map((conversation) =>
    isSameId(conversation.id, conversationId)
      ? {
          ...conversation,
          messages: updater(conversation.messages ?? []),
        }
      : conversation,
  );
}

/**
 * Inserts a server message, replacing either the same message id or the optimistic
 * entry that carries the same clientMessageId. Keeps sent messages from doubling up
 * when the REST response and the realtime event both arrive.
 */
export function mergeMessageIntoList(messages, message) {
  const index = messages.findIndex(
    (item) =>
      isSameId(item.id, message.id) ||
      (message.clientMessageId != null &&
        isSameId(item.clientMessageId, message.clientMessageId)),
  );

  if (index === -1) {
    return [...messages, message];
  }

  return messages.map((item, itemIndex) => (itemIndex === index ? message : item));
}

export function applyOutgoingMessageToList(conversations, message) {
  return conversations.map((conversation) => {
    if (!isSameId(conversation.id, message.conversationId)) {
      return conversation;
    }

    return {
      ...conversation,
      messages: mergeMessageIntoList(conversation.messages ?? [], message),
      lastMessage: toLastMessage(message),
      lastMessageAt: message.createdAt,
      updatedAt: message.createdAt,
    };
  });
}

/** Prepends an older history page, skipping messages already in the thread. */
export function prependMessageHistory(messages, olderMessages) {
  const knownIds = new Set(
    messages.map((message) => message.id).filter((id) => id != null).map(String),
  );

  return [...olderMessages.filter((message) => !knownIds.has(String(message.id))), ...messages];
}

export function resetConversationUnread(conversations, conversationId) {
  return conversations.map((conversation) =>
    isSameId(conversation.id, conversationId) && conversation.unreadCount
      ? { ...conversation, unreadCount: 0 }
      : conversation,
  );
}

export function applyPresenceToList(conversations, presence) {
  const userId = presence?.userId;
  if (userId == null) return conversations;

  const nextPresence = {
    isOnline: Boolean(presence.isOnline),
    lastActiveAt: presence.lastActiveAt ?? null,
  };

  const withPresence = (contact) =>
    contact && isSameId(contact.id, userId) ? { ...contact, presence: nextPresence } : contact;

  return conversations.map((conversation) => ({
    ...conversation,
    otherParticipant: withPresence(conversation.otherParticipant),
    participants: (conversation.participants ?? []).map(withPresence),
  }));
}

export function applyMessageResponseToList(conversations, message) {
  const lastMessage = toLastMessage(message);

  return conversations.map((conversation) => {
    if (!isSameId(conversation.id, message.conversationId)) {
      return conversation;
    }

    const messages = conversation.messages ?? [];
    const hasMessage = messages.some((item) => isSameId(item.id, message.id));
    const nextMessages = messages.map((item) => {
      if (isSameId(item.id, message.id)) {
        return message;
      }

      if (isSameId(item.replyTo?.id, message.id)) {
        return {
          ...item,
          replyTo: {
            ...item.replyTo,
            content: message.content,
            editedAt: message.editedAt,
            deletedAt: message.deletedAt,
          },
        };
      }

      return item;
    });
    const isLastMessage = isSameId(conversation.lastMessage?.id, message.id);

    return {
      ...conversation,
      messages: hasMessage ? nextMessages : mergeMessageIntoList(nextMessages, message),
      lastMessage: isLastMessage ? lastMessage : conversation.lastMessage,
      lastMessageAt: isLastMessage
        ? message.createdAt
        : conversation.lastMessageAt,
    };
  });
}

export function applyIncomingMessageToList(
  conversations,
  message,
  currentUserId,
) {
  let applied = false;
  const lastMessage = toLastMessage(message);
  const next = conversations.map((conversation) => {
    if (!isSameId(conversation.id, message.conversationId)) {
      return conversation;
    }

    applied = true;

    return {
      ...conversation,
      messages: mergeMessageIntoList(conversation.messages ?? [], message),
      lastMessage,
      lastMessageAt: message.createdAt,
      updatedAt: message.createdAt,
      unreadCount: isSameId(message.senderId, currentUserId)
        ? conversation.unreadCount
        : (conversation.unreadCount ?? 0) + 1,
    };
  });

  return {
    applied,
    conversations: applied ? mergeConversationList(next, []) : conversations,
  };
}

export function applyMessageStatusToList(
  conversations,
  data,
  currentUserId,
) {
  const messageId = data?.messageId;
  if (!messageId) return conversations;

  return conversations.map((conversation) => {
    const messages = conversation.messages ?? [];
    const cutoffIndex = getMessageIndex(messages, messageId);
    const isDeliveredEvent = data.status === "DELIVERED";

    const nextMessages =
      isDeliveredEvent && cutoffIndex !== -1
        ? messages.map((message, index) =>
            index <= cutoffIndex &&
            isSameId(message.senderId, currentUserId) &&
            message.status !== "READ"
              ? {
                  ...message,
                  status: "DELIVERED",
                  deliveredAt: data.deliveredAt ?? message.deliveredAt,
                }
              : message,
          )
        : messages.map((message) =>
            isSameId(message.id, messageId)
              ? {
                  ...message,
                  status: data.status ?? message.status,
                  deliveredAt: data.deliveredAt ?? message.deliveredAt,
                  readAt: data.readAt ?? message.readAt,
                }
              : message,
          );

    return {
      ...conversation,
      lastMessage: isSameId(conversation.lastMessage?.id, messageId)
        ? {
            ...conversation.lastMessage,
            status: data.status ?? conversation.lastMessage.status,
            deliveredAt:
              data.deliveredAt ?? conversation.lastMessage.deliveredAt,
            readAt: data.readAt ?? conversation.lastMessage.readAt,
          }
        : conversation.lastMessage,
      messages: nextMessages,
    };
  });
}

export function applyReadReceiptToList(
  conversations,
  {
    conversationId,
    readerId,
    lastReadMessageId,
    readAt,
    currentUserId,
  },
) {
  if (!conversationId || !readerId || !lastReadMessageId) {
    return conversations;
  }

  return conversations.map((conversation) => {
    if (!isSameId(conversation.id, conversationId)) {
      return conversation;
    }

    const messages = conversation.messages ?? [];
    const cutoffIndex = getMessageIndex(messages, lastReadMessageId);
    const nextMessages =
      cutoffIndex === -1
        ? messages
        : messages.map((message, index) =>
            index <= cutoffIndex && !isSameId(message.senderId, readerId)
              ? {
                  ...message,
                  status: "READ",
                  readAt: message.readAt ?? readAt,
                }
              : message,
          );
    const lastMessage =
      conversation.lastMessage &&
      isSameId(conversation.lastMessage.id, lastReadMessageId)
        ? { ...conversation.lastMessage, status: "READ", readAt }
        : conversation.lastMessage;

    return {
      ...conversation,
      lastMessage,
      messages: nextMessages,
      unreadCount: isSameId(readerId, currentUserId)
        ? 0
        : conversation.unreadCount,
    };
  });
}

export function applyMessagePinToList(conversations, messageId, pin = null) {
  return conversations.map((conversation) => ({
    ...conversation,
    messages: (conversation.messages ?? []).map((message) =>
      isSameId(message.id, messageId)
        ? { ...message, pinned: Boolean(pin), pin }
        : message,
    ),
  }));
}

export function applyMessageReactionsToList(conversations, messageId, reactions) {
  return conversations.map((conversation) => ({
    ...conversation,
    messages: (conversation.messages ?? []).map((message) =>
      isSameId(message.id, messageId)
        ? { ...message, reactions: reactions ?? [] }
        : message,
    ),
  }));
}

export function applyMessageReadReceiptsToList(conversations, messageId, readReceipts) {
  return conversations.map((conversation) => ({
    ...conversation,
    messages: (conversation.messages ?? []).map((message) =>
      isSameId(message.id, messageId)
        ? { ...message, readReceipts: readReceipts ?? [] }
        : message,
    ),
  }));
}