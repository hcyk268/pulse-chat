import { useCallback, useEffect, useMemo, useState } from "react";
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
  return {
    ...currentUserMock,
    ...user,
    id: currentUserMock.id,
    backendId: user?.id ?? currentUserMock.id,
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

function mapBackendUserId(backendUserId, currentUser) {
  return Number(backendUserId) === Number(currentUser.backendId) ? currentUser.id : backendUserId;
}

function normalizeMessage(message, currentUser) {
  return {
    id: message.id,
    clientMessageId: message.clientMessageId,
    conversationId: message.conversationId,
    senderId: mapBackendUserId(message.sender?.id, currentUser),
    sender: message.sender,
    content: message.content,
    messageType: message.messageType,
    status: message.status,
    createdAt: message.createdAt,
    deliveredAt: message.deliveredAt,
    readAt: message.readAt,
  };
}

function normalizeLastMessage(lastMessage, currentUser) {
  if (!lastMessage) return null;

  return {
    ...lastMessage,
    senderId: mapBackendUserId(lastMessage.senderId, currentUser),
  };
}

function dedupeById(items) {
  return Array.from(new Map(items.map((item) => [String(item.id), item])).values());
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
  const [typingByConversation] = useState({});
  const [isStartingConversation, setIsStartingConversation] = useState(false);
  const [startConversationError, setStartConversationError] = useState("");
  const [sendingByConversation, setSendingByConversation] = useState({});

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
      lastMessage: normalizeLastMessage(conversation.lastMessage, currentUser),
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
        normalizeMessage(message, currentUser),
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
        normalizeMessage(message, currentUser),
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
              message.senderId === currentUser.id
                ? message
                : { ...message, status: "READ", readAt: message.readAt ?? readAt },
            ),
          };
        }),
      );
    } catch {
      // Read receipts are best-effort in the REST-only UI.
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
      const message = normalizeMessage(response, currentUser);
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
    sendMessage,
    searchUsers,
    startConversation,
    startConversationError,
    stats,
    isAuthenticated: authStatus === "authenticated" && Boolean(authSession),
    isAuthLoading: authStatus === "checking",
    setAuthenticatedSession,
    setAuthenticatedUser,
    signOut,
    typingByConversation,
    updateProfile,
    getMessageError,
    userSearchError,
    userSearchResults,
  };
}
