import { dedupeById, getMessageIndex, getMessagePreview } from "./normalizers.js";
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
      messages: hasMessage
        ? nextMessages
        : dedupeById([...nextMessages, message]),
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
      messages: dedupeById([...(conversation.messages ?? []), message]),
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
