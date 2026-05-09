import { useMemo, useState } from "react";
import {
  contactsMock,
  conversationsMock,
  currentUserMock,
  suggestedReplies,
} from "../data/mockData";

const statusRank = {
  SENT: 1,
  DELIVERED: 2,
  READ: 3,
};

function cloneConversations() {
  return conversationsMock.map((conversation) => ({
    ...conversation,
    messages: conversation.messages.map((message) => ({ ...message })),
  }));
}

function createClientId() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }

  return `client-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function nextNumericId(items, fallback) {
  return items.reduce((max, item) => Math.max(max, Number(item.id) || 0), fallback) + 1;
}

function upgradeStatus(message, nextStatus) {
  if ((statusRank[message.status] || 0) >= statusRank[nextStatus]) {
    return message;
  }

  const now = new Date().toISOString();

  return {
    ...message,
    status: nextStatus,
    deliveredAt: nextStatus === "DELIVERED" ? now : message.deliveredAt,
    readAt: nextStatus === "READ" ? now : message.readAt,
  };
}

export function useChatMock() {
  const [currentUser, setCurrentUser] = useState(currentUserMock);
  const [contacts] = useState(contactsMock);
  const [conversations, setConversations] = useState(cloneConversations);
  const [typingByConversation, setTypingByConversation] = useState({
    1001: true,
  });

  const conversationSummaries = useMemo(() => {
    return conversations
      .map((conversation) => {
        const otherParticipant = contacts.find(
          (contact) => contact.id === conversation.otherParticipantId,
        );
        const lastMessage = conversation.messages.at(-1) ?? null;

        return {
          ...conversation,
          otherParticipant,
          lastMessage: lastMessage
            ? {
                id: lastMessage.id,
                senderId: lastMessage.senderId,
                contentPreview: lastMessage.content,
                status: lastMessage.status,
                createdAt: lastMessage.createdAt,
              }
            : null,
        };
      })
      .sort((a, b) => {
        if (a.pinned !== b.pinned) return a.pinned ? -1 : 1;

        const left = new Date(a.lastMessageAt ?? a.createdAt).getTime();
        const right = new Date(b.lastMessageAt ?? b.createdAt).getTime();

        return right - left;
      });
  }, [contacts, conversations]);

  const stats = useMemo(() => {
    return {
      unreadTotal: conversations.reduce((total, item) => total + item.unreadCount, 0),
      onlineCount: contacts.filter((contact) => contact.presence.isOnline).length,
      activeConversations: conversations.length,
    };
  }, [contacts, conversations]);

  function getConversationById(conversationId) {
    const conversation = conversations.find(
      (item) => String(item.id) === String(conversationId),
    );

    if (!conversation) return null;

    const otherParticipant = contacts.find(
      (contact) => contact.id === conversation.otherParticipantId,
    );

    return { ...conversation, otherParticipant };
  }

  function markConversationRead(conversationId) {
    const now = new Date().toISOString();

    setConversations((previous) =>
      previous.map((conversation) => {
        if (String(conversation.id) !== String(conversationId)) return conversation;

        return {
          ...conversation,
          unreadCount: 0,
          messages: conversation.messages.map((message) =>
            message.senderId === currentUser.id
              ? message
              : { ...message, status: "READ", readAt: message.readAt ?? now },
          ),
        };
      }),
    );
  }

  function updateMessageStatus(conversationId, messageId, nextStatus) {
    setConversations((previous) =>
      previous.map((conversation) => {
        if (String(conversation.id) !== String(conversationId)) return conversation;

        return {
          ...conversation,
          messages: conversation.messages.map((message) =>
            String(message.id) === String(messageId) ? upgradeStatus(message, nextStatus) : message,
          ),
        };
      }),
    );
  }

  function sendMessage(conversationId, content) {
    const trimmed = content.trim();
    if (!trimmed) return;

    const now = new Date().toISOString();
    const messageId = Date.now();

    const message = {
      id: messageId,
      clientMessageId: createClientId(),
      conversationId: Number(conversationId),
      senderId: currentUser.id,
      content: trimmed,
      messageType: "TEXT",
      status: "SENT",
      createdAt: now,
      deliveredAt: null,
      readAt: null,
    };

    setConversations((previous) =>
      previous.map((conversation) =>
        String(conversation.id) === String(conversationId)
          ? {
              ...conversation,
              messages: [...conversation.messages, message],
              lastMessageAt: now,
              updatedAt: now,
              unreadCount: 0,
            }
          : conversation,
      ),
    );

    window.setTimeout(() => updateMessageStatus(conversationId, messageId, "DELIVERED"), 650);
    window.setTimeout(() => updateMessageStatus(conversationId, messageId, "READ"), 1500);
    window.setTimeout(() => {
      setTypingByConversation((previous) => ({ ...previous, [conversationId]: true }));
    }, 850);
    window.setTimeout(() => addMockReply(conversationId), 2300);
  }

  function addMockReply(conversationId) {
    const now = new Date().toISOString();
    const reply = suggestedReplies[Math.floor(Math.random() * suggestedReplies.length)];

    setConversations((previous) =>
      previous.map((conversation) => {
        if (String(conversation.id) !== String(conversationId)) return conversation;

        const otherParticipant = contacts.find(
          (contact) => contact.id === conversation.otherParticipantId,
        );

        return {
          ...conversation,
          messages: [
            ...conversation.messages,
            {
              id: Date.now() + 7,
              clientMessageId: `mock-${Date.now()}`,
              conversationId: Number(conversationId),
              senderId: otherParticipant?.id ?? 0,
              content: reply,
              messageType: "TEXT",
              status: "READ",
              createdAt: now,
              deliveredAt: now,
              readAt: now,
            },
          ],
          lastMessageAt: now,
          updatedAt: now,
          unreadCount: 0,
        };
      }),
    );

    setTypingByConversation((previous) => ({ ...previous, [conversationId]: false }));
  }

  function startConversation(targetUserId) {
    const existing = conversations.find(
      (conversation) => conversation.otherParticipantId === targetUserId,
    );

    if (existing) return existing.id;

    const now = new Date().toISOString();
    const conversationId = nextNumericId(conversations, 1000);

    setConversations((previous) => [
      {
        id: conversationId,
        type: "DIRECT",
        otherParticipantId: targetUserId,
        unreadCount: 0,
        pinned: false,
        muted: false,
        createdAt: now,
        updatedAt: now,
        lastMessageAt: null,
        messages: [],
      },
      ...previous,
    ]);

    return conversationId;
  }

  function updateProfile(nextProfile) {
    setCurrentUser((previous) => ({
      ...previous,
      ...nextProfile,
      updatedAt: new Date().toISOString(),
    }));
  }

  return {
    contacts,
    conversations,
    conversationSummaries,
    currentUser,
    getConversationById,
    markConversationRead,
    sendMessage,
    startConversation,
    stats,
    typingByConversation,
    updateProfile,
  };
}
