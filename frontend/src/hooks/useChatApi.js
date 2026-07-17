import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { getAuthSession, isPersistentSession } from "../utils/authStorage";
import { isSameId } from "../utils/chat";
import { setAuthEventHandlers } from "../services/apiClient";
import { createRealtimeClient } from "../services/realtimeClient";
import { getMe } from "../services/userApi";
import {
  applyMessageResponseToList,
} from "../domain/chat/conversationState";
import {
  EMPTY_CURRENT_USER,
  getMessagePreview,
  getNormalizedConversationContacts,
  mergeContacts,
  normalizeMessage,
  toCurrentUser,
} from "../domain/chat/normalizers";
import {
  createSessionDomain,
  getStoredAuthSession,
  SESSION_EXPIRED_MESSAGE,
} from "../domain/chat/sessionDomain";
import { createConversationDomain } from "../domain/chat/conversationDomain";
import { createRealtimeDomain } from "../domain/chat/realtimeDomain";
import { createMessageDomain } from "../domain/chat/messageDomain";
import { createConversationActionsDomain } from "../domain/chat/conversationActionsDomain";
import { useToast } from "./useToast";

export function useChatApi() {
  const toast = useToast();
  const [authSession, setAuthSession] = useState(getStoredAuthSession);
  const [currentUser, setCurrentUser] = useState(() => {
    return authSession?.user ? toCurrentUser(authSession.user) : EMPTY_CURRENT_USER;
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
  const [hasLoadedConversations, setHasLoadedConversations] = useState(false);
  const [isLoadingMoreConversations, setIsLoadingMoreConversations] = useState(false);
  const [conversationError, setConversationError] = useState("");
  const [messagePagingByConversation, setMessagePagingByConversation] = useState({});
  const [loadingMessagesByConversation, setLoadingMessagesByConversation] = useState({});
  const [loadingOlderMessagesByConversation, setLoadingOlderMessagesByConversation] = useState({});
  const [messageErrorByConversation, setMessageErrorByConversation] = useState({});
  const [pinnedMessageIdsByConversation, setPinnedMessageIdsByConversation] = useState({});
  const [pinnedMessagesByConversation, setPinnedMessagesByConversation] = useState({});
  const [reactionsByMessageId, setReactionsByMessageId] = useState({});
  const [readReceiptsByMessageId, setReadReceiptsByMessageId] = useState({});
  const [loadingReadReceiptsByMessageId, setLoadingReadReceiptsByMessageId] = useState({});
  const [uploadProgressByConversation, setUploadProgressByConversation] = useState({});
  const [chatActionError, setChatActionError] = useState("");
  const [typingByConversation, setTypingByConversation] = useState({});
  const [isStartingConversation, setIsStartingConversation] = useState(false);
  const [startConversationError, setStartConversationError] = useState("");
  const [sendingByConversation, setSendingByConversation] = useState({});
  const [realtimeStatus, setRealtimeStatus] = useState("idle");
  const [realtimeError, setRealtimeError] = useState("");
  const activeConversationIdRef = useRef(null);
  const conversationsRef = useRef(conversations);
  const loadConversationsRef = useRef(null);
  const conversationRequestInFlightRef = useRef(false);
  const surfacedErrorKeysRef = useRef(new Set());
  const currentUserRef = useRef(currentUser);
  const deliveredMessageIdsRef = useRef(new Set());
  const pendingMessagesByConversationRef = useRef(new Map());
  const loadedReactionMessageIdsRef = useRef(new Set());
  const realtimeClientRef = useRef(null);
  const realtimeEventHandlerRef = useRef(null);
  const typingTimeoutsRef = useRef(new Map());
  currentUserRef.current = currentUser;
  conversationsRef.current = conversations;

  useEffect(() => {
    const entries = [
      ["auth", authMessage],
      ["conversations", conversationError],
      ["action", chatActionError],
      ["start", startConversationError],
      ["search", userSearchError],
      ["realtime", realtimeError],
      ...Object.entries(messageErrorByConversation).map(([id, message]) => [`message:${id}`, message]),
    ].filter(([, message]) => Boolean(message));
    const currentKeys = new Set(entries.map(([scope, message]) => `${scope}:${message}`));

    entries.forEach(([scope, message]) => {
      const key = `${scope}:${message}`;
      if (!surfacedErrorKeysRef.current.has(key)) toast.error(message);
    });
    surfacedErrorKeysRef.current = currentKeys;
  }, [authMessage, chatActionError, conversationError, messageErrorByConversation, realtimeError, startConversationError, toast, userSearchError]);

  const isLoadingSelectedConversation = (conversationId) =>
    Boolean(conversationId && loadingMessagesByConversation[conversationId]);
  const isLoadingOlderMessages = (conversationId) =>
    Boolean(conversationId && loadingOlderMessagesByConversation[conversationId]);
  const getMessageError = (conversationId) =>
    (conversationId && messageErrorByConversation[conversationId]) || "";
  const getMessagePaging = useCallback(
    (conversationId) => (conversationId && messagePagingByConversation[conversationId]) || null,
    [messagePagingByConversation],
  );
  const hasMoreMessages = (conversationId) => Boolean(getMessagePaging(conversationId)?.hasMore);
  const isSendingMessage = (conversationId) =>
    Boolean(conversationId && sendingByConversation[conversationId]);

  const conversationSummaries = useMemo(() => {
    return conversations
      .map((conversation) => {
        const otherParticipant = contacts.find(
          (contact) => isSameId(contact.id, conversation.otherParticipantId),
        ) ?? conversation.otherParticipant;
          const lastMessage = conversation.messages.at(-1) ?? conversation.lastMessage ?? null;

        return {
          ...conversation,
          otherParticipant,
          lastMessage: lastMessage
            ? {
                id: lastMessage.id,
                senderId: lastMessage.senderId,
                contentPreview: getMessagePreview(lastMessage),
                status: lastMessage.status,
                createdAt: lastMessage.createdAt,
                deletedAt: lastMessage.deletedAt,
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

  function mergeNormalizedConversationContacts(conversation) {
    setContacts((previous) => mergeContacts(previous, getNormalizedConversationContacts(conversation)));
  }

  function applyMessageResponseToConversation(messageResponse) {
    const message = normalizeMessage(messageResponse);
    setConversations((previous) => applyMessageResponseToList(previous, message));

    updatePinnedMessageDetail(message);

    if (message.deletedAt) {
      setMessagePinned(message.conversationId, message.id, false);
      removePinnedMessageDetail(message.conversationId, message.id);
    }
  }

 const messageDomain = createMessageDomain({
    conversations,
    reactionsByMessageId,
    readReceiptsByMessageId,
    pinnedMessageIdsByConversation,
    setConversations,
    setPinnedMessageIdsByConversation,
    setPinnedMessagesByConversation,
    setReactionsByMessageId,
    setReadReceiptsByMessageId,
    setLoadingReadReceiptsByMessageId,
    setSendingByConversation,
    setUploadProgressByConversation,
    setChatActionError,
    currentUserRef,
    loadedReactionMessageIdsRef,
    applyMessageResponseToConversation,
  });
  const {
    replaceConversationPins,
    setMessagePinned,
    setPinnedMessageDetail,
    removePinnedMessageDetail,
    updatePinnedMessageDetail,
    loadMessageReactions,
    loadMessageReadReceipts,
    markConversationRead,
    sendMessage,
    editMessage,
    deleteMessage,
    toggleMessagePin,
    toggleMessageReaction,
  } = messageDomain;
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

  const sessionDomain = useMemo(() => createSessionDomain({
    authSession,
    setAuthSession,
    setCurrentUser,
    setAuthStatus,
    setAuthMessage,
    resetChatState: () => {
      setConversations([]);
      setConversationPaging(null);
      setHasLoadedConversations(false);
      setContacts([]);
      setUserSearchResults([]);
      setMessagePagingByConversation({});
      setPinnedMessageIdsByConversation({});
      setPinnedMessagesByConversation({});
      setReactionsByMessageId({});
      setTypingByConversation({});
      activeConversationIdRef.current = null;
      deliveredMessageIdsRef.current.clear();
      pendingMessagesByConversationRef.current.clear();
      loadedReactionMessageIdsRef.current.clear();
      clearAllTypingTimeouts();
    },
  }), [authSession]);
  const {
    applyAuthenticatedSession,
    clearAuthenticatedSession,
    setAuthenticatedSession,
    setAuthenticatedUser,
    signOut,
  } = sessionDomain;

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
        if (!cancelled) setAuthStatus("unauthenticated");
        return;
      }

      try {
        setAuthStatus("checking");
        const user = await getMe();
        if (cancelled) return;

        const latestAuthSession = getAuthSession() ?? storedAuthSession;
        applyAuthenticatedSession(
          { ...latestAuthSession, user },
          isPersistentSession(),
        );
      } catch (error) {
        if (!cancelled) {
          clearAuthenticatedSession(error.message || SESSION_EXPIRED_MESSAGE);
        }
      }
    }

    verifyStoredSession();

    return () => {
      cancelled = true;
    };
  }, [applyAuthenticatedSession, clearAuthenticatedSession]);

  const conversationDomain = useMemo(() => createConversationDomain({
    conversations,
    conversationPaging,
    contacts,
    setContacts,
    setConversations,
    setConversationPaging,
    setConversationError,
    setHasLoadedConversations,
    setIsLoadingConversations,
    setIsLoadingMoreConversations,
    setLoadingMessagesByConversation,
    setLoadingOlderMessagesByConversation,
    setMessageErrorByConversation,
    setMessagePagingByConversation,
    conversationsRef,
    conversationRequestInFlightRef,
    replaceConversationPins,
    mergeNormalizedConversationContacts,
    getMessagePaging,
  }), [conversations, conversationPaging, contacts, getMessagePaging, replaceConversationPins]);
  const {
    loadConversations,
    loadConversation,
    loadMoreMessages,
    getConversationById: getConversationFromDomain,
  } = conversationDomain;
  loadConversationsRef.current = loadConversations;

  useEffect(() => {
    if (authStatus !== "authenticated") return undefined;

    const load = (options) => loadConversationsRef.current?.(options);
    load();
    const refresh = () => {
      if (document.visibilityState === "visible") load({ silent: true });
    };
    const handleVisibilityChange = () => refresh();
    const intervalId = window.setInterval(refresh, 30000);
    window.addEventListener("focus", refresh);
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener("focus", refresh);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [authStatus]);
  const { handleRealtimeEvent } = createRealtimeDomain({
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
  });

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
  }, [authSession?.accessToken, authStatus]);

  const getConversationById = useCallback(
    (conversationId) => getConversationFromDomain(
      conversationId,
      pinnedMessageIdsByConversation,
      pinnedMessagesByConversation,
    ),
    [getConversationFromDomain, pinnedMessageIdsByConversation, pinnedMessagesByConversation],
  );

 const conversationActions = createConversationActionsDomain({
    conversations,
    setConversations,
    setContacts,
    setMessagePagingByConversation,
    setIsStartingConversation,
    setStartConversationError,
    setChatActionError,
    setCurrentUser,
    setAuthSession,
    setUserSearchResults,
    setIsSearchingUsers,
    setUserSearchError,
    mergeNormalizedConversationContacts,
    loadConversation,
  });
  const {
    startConversation,
    startGroupConversation,
    addMembersToGroup,
    acceptGroupInvitation,
    rejectGroupInvitation,
    updateGroup,
    updateGroupMemberRole,
    removeMemberFromGroup,
    leaveCurrentGroup,
    updateProfile,
    searchUsers,
    clearUserSearch,
  } = conversationActions;
  const setActiveConversationId = useCallback((conversationId) => {
    activeConversationIdRef.current = conversationId == null ? null : String(conversationId);
  }, []);

  function sendTypingStatus(conversationId, typing) {
    realtimeClientRef.current?.sendTypingStatus(conversationId, typing);
  }

  return {
    authSession,
    authMessage,
    authStatus,
    contacts,
    acceptGroupInvitation,
    addMembersToGroup,
    chatActionError,
    clearUserSearch,
    conversations,
    conversationError,
    conversationPaging,
    conversationSummaries,
    currentUser,
    deleteMessage,
    editMessage,
    getConversationById,
    hasLoadedConversations,
    hasMoreMessages,
    isSearchingUsers,
    isLoadingConversations,
    isLoadingMoreConversations,
    isLoadingOlderMessages,
    isLoadingSelectedConversation,
    isSendingMessage,
    isStartingConversation,
    leaveCurrentGroup,
    loadConversation,
    loadConversations,
    loadMessageReactions,
    loadMessageReadReceipts,
    loadMoreMessages,
    markConversationRead,
    readReceiptsByMessageId,
    loadingReadReceiptsByMessageId,
    realtimeError,
    realtimeStatus,
    sendMessage,
    rejectGroupInvitation,
    removeMemberFromGroup,
    searchUsers,
    startConversation,
    startConversationError,
    startGroupConversation,
    stats,
    isAuthenticated: authStatus === "authenticated" && Boolean(authSession),
    isAuthLoading: authStatus === "checking",
    setActiveConversationId,
    setAuthenticatedSession,
    setAuthenticatedUser,
    signOut,
    sendTypingStatus,
    toggleMessageReaction,
    toggleMessagePin,
    typingByConversation,
    updateGroup,
    updateGroupMemberRole,
    updateProfile,
    getMessageError,
    reactionsByMessageId,
    uploadProgressByConversation,
    userSearchError,
    userSearchResults,
  };
}
