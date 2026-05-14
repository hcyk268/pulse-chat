import { useCallback, useEffect, useMemo, useState } from "react";
import {
  contactsMock,
  conversationsMock,
  currentUserMock,
  suggestedReplies,
} from "../data/mockData";
import {
  clearAuthSession,
  getAuthSession,
  hasValidAuthSession,
  isPersistentSession,
  saveAuthSession,
} from "../utils/authStorage";
import { setAuthEventHandlers } from "../services/apiClient";
import { getMe } from "../services/userApi";

const statusRank = {
  SENT: 1,
  DELIVERED: 2,
  READ: 3,
};

const SESSION_EXPIRED_MESSAGE = "Your session has expired. Please sign in again.";

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

function toCurrentUser(user) {
  return {
    ...currentUserMock,
    ...user,
    id: currentUserMock.id,
    backendId: user?.id ?? currentUserMock.id,
    bio: user?.bio ?? currentUserMock.bio,
    accent: user?.accent ?? currentUserMock.accent,
  };
}

function getStoredAuthSession() {
  if (!hasValidAuthSession()) {
    clearAuthSession();
    return null;
  }

  return getAuthSession();
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
  const [authSession, setAuthSession] = useState(getStoredAuthSession);
  const [currentUser, setCurrentUser] = useState(() => {
    return authSession?.user ? toCurrentUser(authSession.user) : currentUserMock;
  });
  const [authStatus, setAuthStatus] = useState(() =>
    authSession ? "checking" : "unauthenticated",
  );
  const [authMessage, setAuthMessage] = useState("");
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

  const applyAuthenticatedSession = useCallback((nextAuthSession, rememberSession = true) => {
    saveAuthSession(nextAuthSession, rememberSession);
    setAuthSession(nextAuthSession);
    setCurrentUser(toCurrentUser(nextAuthSession.user));
    setAuthStatus("authenticated");
    setAuthMessage("");
  }, []);

  const clearAuthenticatedSession = useCallback((message = "") => {
    clearAuthSession();
    setAuthSession(null);
    setCurrentUser(currentUserMock);
    setAuthStatus("unauthenticated");
    setAuthMessage(message);
  }, []);

  useEffect(() => {
    setAuthEventHandlers({
      onAuthFailure: (message) => {
        clearAuthenticatedSession(message || SESSION_EXPIRED_MESSAGE);
      },
      onAuthRefresh: (nextAuthSession) => {
        applyAuthenticatedSession(nextAuthSession, isPersistentSession());
      },
    });

    return () => setAuthEventHandlers();
  }, [applyAuthenticatedSession, clearAuthenticatedSession]);

  useEffect(() => {
    let cancelled = false;

    async function verifyStoredSession() {
      const storedAuthSession = getStoredAuthSession();

      if (!storedAuthSession) {
        if (!cancelled) {
          setAuthStatus("unauthenticated");
        }
        return;
      }

      try {
        setAuthStatus("checking");
        const user = await getMe();
        if (cancelled) return;

        const latestAuthSession = getAuthSession() ?? storedAuthSession;
        applyAuthenticatedSession(
          {
            ...latestAuthSession,
            user,
          },
          isPersistentSession(),
        );
      } catch (error) {
        if (cancelled) return;

        clearAuthenticatedSession(error.message || SESSION_EXPIRED_MESSAGE);
      }
    }

    verifyStoredSession();

    return () => {
      cancelled = true;
    };
  }, [applyAuthenticatedSession, clearAuthenticatedSession]);

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

  function setAuthenticatedUser(user) {
    setCurrentUser(toCurrentUser(user));
  }

  function setAuthenticatedSession(nextAuthSession, rememberSession = true) {
    applyAuthenticatedSession(nextAuthSession, rememberSession);
  }

  function signOut(message = "") {
    clearAuthenticatedSession(message);
  }

  return {
    authSession,
    authMessage,
    authStatus,
    contacts,
    conversations,
    conversationSummaries,
    currentUser,
    getConversationById,
    markConversationRead,
    sendMessage,
    startConversation,
    stats,
    isAuthenticated: authStatus === "authenticated" && Boolean(authSession),
    isAuthLoading: authStatus === "checking",
    setAuthenticatedSession,
    setAuthenticatedUser,
    signOut,
    typingByConversation,
    updateProfile,
  };
}
