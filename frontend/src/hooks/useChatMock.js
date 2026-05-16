import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { currentUserMock } from "../data/mockData";
import {
  clearAuthSession,
  getAuthSession,
  hasValidAuthSession,
  isPersistentSession,
  saveAuthSession,
} from "../utils/authStorage";
import { setAuthEventHandlers } from "../services/apiClient";
import {
  createDirectConversation,
  getConversation,
  listConversations,
} from "../services/conversationApi";
import {
  listMessages,
  markMessagesRead,
  sendMessage as sendMessageApi,
} from "../services/messageApi";
import { createRealtimeClient } from "../services/realtimeClient";
import { getMe, searchUsers as searchUsersApi, updateMe } from "../services/userApi";

const SESSION_EXPIRED_MESSAGE = "Your session has expired. Please sign in again.";

function createClientId() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }

  return "10000000-1000-4000-8000-100000000000".replace(/[018]/g, (char) =>
    (Number(char) ^ ((Math.random() * 16) >> (Number(char) / 4))).toString(16),
  );
}

function toCurrentUser(user) {
  const userId = user?.id ?? currentUserMock.id;

  return {
    ...currentUserMock,
    ...user,
    id: userId,
    backendId: userId,
    bio: user?.bio ?? currentUserMock.bio,
    accent: user?.accent ?? currentUserMock.accent,
  };
}

function toContact(user) {
  return {
    id: user.id,
    backendId: user.id,
    username: user.username,
    email: user.email ?? "",
    displayName: user.displayName,
    avatarUrl: user.avatarUrl ?? null,
    role: user.role ?? (user.directConversationId ? "Existing direct chat" : "Active user"),
    bio: user.bio ?? "",
    accent: user.accent ?? "from-sky-300 to-blue-500",
    presence: user.presence ?? { isOnline: false, lastActiveAt: null },
    directConversationId: user.directConversationId ?? null,
  };
}

function mergeContacts(previousContacts, nextContacts) {
  const byId = new Map(previousContacts.map((contact) => [String(contact.id), contact]));

  nextContacts.forEach((contact) => {
    byId.set(String(contact.id), {
      ...byId.get(String(contact.id)),
      ...contact,
    });
  });

  return Array.from(byId.values());
}

function getStoredAuthSession() {
  if (!hasValidAuthSession()) {
    clearAuthSession();
    return null;
  }

  return getAuthSession();
}

function isSameId(left, right) {
  return left != null && right != null && String(left) === String(right);
}

function getMessageSenderId(message) {
  return message.sender?.id ?? message.senderId ?? null;
}

function normalizeMessage(message) {
  return {
    id: message.id,
    clientMessageId: message.clientMessageId,
    conversationId: message.conversationId,
    senderId: getMessageSenderId(message),
    sender: message.sender,
    content: message.content,
    messageType: message.messageType,
    status: message.status,
    createdAt: message.createdAt,
    deliveredAt: message.deliveredAt,
    readAt: message.readAt,
  };
}

function normalizeLastMessage(lastMessage) {
  if (!lastMessage) return null;

  return {
    ...lastMessage,
    senderId: getMessageSenderId(lastMessage),
  };
}

function dedupeById(items) {
  return Array.from(new Map(items.map((item) => [String(item.id), item])).values());
}

function getMessageIndex(messages, messageId) {
  return messages.findIndex((message) => isSameId(message.id, messageId));
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
  const [contacts, setContacts] = useState([]);
  const [userSearchResults, setUserSearchResults] = useState([]);
  const [isSearchingUsers, setIsSearchingUsers] = useState(false);
  const [userSearchError, setUserSearchError] = useState("");
  const [conversations, setConversations] = useState([]);
  const [conversationPaging, setConversationPaging] = useState(null);
  const [isLoadingConversations, setIsLoadingConversations] = useState(false);
  const [isLoadingMoreConversations, setIsLoadingMoreConversations] = useState(false);
  const [conversationError, setConversationError] = useState("");
  const [messagePagingByConversation, setMessagePagingByConversation] = useState({});
  const [loadingMessagesByConversation, setLoadingMessagesByConversation] = useState({});
  const [loadingOlderMessagesByConversation, setLoadingOlderMessagesByConversation] = useState({});
  const [messageErrorByConversation, setMessageErrorByConversation] = useState({});
  const [chatActionError, setChatActionError] = useState("");
  const [typingByConversation, setTypingByConversation] = useState({});
  const [isStartingConversation, setIsStartingConversation] = useState(false);
  const [startConversationError, setStartConversationError] = useState("");
  const [sendingByConversation, setSendingByConversation] = useState({});
  const [realtimeStatus, setRealtimeStatus] = useState("idle");
  const [realtimeError, setRealtimeError] = useState("");
  const activeConversationIdRef = useRef(null);
  const currentUserRef = useRef(currentUser);
  const pendingMessagesByConversationRef = useRef(new Map());
  const realtimeClientRef = useRef(null);
  const realtimeEventHandlerRef = useRef(null);
  const typingTimeoutsRef = useRef(new Map());
  currentUserRef.current = currentUser;

  const isLoadingSelectedConversation = (conversationId) =>
    Boolean(conversationId && loadingMessagesByConversation[conversationId]);
  const isLoadingOlderMessages = (conversationId) =>
    Boolean(conversationId && loadingOlderMessagesByConversation[conversationId]);
  const getMessageError = (conversationId) =>
    (conversationId && messageErrorByConversation[conversationId]) || "";
  const getMessagePaging = (conversationId) =>
    (conversationId && messagePagingByConversation[conversationId]) || null;
  const hasMoreMessages = (conversationId) => Boolean(getMessagePaging(conversationId)?.hasMore);
  const isSendingMessage = (conversationId) =>
    Boolean(conversationId && sendingByConversation[conversationId]);

  const conversationSummaries = useMemo(() => {
    return conversations
      .map((conversation) => {
        const otherParticipant = contacts.find(
          (contact) => contact.id === conversation.otherParticipantId,
        ) ?? conversation.otherParticipant;
        const lastMessage = conversation.messages.at(-1) ?? conversation.lastMessage ?? null;

        return {
          ...conversation,
          otherParticipant,
          lastMessage: lastMessage
            ? {
                id: lastMessage.id,
                senderId: lastMessage.senderId,
                contentPreview: lastMessage.content ?? lastMessage.contentPreview ?? "",
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

  function normalizeConversation(conversation, existingMessages = []) {
    const otherParticipant = toContact(conversation.otherParticipant);

    return {
      id: conversation.id,
      type: conversation.type,
      otherParticipantId: otherParticipant.id,
      otherParticipant,
      participants: conversation.participants ?? [],
      unreadCount: conversation.unreadCount ?? 0,
      pinned: false,
      muted: false,
      lastMessage: normalizeLastMessage(conversation.lastMessage),
      lastMessageAt: conversation.lastMessageAt,
      createdAt: conversation.createdAt,
      updatedAt: conversation.updatedAt,
      messages: existingMessages,
    };
  }

  function mergeConversationList(previous, nextItems) {
    const previousById = new Map(previous.map((conversation) => [String(conversation.id), conversation]));

    nextItems.forEach((conversation) => {
      const existing = previousById.get(String(conversation.id));
      previousById.set(String(conversation.id), {
        ...existing,
        ...conversation,
        messages: conversation.messages?.length ? conversation.messages : existing?.messages ?? [],
      });
    });

    return Array.from(previousById.values()).sort((a, b) => {
      const left = new Date(a.lastMessageAt ?? a.updatedAt ?? a.createdAt).getTime();
      const right = new Date(b.lastMessageAt ?? b.updatedAt ?? b.createdAt).getTime();
      return right - left;
    });
  }

  function updateConversationMessages(conversationId, updater) {
    setConversations((previous) =>
      previous.map((conversation) =>
        String(conversation.id) === String(conversationId)
          ? {
              ...conversation,
              messages: updater(conversation.messages ?? []),
            }
          : conversation,
      ),
    );
  }

  function clearTypingTimeout(conversationId) {
    const key = String(conversationId);
    const timeoutId = typingTimeoutsRef.current.get(key);

    if (timeoutId) {
      window.clearTimeout(timeoutId);
      typingTimeoutsRef.current.delete(key);
    }
  }

  function setConversationTyping(conversationId, typing) {
    if (!conversationId) return;

    clearTypingTimeout(conversationId);
    setTypingByConversation((previous) => ({
      ...previous,
      [conversationId]: typing,
    }));

    if (typing) {
      const timeoutId = window.setTimeout(() => {
        setTypingByConversation((previous) => ({
          ...previous,
          [conversationId]: false,
        }));
        typingTimeoutsRef.current.delete(String(conversationId));
      }, 5000);

      typingTimeoutsRef.current.set(String(conversationId), timeoutId);
    }
  }

  function clearAllTypingTimeouts() {
    typingTimeoutsRef.current.forEach((timeoutId) => window.clearTimeout(timeoutId));
    typingTimeoutsRef.current.clear();
  }

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
    setConversations([]);
    setConversationPaging(null);
    setContacts([]);
    setUserSearchResults([]);
    setMessagePagingByConversation({});
    setTypingByConversation({});
    activeConversationIdRef.current = null;
    pendingMessagesByConversationRef.current.clear();
    clearAllTypingTimeouts();
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
    return () => clearAllTypingTimeouts();
  }, []);

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

  async function loadConversations({ append = false } = {}) {
    const paging = append ? conversationPaging : null;

    if (append && !paging?.hasMore) return;

    if (append) {
      setIsLoadingMoreConversations(true);
    } else {
      setIsLoadingConversations(true);
    }
    setConversationError("");

    try {
      const response = await listConversations({
        limit: 20,
        cursor: append ? paging?.nextCursor : null,
        snapshotAt: append ? paging?.snapshotAt : null,
      });
      const normalized = (response.items ?? []).map((conversation) => {
        const existing = conversations.find((item) => String(item.id) === String(conversation.id));
        return normalizeConversation(conversation, existing?.messages ?? []);
      });
      const nextContacts = normalized.map((conversation) => conversation.otherParticipant);

      setContacts((previous) => mergeContacts(previous, nextContacts));
      setConversations((previous) =>
        append ? mergeConversationList(previous, normalized) : mergeConversationList([], normalized),
      );
      setConversationPaging(response.paging ?? null);
    } catch (error) {
      setConversationError(error.message || "Could not load conversations.");
    } finally {
      setIsLoadingConversations(false);
      setIsLoadingMoreConversations(false);
    }
  }

  useEffect(() => {
    if (authStatus === "authenticated") {
      loadConversations();
    }
  }, [authStatus]);

  async function loadConversation(conversationId) {
    if (!conversationId) return null;

    setLoadingMessagesByConversation((previous) => ({ ...previous, [conversationId]: true }));
    setMessageErrorByConversation((previous) => ({ ...previous, [conversationId]: "" }));

    try {
      const [conversationResponse, messageResponse] = await Promise.all([
        getConversation(conversationId),
        listMessages({ conversationId, limit: 20 }),
      ]);
      const messages = (messageResponse.items ?? []).map((message) =>
        normalizeMessage(message),
      );
      const normalizedConversation = normalizeConversation(conversationResponse, messages);

      setContacts((previous) => mergeContacts(previous, [normalizedConversation.otherParticipant]));
      setConversations((previous) => mergeConversationList(previous, [normalizedConversation]));
      setMessagePagingByConversation((previous) => ({
        ...previous,
        [conversationId]: messageResponse.paging ?? null,
      }));

      return normalizedConversation;
    } catch (error) {
      setMessageErrorByConversation((previous) => ({
        ...previous,
        [conversationId]: error.message || "Could not load messages.",
      }));
      return null;
    } finally {
      setLoadingMessagesByConversation((previous) => ({ ...previous, [conversationId]: false }));
    }
  }

  async function loadMoreMessages(conversationId) {
    const paging = getMessagePaging(conversationId);
    if (!conversationId || !paging?.hasMore || !paging.nextCursor) return;

    setLoadingOlderMessagesByConversation((previous) => ({ ...previous, [conversationId]: true }));

    try {
      const response = await listMessages({
        conversationId,
        limit: 20,
        cursor: paging.nextCursor,
      });
      const olderMessages = (response.items ?? []).map((message) =>
        normalizeMessage(message),
      );

      updateConversationMessages(conversationId, (messages) =>
        dedupeById([...olderMessages, ...messages]),
      );
      setMessagePagingByConversation((previous) => ({
        ...previous,
        [conversationId]: response.paging ?? null,
      }));
    } catch (error) {
      setMessageErrorByConversation((previous) => ({
        ...previous,
        [conversationId]: error.message || "Could not load older messages.",
      }));
    } finally {
      setLoadingOlderMessagesByConversation((previous) => ({
        ...previous,
        [conversationId]: false,
      }));
    }
  }

  function applyRealtimeMessage(messageResponse) {
    const message = normalizeMessage(messageResponse);
    const lastMessage = {
      id: message.id,
      senderId: message.senderId,
      contentPreview: message.content,
      status: message.status,
      createdAt: message.createdAt,
    };

    setConversations((previous) => {
      let messageWasApplied = false;
      const next = previous.map((conversation) => {
        if (String(conversation.id) !== String(message.conversationId)) {
          return conversation;
        }

        messageWasApplied = true;

        return {
          ...conversation,
          messages: dedupeById([...(conversation.messages ?? []), message]),
          lastMessage,
          lastMessageAt: message.createdAt,
          updatedAt: message.createdAt,
          unreadCount:
            isSameId(message.senderId, currentUserRef.current.id)
              ? conversation.unreadCount
              : (conversation.unreadCount ?? 0) + 1,
        };
      });

      if (!messageWasApplied) {
        storePendingRealtimeMessage(message);
        return previous;
      }

      return mergeConversationList(next, []);
    });

    if (!isSameId(message.senderId, currentUserRef.current.id)) {
      setConversationTyping(message.conversationId, false);

      if (isSameId(activeConversationIdRef.current, message.conversationId)) {
        markConversationRead(message.conversationId, message.id);
      }
    }
  }

  function applyRealtimeConversation(conversationResponse) {
    setContacts((previous) => mergeContacts(previous, [toContact(conversationResponse.otherParticipant)]));
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
    const messageId = data?.messageId;
    if (!messageId) return;

    setConversations((previous) =>
      previous.map((conversation) => ({
        ...conversation,
        lastMessage: isSameId(conversation.lastMessage?.id, messageId)
          ? {
              ...conversation.lastMessage,
              status: data.status ?? conversation.lastMessage.status,
              deliveredAt: data.deliveredAt ?? conversation.lastMessage.deliveredAt,
              readAt: data.readAt ?? conversation.lastMessage.readAt,
            }
          : conversation.lastMessage,
        messages: (conversation.messages ?? []).map((message) =>
          isSameId(message.id, messageId)
            ? {
                ...message,
                status: data.status ?? message.status,
                deliveredAt: data.deliveredAt ?? message.deliveredAt,
                readAt: data.readAt ?? message.readAt,
              }
            : message,
        ),
      })),
    );
  }

  function applyRealtimeReadReceipt(event) {
    const readerId = event.data?.readerId ?? event.data?.readerUserId ?? event.data?.userId;
    const lastReadMessageId = event.data?.lastReadMessageId;
    const readAt = event.data?.readAt ?? event.occurredAt ?? new Date().toISOString();

    if (!event.conversationId || !readerId || !lastReadMessageId) return;

    setConversations((previous) =>
      previous.map((conversation) => {
        if (String(conversation.id) !== String(event.conversationId)) {
          return conversation;
        }

        const messages = conversation.messages ?? [];
        const cutoffIndex = getMessageIndex(messages, lastReadMessageId);
        const nextMessages =
          cutoffIndex === -1
            ? messages
            : messages.map((message, index) =>
                index <= cutoffIndex && !isSameId(message.senderId, readerId)
                  ? { ...message, status: "READ", readAt: message.readAt ?? readAt }
                  : message,
              );

        const lastMessage =
          conversation.lastMessage && isSameId(conversation.lastMessage.id, lastReadMessageId)
            ? { ...conversation.lastMessage, status: "READ", readAt }
            : conversation.lastMessage;

        return {
          ...conversation,
          lastMessage,
          messages: nextMessages,
          unreadCount: isSameId(readerId, currentUserRef.current.id)
            ? 0
            : conversation.unreadCount,
        };
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
        isSameId(contact.id, userId)
          ? { ...contact, presence }
          : contact,
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

  function handleRealtimeEvent(event) {
    if (!event?.eventType) return;

    if (event.eventType === "message.created" && event.data?.message) {
      applyRealtimeMessage(event.data.message);
      return;
    }

    if (event.eventType === "conversation.updated" && event.data?.conversation) {
      applyRealtimeConversation(event.data.conversation);
      return;
    }

    if (event.eventType === "message.read") {
      applyRealtimeReadReceipt(event);
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

  realtimeEventHandlerRef.current = handleRealtimeEvent;

  useEffect(() => {
    if (authStatus !== "authenticated") {
      realtimeClientRef.current?.disconnect();
      realtimeClientRef.current = null;
      setRealtimeStatus("idle");
      return undefined;
    }

    const client = createRealtimeClient({
      onError: (message) => setRealtimeError(String(message)),
      onEvent: (event) => realtimeEventHandlerRef.current?.(event),
      onStatus: (status) => {
        setRealtimeStatus(status);
        if (status === "connected") {
          setRealtimeError("");
        }
      },
    });

    realtimeClientRef.current = client;
    client.connect();

    return () => {
      client.disconnect();
      if (realtimeClientRef.current === client) {
        realtimeClientRef.current = null;
      }
    };
  }, [authStatus]);

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

  async function markConversationRead(conversationId, explicitLastReadMessageId = null) {
    const conversation = conversations.find((item) => String(item.id) === String(conversationId));
    const lastReadMessageId =
      explicitLastReadMessageId ?? conversation?.messages?.at(-1)?.id ?? conversation?.lastMessage?.id;

    if (!conversationId || !lastReadMessageId) return;

    try {
      const response = await markMessagesRead({ conversationId, lastReadMessageId });
      const readAt = response.readAt ?? new Date().toISOString();

      setConversations((previous) =>
        previous.map((item) => {
          if (String(item.id) !== String(conversationId)) return item;

          return {
            ...item,
            unreadCount: response.unreadCount ?? 0,
            messages: (item.messages ?? []).map((message) =>
              isSameId(message.senderId, currentUserRef.current.id)
                ? message
                : { ...message, status: "READ", readAt: message.readAt ?? readAt },
            ),
          };
        }),
      );
    } catch {
      // Read receipts should not block the chat surface if a realtime race loses.
    }
  }

  async function sendMessage(conversationId, content) {
    const trimmed = content.trim();
    if (!trimmed) return null;

    setSendingByConversation((previous) => ({ ...previous, [conversationId]: true }));
    setChatActionError("");

    try {
      const response = await sendMessageApi({
        conversationId: Number(conversationId),
        clientMessageId: createClientId(),
        content: trimmed,
        messageType: "TEXT",
      });
      const message = normalizeMessage(response);
      const lastMessage = {
        id: message.id,
        senderId: message.senderId,
        contentPreview: message.content,
        status: message.status,
        createdAt: message.createdAt,
      };

      setConversations((previous) =>
        mergeConversationList(
          previous.map((conversation) =>
            String(conversation.id) === String(conversationId)
              ? {
                  ...conversation,
                  messages: dedupeById([...(conversation.messages ?? []), message]),
                  lastMessage,
                  lastMessageAt: message.createdAt,
                  updatedAt: message.createdAt,
                  unreadCount: 0,
                }
              : conversation,
          ),
          [],
        ),
      );

      return message;
    } catch (error) {
      setChatActionError(error.message || "Could not send message.");
      return null;
    } finally {
      setSendingByConversation((previous) => ({ ...previous, [conversationId]: false }));
    }
  }

  async function startConversation(targetUserId) {
    const existing = conversations.find(
      (conversation) => String(conversation.otherParticipantId) === String(targetUserId),
    );

    if (existing) return existing.id;

    setIsStartingConversation(true);
    setStartConversationError("");

    try {
      const response = await createDirectConversation(Number(targetUserId));
      const normalizedConversation = normalizeConversation(response, []);

      setContacts((previous) => mergeContacts(previous, [normalizedConversation.otherParticipant]));
      setConversations((previous) => mergeConversationList(previous, [normalizedConversation]));
      setMessagePagingByConversation((previous) => ({
        ...previous,
        [normalizedConversation.id]: null,
      }));

      return normalizedConversation.id;
    } catch (error) {
      setStartConversationError(error.message || "Could not start conversation.");
      return null;
    } finally {
      setIsStartingConversation(false);
    }
  }

  async function updateProfile(nextProfile) {
    const user = await updateMe({
      displayName: nextProfile.displayName?.trim() || null,
      avatarUrl: nextProfile.avatarUrl ?? currentUser.avatarUrl ?? null,
      bio: nextProfile.bio?.trim() || null,
    });

    setCurrentUser(toCurrentUser(user));

    setAuthSession((previous) => {
      if (!previous) return previous;

      const nextAuthSession = {
        ...previous,
        user,
      };

      saveAuthSession(nextAuthSession, isPersistentSession());
      return nextAuthSession;
    });

    return user;
  }

  const searchUsers = useCallback(async (query) => {
    const normalized = query.trim();

    if (!normalized) {
      setUserSearchResults([]);
      setUserSearchError("");
      setIsSearchingUsers(false);
      return [];
    }

    setIsSearchingUsers(true);
    setUserSearchError("");

    try {
      const response = await searchUsersApi(normalized, { limit: 20 });
      const results = (response.items ?? []).map(toContact);
      setUserSearchResults(results);
      setContacts((previous) => mergeContacts(previous, results));
      return results;
    } catch (error) {
      setUserSearchError(error.message || "Could not search users.");
      setUserSearchResults([]);
      return [];
    } finally {
      setIsSearchingUsers(false);
    }
  }, []);

  const clearUserSearch = useCallback(() => {
    setUserSearchResults([]);
    setUserSearchError("");
    setIsSearchingUsers(false);
  }, []);

  function setAuthenticatedUser(user) {
    setCurrentUser(toCurrentUser(user));
  }

  function setAuthenticatedSession(nextAuthSession, rememberSession = true) {
    applyAuthenticatedSession(nextAuthSession, rememberSession);
  }

  const setActiveConversationId = useCallback((conversationId) => {
    activeConversationIdRef.current = conversationId == null ? null : String(conversationId);
  }, []);

  function sendTypingStatus(conversationId, typing) {
    realtimeClientRef.current?.sendTypingStatus(conversationId, typing);
  }

  function signOut(message = "") {
    clearAuthenticatedSession(message);
  }

  return {
    authSession,
    authMessage,
    authStatus,
    contacts,
    chatActionError,
    clearUserSearch,
    conversations,
    conversationError,
    conversationPaging,
    conversationSummaries,
    currentUser,
    getConversationById,
    hasMoreMessages,
    isSearchingUsers,
    isLoadingConversations,
    isLoadingMoreConversations,
    isLoadingOlderMessages,
    isLoadingSelectedConversation,
    isSendingMessage,
    isStartingConversation,
    loadConversation,
    loadConversations,
    loadMoreMessages,
    markConversationRead,
    realtimeError,
    realtimeStatus,
    sendMessage,
    searchUsers,
    startConversation,
    startConversationError,
    stats,
    isAuthenticated: authStatus === "authenticated" && Boolean(authSession),
    isAuthLoading: authStatus === "checking",
    setActiveConversationId,
    setAuthenticatedSession,
    setAuthenticatedUser,
    signOut,
    sendTypingStatus,
    typingByConversation,
    updateProfile,
    getMessageError,
    userSearchError,
    userSearchResults,
  };
}
