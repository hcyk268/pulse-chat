import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  clearAuthSession,
  getAuthSession,
  hasValidAuthSession,
  isPersistentSession,
  saveAuthSession,
} from "../utils/authStorage";
import { setAuthEventHandlers } from "../services/apiClient";
import { logout as logoutApi } from "../services/authApi";
import {
  acceptGroupInvitation as acceptGroupInvitationApi,
  addGroupMembers as addGroupMembersApi,
  createDirectConversation,
  createGroupConversation as createGroupConversationApi,
  getConversation,
  leaveGroup as leaveGroupApi,
  listConversationPins,
  listConversations,
  rejectGroupInvitation as rejectGroupInvitationApi,
  removeGroupMember as removeGroupMemberApi,
  updateGroupMemberRole as updateGroupMemberRoleApi,
  updateGroupProfile as updateGroupProfileApi,
} from "../services/conversationApi";
import {
  deleteMessage as deleteMessageApi,
  editMessage as editMessageApi,
  getMessageReactions,
  getMessageReadReceipts,
  listMessages,
  markMessagesRead,
  pinMessage as pinMessageApi,
  reactToMessage,
  removeMessageReaction,
  sendMessage as sendMessageApi,
  unpinMessage as unpinMessageApi,
} from "../services/messageApi";
import { createRealtimeClient } from "../services/realtimeClient";
import { uploadMessageAttachment } from "../services/uploadApi";
import { getMe, searchUsers as searchUsersApi, updateMe } from "../services/userApi";

const SESSION_EXPIRED_MESSAGE = "Your session has expired. Please sign in again.";
const DEFAULT_USER_ACCENT = "from-cyan-300 to-emerald-400";
const EMPTY_CURRENT_USER = {
  id: null,
  backendId: null,
  username: "",
  email: "",
  displayName: "You",
  avatarUrl: null,
  bio: "",
  accountStatus: null,
  accent: DEFAULT_USER_ACCENT,
  createdAt: null,
  updatedAt: null,
};

function createClientId() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }

  return "10000000-1000-4000-8000-100000000000".replace(/[018]/g, (char) =>
    (Number(char) ^ ((Math.random() * 16) >> (Number(char) / 4))).toString(16),
  );
}

function toCurrentUser(user) {
  const userId = user?.id ?? null;

  return {
    ...EMPTY_CURRENT_USER,
    ...user,
    id: userId,
    backendId: userId,
    displayName: user?.displayName || user?.username || EMPTY_CURRENT_USER.displayName,
    bio: user?.bio ?? "",
    accent: user?.accent ?? DEFAULT_USER_ACCENT,
  };
}

function toContact(user) {
  if (!user) return null;

  const userId = user.id ?? user.userId ?? null;

  return {
    id: userId,
    backendId: userId,
    username: user.username ?? "",
    email: user.email ?? "",
    displayName: user.displayName || user.username || "Unknown user",
    avatarUrl: user.avatarUrl ?? null,
    role: user.role ?? (user.directConversationId ? "Existing direct chat" : "Active user"),
    bio: user.bio ?? "",
    accent: user.accent ?? "from-sky-300 to-blue-500",
    presence: user.presence ?? { isOnline: false, lastActiveAt: null },
    directConversationId: user.directConversationId ?? null,
  };
}

function toMemberContact(member) {
  const contact = toContact(member);
  if (!contact) return null;

  return {
    ...contact,
    role: member.role ?? contact.role,
    joinedAt: member.joinedAt ?? null,
    leftAt: member.leftAt ?? null,
    status: member.status ?? null,
  };
}

function toGroupDisplayContact(conversation) {
  return {
    id: `group-${conversation.id}`,
    backendId: null,
    username: "group",
    email: "",
    displayName: conversation.title || conversation.name || "Group chat",
    avatarUrl: conversation.avatarUrl ?? null,
    role: `${conversation.participantCount ?? conversation.participants?.length ?? 0} members`,
    bio: "",
    accent: "from-amber-300 to-rose-500",
    presence: { isOnline: false, lastActiveAt: null },
    directConversationId: null,
  };
}

function getConversationDisplayContact(conversation) {
  if (conversation.type === "GROUP" || conversation.peer === null) {
    return toGroupDisplayContact(conversation);
  }

  return toContact(conversation.otherParticipant ?? conversation.peer) ?? toGroupDisplayContact(conversation);
}

function getConversationContacts(conversation) {
  const participants = (conversation.participants ?? [])
    .map(toMemberContact)
    .filter(Boolean);
  const peer = toContact(conversation.otherParticipant ?? conversation.peer);

  return peer ? [peer, ...participants] : participants;
}

function mergeContacts(previousContacts, nextContacts) {
  const byId = new Map(previousContacts.map((contact) => [String(contact.id), contact]));

  nextContacts.filter(Boolean).forEach((contact) => {
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
    replyTo: message.replyTo ?? null,
    attachments: message.attachments ?? [],
    messageType: message.messageType,
    status: message.status,
    createdAt: message.createdAt,
    editedAt: message.editedAt,
    deletedBy: message.deletedBy,
    deletedAt: message.deletedAt,
    deliveredAt: message.deliveredAt,
    readAt: message.readAt,
  };
}

function normalizeLastMessage(lastMessage) {
  if (!lastMessage) return null;

  return {
    ...lastMessage,
    senderId: getMessageSenderId(lastMessage),
    contentPreview: lastMessage.deletedAt ? "Message deleted" : lastMessage.contentPreview,
  };
}

function dedupeById(items) {
  return Array.from(new Map(items.map((item) => [String(item.id), item])).values());
}

function getMessageIndex(messages, messageId) {
  return messages.findIndex((message) => isSameId(message.id, messageId));
}

function getMessagePreview(message) {
  if (!message) return "";
  if (message.deletedAt) return "Message deleted";
  if (message.content ?? message.contentPreview) return message.content ?? message.contentPreview;

  const attachmentCount = message.attachments?.length ?? 0;
  if (attachmentCount === 1) return message.attachments[0]?.fileName || "Attachment";
  if (attachmentCount > 1) return `${attachmentCount} attachments`;

  return "";
}

function getPinnedMessageIds(pinResponse) {
  return (pinResponse?.items ?? [])
    .map((pin) => pin?.message?.id)
    .filter((messageId) => messageId != null)
    .map(String);
}

function normalizePin(pin) {
  return {
    pinId: pin.pinId,
    message: normalizeMessage(pin.message),
    pinnedBy: pin.pinnedBy,
    pinnedAt: pin.pinnedAt,
  };
}

function normalizeReactionGroups(response) {
  return (response?.items ?? []).map((group) => ({
    emoji: group.emoji,
    count: group.count ?? 0,
    reactedByMe: Boolean(group.reactedByMe),
    users: group.users ?? [],
  }));
}

export function useChatApi() {
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
  const currentUserRef = useRef(currentUser);
  const deliveredMessageIdsRef = useRef(new Set());
  const pendingMessagesByConversationRef = useRef(new Map());
  const loadedReactionMessageIdsRef = useRef(new Set());
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

  function normalizeConversation(conversation, existingMessages = []) {
  const displayContact = getConversationDisplayContact(conversation);
    const participantContacts = getConversationContacts(conversation);
    const lastMessage = normalizeLastMessage(conversation.lastMessage);
    const isGroup = conversation.type === "GROUP";

  return {
    id: conversation.id,
    type: conversation.type,
    title: isGroup ? displayContact.displayName : displayContact.displayName,
    avatarUrl: isGroup ? conversation.avatarUrl ?? null : displayContact.avatarUrl,
    otherParticipantId: isGroup ? null : displayContact.id,
    otherParticipant: displayContact,
    participants: participantContacts,
    participantCount: conversation.participantCount ?? participantContacts.length,
    currentUserRole: conversation.currentUserRole ?? null,
    currentUserStatus: conversation.currentUserStatus ?? conversation.status ?? null,
    isPendingInvitation: Boolean(conversation.isPendingInvitation),
    createdBy: conversation.createdBy ?? null,
    unreadCount: conversation.unreadCount ?? 0,
    pinned: false,
    muted: false,
    lastMessage,
    lastMessageAt: conversation.lastMessageAt ?? lastMessage?.createdAt ?? conversation.updatedAt ?? conversation.createdAt,
    createdAt: conversation.createdAt,
    updatedAt: conversation.updatedAt,
    messages: existingMessages,
  };
}

  function getNormalizedConversationContacts(conversation) {
    return conversation.participants.length
      ? conversation.participants
      : [conversation.otherParticipant];
  }

  function mergeNormalizedConversationContacts(conversation) {
    setContacts((previous) => mergeContacts(previous, getNormalizedConversationContacts(conversation)));
  }

  async function runChatAction(fallbackMessage, action, fallbackValue = null) {
    setChatActionError("");

    try {
      return await action();
    } catch (error) {
      setChatActionError(error.message || fallbackMessage);
      return fallbackValue;
    }
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

  function applyMessageResponseToConversation(messageResponse) {
    const message = normalizeMessage(messageResponse);
      const lastMessage = {
      id: message.id,
      senderId: message.senderId,
      contentPreview: getMessagePreview(message),
      status: message.status,
      createdAt: message.createdAt,
      deletedAt: message.deletedAt,
    };

    setConversations((previous) =>
      previous.map((conversation) => {
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

        return {
          ...conversation,
          messages: hasMessage ? nextMessages : dedupeById([...nextMessages, message]),
          lastMessage: isSameId(conversation.lastMessage?.id, message.id)
            ? lastMessage
            : conversation.lastMessage,
          lastMessageAt: isSameId(conversation.lastMessage?.id, message.id)
            ? message.createdAt
            : conversation.lastMessageAt,
        };
      }),
    );

    updatePinnedMessageDetail(message);

    if (message.deletedAt) {
      setMessagePinned(message.conversationId, message.id, false);
      removePinnedMessageDetail(message.conversationId, message.id);
    }
  }

  function replaceConversationPins(conversationId, pinResponse) {
    if (!conversationId) return;

    setPinnedMessageIdsByConversation((previous) => ({
      ...previous,
      [String(conversationId)]: getPinnedMessageIds(pinResponse),
    }));
    setPinnedMessagesByConversation((previous) => ({
      ...previous,
      [String(conversationId)]: (pinResponse?.items ?? []).map(normalizePin),
    }));
  }

  function setMessagePinned(conversationId, messageId, pinned) {
    if (!conversationId || !messageId) return;

    const key = String(conversationId);
    const messageKey = String(messageId);

    setPinnedMessageIdsByConversation((previous) => {
      const ids = new Set(previous[key] ?? []);

      if (pinned) {
        ids.add(messageKey);
      } else {
        ids.delete(messageKey);
      }

      return {
        ...previous,
        [key]: Array.from(ids),
      };
    });
  }

  function setPinnedMessageDetail(conversationId, pinResponse) {
    if (!conversationId || !pinResponse?.message?.id) return;

    const key = String(conversationId);
    const messageKey = String(pinResponse.message.id);
    const normalizedPin = normalizePin(pinResponse);

    setPinnedMessagesByConversation((previous) => {
      const nextPins = (previous[key] ?? []).filter(
        (pin) => !isSameId(pin.message?.id, messageKey),
      );

      return {
        ...previous,
        [key]: [normalizedPin, ...nextPins],
      };
    });
  }

  function removePinnedMessageDetail(conversationId, messageId) {
    if (!conversationId || !messageId) return;

    const key = String(conversationId);

    setPinnedMessagesByConversation((previous) => ({
      ...previous,
      [key]: (previous[key] ?? []).filter((pin) => !isSameId(pin.message?.id, messageId)),
    }));
  }

  function updatePinnedMessageDetail(message) {
    if (!message?.conversationId || !message.id) return;

    const key = String(message.conversationId);

    setPinnedMessagesByConversation((previous) => ({
      ...previous,
      [key]: (previous[key] ?? []).map((pin) =>
        isSameId(pin.message?.id, message.id)
          ? {
              ...pin,
              message: {
                ...pin.message,
                ...message,
              },
            }
          : pin,
      ),
    }));
  }

  function setMessageReactions(messageId, response) {
    if (!messageId) return;

    setReactionsByMessageId((previous) => ({
      ...previous,
      [String(messageId)]: normalizeReactionGroups(response),
    }));
  }

  async function loadMessageReactions(messageId, { force = false } = {}) {
    if (!messageId) return [];

    const key = String(messageId);
    if (!force && loadedReactionMessageIdsRef.current.has(key)) {
      return reactionsByMessageId[key] ?? [];
    }

    loadedReactionMessageIdsRef.current.add(key);

    try {
      const response = await getMessageReactions(messageId);
      setMessageReactions(messageId, response);
      return normalizeReactionGroups(response);
    } catch {
      loadedReactionMessageIdsRef.current.delete(key);
      return [];
    }
  }


  async function loadMessageReadReceipts(messageId, { force = false } = {}) {
    if (!messageId) return [];

    const key = String(messageId);
    if (!force && readReceiptsByMessageId[key]) {
      return readReceiptsByMessageId[key];
    }

    setLoadingReadReceiptsByMessageId((previous) => ({ ...previous, [key]: true }));

    try {
      const response = await getMessageReadReceipts(messageId);
      const receipts = response?.items ?? [];
      setReadReceiptsByMessageId((previous) => ({
        ...previous,
        [key]: receipts,
      }));
      return receipts;
    } catch (error) {
      setChatActionError(error.message || "Could not load read receipts.");
      return [];
    } finally {
      setLoadingReadReceiptsByMessageId((previous) => ({ ...previous, [key]: false }));
    }
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
    setCurrentUser(EMPTY_CURRENT_USER);
    setConversations([]);
    setConversationPaging(null);
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
      const nextContacts = normalized.flatMap(getNormalizedConversationContacts);

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
      const [conversationResponse, messageResponse, pinsResponse] = await Promise.all([
        getConversation(conversationId),
        listMessages({ conversationId, limit: 20 }),
        listConversationPins(conversationId),
      ]);
      const messages = (messageResponse.items ?? []).map((message) =>
        normalizeMessage(message),
      );
      const normalizedConversation = normalizeConversation(conversationResponse, messages);

      mergeNormalizedConversationContacts(normalizedConversation);
      setConversations((previous) => mergeConversationList(previous, [normalizedConversation]));
      replaceConversationPins(conversationId, pinsResponse);
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
      contentPreview: getMessagePreview(message),
      status: message.status,
      createdAt: message.createdAt,
      deletedAt: message.deletedAt,
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
      sendMessageDelivered(message);
      setConversationTyping(message.conversationId, false);

      if (isSameId(activeConversationIdRef.current, message.conversationId)) {
        markConversationRead(message.conversationId, message.id);
      }
    }
  }

  function applyRealtimeConversation(conversationResponse) {
    setContacts((previous) => mergeContacts(previous, getConversationContacts(conversationResponse)));
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
      previous.map((conversation) => {
        const messages = conversation.messages ?? [];
        const cutoffIndex = getMessageIndex(messages, messageId);
        const isDeliveredEvent = data.status === "DELIVERED";

        const nextMessages =
          isDeliveredEvent && cutoffIndex !== -1
            ? messages.map((message, index) =>
                index <= cutoffIndex &&
                isSameId(message.senderId, currentUserRef.current.id) &&
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
                deliveredAt: data.deliveredAt ?? conversation.lastMessage.deliveredAt,
                readAt: data.readAt ?? conversation.lastMessage.readAt,
              }
            : conversation.lastMessage,
          messages: nextMessages,
        };
      }),
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
      if (conversation) {
        applyRealtimeConversation(conversation);
      }
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
      (contact) => isSameId(contact.id, conversation.otherParticipantId),
    );
    const pinnedMessageIds = new Set(
      pinnedMessageIdsByConversation[String(conversation.id)] ?? [],
    );

    return {
      ...conversation,
      otherParticipant,
      pinnedMessages: pinnedMessagesByConversation[String(conversation.id)] ?? [],
      messages: (conversation.messages ?? []).map((message) => ({
        ...message,
        pinned: pinnedMessageIds.has(String(message.id)),
      })),
    };
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

  async function sendMessage(conversationId, content, { replyToMessageId = null, files = [] } = {}) {
    const trimmed = content.trim();
    const selectedFiles = Array.from(files ?? []);
    if (!trimmed && selectedFiles.length === 0) return null;

    setSendingByConversation((previous) => ({ ...previous, [conversationId]: true }));
    setUploadProgressByConversation((previous) => ({ ...previous, [conversationId]: selectedFiles.length ? 0 : null }));
    setChatActionError("");

    try {
      const attachments = [];

      for (const file of selectedFiles) {
        const uploaded = await uploadMessageAttachment(file, {
          onProgress: (progress) => {
            setUploadProgressByConversation((previous) => ({
              ...previous,
              [conversationId]: progress,
            }));
          },
        });
        attachments.push(uploaded);
      }

      const response = await sendMessageApi({
        conversationId: Number(conversationId),
        clientMessageId: createClientId(),
        content: trimmed || null,
        messageType: attachments.length ? "MEDIA" : "TEXT",
        replyToMessageId,
        attachments,
      });
      const message = normalizeMessage(response);
        const lastMessage = {
        id: message.id,
        senderId: message.senderId,
        contentPreview: getMessagePreview(message),
        status: message.status,
        createdAt: message.createdAt,
        deletedAt: message.deletedAt,
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
      setUploadProgressByConversation((previous) => ({ ...previous, [conversationId]: null }));
    }
  }

  async function editMessage(messageId, content) {
    const trimmed = content.trim();
    if (!messageId || !trimmed) return null;

    setChatActionError("");

    try {
      const response = await editMessageApi(messageId, {
        newContent: trimmed,
        type: "TEXT",
      });
      applyMessageResponseToConversation(response);
      return normalizeMessage(response);
    } catch (error) {
      setChatActionError(error.message || "Could not edit message.");
      return null;
    }
  }

  async function deleteMessage(messageId) {
    if (!messageId) return null;

    setChatActionError("");

    try {
      const response = await deleteMessageApi(messageId);
      applyMessageResponseToConversation(response);
      return normalizeMessage(response);
    } catch (error) {
      setChatActionError(error.message || "Could not delete message.");
      return null;
    }
  }

  async function toggleMessagePin(conversationId, message) {
    const messageId = message?.id;
    if (!conversationId || !messageId) return null;

    const pinnedMessageIds = new Set(
      pinnedMessageIdsByConversation[String(conversationId)] ?? [],
    );
    const isPinned = pinnedMessageIds.has(String(messageId));

    setChatActionError("");

    try {
      if (isPinned) {
        const response = await unpinMessageApi(messageId);
        setMessagePinned(
          response?.conversationId ?? conversationId,
          response?.messageId ?? messageId,
          false,
        );
        removePinnedMessageDetail(response?.conversationId ?? conversationId, response?.messageId ?? messageId);
        return response;
      }

      const response = await pinMessageApi(messageId);
      setMessagePinned(
        response?.message?.conversationId ?? conversationId,
        response?.message?.id ?? messageId,
        true,
      );
      setPinnedMessageDetail(response?.message?.conversationId ?? conversationId, response);
      return response;
    } catch (error) {
      setChatActionError(error.message || "Could not update message pin.");
      return null;
    }
  }

  async function toggleMessageReaction(message, emoji) {
    const messageId = message?.id;
    if (!messageId || !emoji) return null;

    const currentGroups = reactionsByMessageId[String(messageId)] ?? [];
    const reactedByMe = currentGroups.some(
      (group) => group.emoji === emoji && group.reactedByMe,
    );

    setChatActionError("");

    try {
      if (reactedByMe) {
        await removeMessageReaction(messageId, emoji);
      } else {
        await reactToMessage(messageId, emoji);
      }

      loadedReactionMessageIdsRef.current.delete(String(messageId));
      return loadMessageReactions(messageId, { force: true });
    } catch (error) {
      setChatActionError(error.message || "Could not update message reaction.");
      return null;
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

      mergeNormalizedConversationContacts(normalizedConversation);
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


  function applyConversationResponse(conversationResponse, existingMessages = null) {
    const existing = conversations.find((item) => String(item.id) === String(conversationResponse.id));
    const normalizedConversation = normalizeConversation(
      conversationResponse,
      existingMessages ?? existing?.messages ?? [],
    );

    mergeNormalizedConversationContacts(normalizedConversation);
    setConversations((previous) => mergeConversationList(previous, [normalizedConversation]));
    return normalizedConversation;
  }

  async function startGroupConversation({ name, avatarUrl = null, memberIds }) {
    setIsStartingConversation(true);
    setStartConversationError("");

    try {
      const response = await createGroupConversationApi({
        name: name.trim(),
        avatarUrl: avatarUrl?.trim() || null,
        memberIds: memberIds.map(Number),
      });
      const normalizedConversation = applyConversationResponse(response, []);
      setMessagePagingByConversation((previous) => ({
        ...previous,
        [normalizedConversation.id]: null,
      }));
      return normalizedConversation.id;
    } catch (error) {
      setStartConversationError(error.message || "Could not create group.");
      return null;
    } finally {
      setIsStartingConversation(false);
    }
  }

  async function addMembersToGroup(conversationId, memberIds) {
    if (!conversationId || memberIds.length === 0) return null;

    return runChatAction("Could not add group members.", async () => {
      const response = await addGroupMembersApi(conversationId, memberIds.map(Number));
      return applyConversationResponse(response);
    });
  }

  async function acceptGroupInvitation(conversationId) {
    if (!conversationId) return null;

    return runChatAction("Could not accept invitation.", async () => {
      const response = await acceptGroupInvitationApi(conversationId);
      return applyConversationResponse(response);
    });
  }

  async function rejectGroupInvitation(conversationId) {
    if (!conversationId) return false;

    return runChatAction("Could not reject invitation.", async () => {
      await rejectGroupInvitationApi(conversationId);
      setConversations((previous) => previous.filter((conversation) => !isSameId(conversation.id, conversationId)));
      return true;
    }, false);
  }

  async function updateGroup(conversationId, profile) {
    if (!conversationId) return null;

    return runChatAction("Could not update group.", async () => {
      const response = await updateGroupProfileApi(conversationId, {
        name: profile.name?.trim() || null,
        avatarUrl: profile.avatarUrl?.trim() || null,
      });
      return applyConversationResponse(response);
    });
  }

  async function updateGroupMemberRole(conversationId, memberId, role) {
    if (!conversationId || !memberId || !role) return null;

    return runChatAction("Could not update member role.", async () => {
      const response = await updateGroupMemberRoleApi(conversationId, memberId, role);
      return applyConversationResponse(response);
    });
  }

  async function removeMemberFromGroup(conversationId, memberId) {
    if (!conversationId || !memberId) return null;

    return runChatAction("Could not remove member.", async () => {
      const response = await removeGroupMemberApi(conversationId, memberId);
      return applyConversationResponse(response);
    });
  }

  async function leaveCurrentGroup(conversationId) {
    if (!conversationId) return false;

    return runChatAction("Could not leave group.", async () => {
      await leaveGroupApi(conversationId);
      setConversations((previous) => previous.filter((conversation) => !isSameId(conversation.id, conversationId)));
      return true;
    }, false);
  }

  async function updateProfile(nextProfile) {
    const displayName = nextProfile.displayName?.trim();
    const avatarUrl = nextProfile.avatarUrl?.trim();
    const bio = nextProfile.bio?.trim();
    const user = await updateMe({
      displayName: displayName || null,
      // The backend normalizes an empty string to null, which lets the user clear these fields.
      avatarUrl: avatarUrl ?? null,
      bio: bio ?? null,
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
    const refreshToken = authSession?.refreshToken;

    if (refreshToken) {
      logoutApi(refreshToken).catch(() => {
        // Local sign-out must still complete if the backend session is already gone.
      });
    }

    clearAuthenticatedSession(message);
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
