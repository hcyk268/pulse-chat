import { isSameId } from "../../utils/chat";
import {
  applyIncomingMessageToList,
  applyMessageStatusToList,
  applyReadReceiptToList,
  mergeConversationList,
} from "./conversationState";
import {
  dedupeById,
  getConversationContacts,
  mergeContacts,
  normalizeConversation,
  normalizeMessage,
} from "./normalizers";

export function createRealtimeDomain({
  currentUserRef,
  realtimeClientRef,
  activeConversationIdRef,
  deliveredMessageIdsRef,
  pendingMessagesByConversationRef,
  setConversations,
  setContacts,
  setConversationTyping,
  setMessagePinned,
  setPinnedMessageDetail,
  removePinnedMessageDetail,
  applyMessageResponseToConversation,
  markConversationRead,
}) {
  function storePendingRealtimeMessage(message) {
    const key = String(message.conversationId);
    const current = pendingMessagesByConversationRef.current.get(key) ?? [];

    pendingMessagesByConversationRef.current.set(key, dedupeById([...current, message]));
  }

  function takePendingRealtimeMessages(conversationId) {
    const key = String(conversationId);
    const messages = pendingMessagesByConversationRef.current.get(key) ?? [];

    pendingMessagesByConversationRef.current.delete(key);
    return messages;
  }

  function sendMessageDelivered(message) {
    const messageId = message?.id;
    if (!messageId || isSameId(message.senderId, currentUserRef.current.id)) {
      return;
    }

    if (message.status === "DELIVERED" || message.status === "READ") {
      return;
    }

    const key = String(messageId);
    if (deliveredMessageIdsRef.current.has(key)) {
      return;
    }

    if (realtimeClientRef.current?.sendMessageDelivered(messageId)) {
      deliveredMessageIdsRef.current.add(key);
    }
  }

  function applyRealtimeMessage(messageResponse) {
    const message = normalizeMessage(messageResponse);

    setConversations((previous) => {
      const result = applyIncomingMessageToList(
        previous,
        message,
        currentUserRef.current.id,
      );

      if (!result.applied) {
        storePendingRealtimeMessage(message);
      }

      return result.conversations;
    });

    if (!isSameId(message.senderId, currentUserRef.current.id)) {
      sendMessageDelivered(message);
      setConversationTyping(message.conversationId, false);

      if (isSameId(activeConversationIdRef.current, message.conversationId)) {
        markConversationRead(message.conversationId, message.id);
      }
    }
  }

  function applyRealtimeConversation(conversationResponse) {
    setContacts((previous) =>
      mergeContacts(previous, getConversationContacts(conversationResponse)),
    );
    setConversations((previous) => {
      const existing = previous.find(
        (conversation) => String(conversation.id) === String(conversationResponse.id),
      );
      const normalizedConversation = normalizeConversation(
        conversationResponse,
        dedupeById([
          ...(existing?.messages ?? []),
          ...takePendingRealtimeMessages(conversationResponse.id),
        ]),
      );

      return mergeConversationList(previous, [normalizedConversation]);
    });
  }

  function applyRealtimeMessageStatus(data) {
    setConversations((previous) =>
      applyMessageStatusToList(previous, data, currentUserRef.current.id),
    );
  }

  function applyRealtimeReadReceipt(event) {
    const readerId = event.data?.readerId ?? event.data?.readerUserId ?? event.data?.userId;
    const lastReadMessageId = event.data?.lastReadMessageId;
    const readAt = event.data?.readAt ?? event.occurredAt ?? new Date().toISOString();

    setConversations((previous) =>
      applyReadReceiptToList(previous, {
        conversationId: event.conversationId,
        readerId,
        lastReadMessageId,
        readAt,
        currentUserId: currentUserRef.current.id,
      }),
    );
  }

  function applyRealtimeTyping(event) {
    const actorUserId = event.data?.userId ?? event.data?.actorUserId;

    if (!event.conversationId || isSameId(actorUserId, currentUserRef.current.id)) {
      return;
    }

    const typing =
      typeof event.data?.typing === "boolean"
        ? event.data.typing
        : event.data?.action === "START";

    setConversationTyping(event.conversationId, Boolean(typing));
  }

  function applyRealtimePresence(data) {
    const userId = data?.userId;
    if (!userId) return;

    const presence = {
      isOnline: Boolean(data.isOnline),
      lastActiveAt: data.lastActiveAt ?? null,
    };

    setContacts((previous) =>
      previous.map((contact) =>
        isSameId(contact.id, userId) ? { ...contact, presence } : contact,
      ),
    );
    setConversations((previous) =>
      previous.map((conversation) =>
        isSameId(conversation.otherParticipantId, userId)
          ? {
              ...conversation,
              otherParticipant: {
                ...conversation.otherParticipant,
                presence,
              },
            }
          : conversation,
      ),
    );
  }

  function applyRealtimeMessagePinned(event) {
    const pin = event.data?.pin;
    const conversationId = event.conversationId ?? pin?.message?.conversationId;
    const messageId = pin?.message?.id;

    setMessagePinned(conversationId, messageId, true);
    setPinnedMessageDetail(conversationId, pin);
  }

  function applyRealtimeMessageUnpinned(event) {
    const conversationId = event.conversationId;
    const messageId = event.data?.messageId;

    setMessagePinned(conversationId, messageId, false);
    removePinnedMessageDetail(conversationId, messageId);
  }

  function handleRealtimeEvent(event) {
    if (!event?.eventType) return;

    if (event.eventType === "message.created" && event.data?.message) {
      applyRealtimeMessage(event.data.message);
      return;
    }

    if (event.eventType === "message.updated" && event.data?.message) {
      applyMessageResponseToConversation(event.data.message);
      return;
    }

    if (event.eventType === "message.deleted" && event.data?.message) {
      applyMessageResponseToConversation(event.data.message);
      return;
    }

    if (event.eventType === "conversation.updated" && event.data?.conversation) {
      applyRealtimeConversation(event.data.conversation);
      return;
    }

    if (["group.created", "group.member.added", "group.member.removed", "group.updated"].includes(event.eventType)) {
      const conversation = event.data?.conversation ?? event.data?.group;
      if (conversation) applyRealtimeConversation(conversation);
      return;
    }

    if (event.eventType === "message.read") {
      applyRealtimeReadReceipt(event);
      return;
    }

    if (event.eventType === "message.pinned") {
      applyRealtimeMessagePinned(event);
      return;
    }

    if (event.eventType === "message.unpinned") {
      applyRealtimeMessageUnpinned(event);
      return;
    }

    if (event.eventType === "message.status.updated") {
      applyRealtimeMessageStatus(event.data);
      return;
    }

    if (event.eventType === "typing.updated") {
      applyRealtimeTyping(event);
      return;
    }

    if (event.eventType === "presence.updated") {
      applyRealtimePresence(event.data);
    }
  }

  return {
    handleRealtimeEvent,
    sendMessageDelivered,
    storePendingRealtimeMessage,
    takePendingRealtimeMessages,
  };
}
