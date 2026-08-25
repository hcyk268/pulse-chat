import { useCallback, useEffect, useRef, useState } from "react";
import {
  addMessageReaction,
  deleteMessage as deleteMessageApi,
  editMessage as editMessageApi,
  getMessageHistory,
  getMessageReactions,
  getMessageReadReceipts,
  markConversationRead,
  pinMessage as pinMessageApi,
  removeMessageReaction,
  sendMessage,
  unpinMessage as unpinMessageApi,
  uploadMessageAttachments,
} from "../../api/chatApi.js";
import { getApiErrorMessage } from "../../api/communityApi.js";
import {
  createClientId,
  normalizeMessage,
  normalizeReactionGroups,
} from "../../domain/chat/normalizers.js";
import { prependMessageHistory } from "../../domain/chat/conversationState.js";
import { useLatestRef } from "../../hooks/useLatestRef.js";
import { useRealtimeTopic } from "../../hooks/useRealtimeTopic.js";
import { publishRealtime } from "../../services/realtimeClient.js";
import { isSameId } from "../../utils/chat.js";

const MESSAGE_PAGE_SIZE = 50;
const TYPING_EXPIRY = 5000;
const USER_EVENTS_DESTINATION = "/user/queue/events";
const USER_ERRORS_DESTINATION = "/user/queue/errors";

function sortMessages(messages) {
  return [...messages].sort(
    (left, right) => new Date(left.createdAt ?? 0) - new Date(right.createdAt ?? 0),
  );
}

function mergeMessage(messages, message) {
  const index = messages.findIndex(
    (item) =>
      (message.id != null && isSameId(item.id, message.id)) ||
      (message.clientMessageId != null &&
        isSameId(item.clientMessageId, message.clientMessageId)),
  );

  const merged =
    index === -1
      ? [...messages, message]
      : messages.map((item, itemIndex) => (itemIndex === index ? message : item));

  return merged.map((item) =>
    isSameId(item.replyTo?.id, message.id)
      ? {
          ...item,
          replyTo: {
            ...item.replyTo,
            content: message.content,
            editedAt: message.editedAt,
            deletedAt: message.deletedAt,
          },
        }
      : item,
  );
}

function updateMessageStatus(messages, data, currentUserId) {
  const messageId = data?.messageId;
  const cutoffIndex = messages.findIndex((message) => isSameId(message.id, messageId));
  if (messageId == null || cutoffIndex === -1) return messages;

  if (data.status === "DELIVERED") {
    return messages.map((message, index) =>
      index <= cutoffIndex &&
      isSameId(message.senderId, currentUserId) &&
      message.status !== "READ"
        ? {
            ...message,
            status: "DELIVERED",
            deliveredAt: data.deliveredAt ?? message.deliveredAt,
          }
        : message,
    );
  }

  return messages.map((message) =>
    isSameId(message.id, messageId)
      ? {
          ...message,
          status: data.status ?? message.status,
          deliveredAt: data.deliveredAt ?? message.deliveredAt,
          readAt: data.readAt ?? message.readAt,
        }
      : message,
  );
}

function applyReadReceipt(messages, data) {
  const cutoffIndex = messages.findIndex((message) =>
    isSameId(message.id, data?.lastReadMessageId),
  );
  if (cutoffIndex === -1 || data?.readerId == null) return messages;

  return messages.map((message, index) =>
    index <= cutoffIndex && !isSameId(message.senderId, data.readerId)
      ? {
          ...message,
          status: "READ",
          readAt: message.readAt ?? data.readAt,
        }
      : message,
  );
}

function lastIncomingMessageId(messages, currentUserId) {
  for (let index = messages.length - 1; index >= 0; index -= 1) {
    const message = messages[index];
    if (message.id != null && !isSameId(message.senderId, currentUserId)) {
      return message.id;
    }
  }
  return null;
}

export default function useCommunityChannelChat({
  conversationId,
  currentUser,
  onOtherConversationMessage,
  onPresenceUpdated,
  t,
}) {
  const currentUserId = currentUser?.id ?? null;
  const [messages, setMessages] = useState([]);
  const [paging, setPaging] = useState({});
  const [typingUsers, setTypingUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");
  const [realtimeRevision, setRealtimeRevision] = useState(0);

  const conversationIdRef = useLatestRef(conversationId);
  const currentUserIdRef = useLatestRef(currentUserId);
  const messagesRef = useLatestRef(messages);
  const onOtherConversationMessageRef = useLatestRef(onOtherConversationMessage);
  const onPresenceUpdatedRef = useLatestRef(onPresenceUpdated);
  const typingTimersRef = useRef(new Map());
  const readCursorRef = useRef(new Map());
  const lostRealtimeConnectionRef = useRef(false);

  useEffect(() => {
    if (!conversationId) {
      setMessages([]);
      setPaging({});
      setTypingUsers([]);
      return undefined;
    }

    let active = true;
    setLoading(true);
    setError("");

    getMessageHistory({ conversationId, limit: MESSAGE_PAGE_SIZE })
      .then((history) => {
        if (!active) return;
        setMessages(sortMessages((history?.items ?? []).map(normalizeMessage)));
        setPaging({
          nextCursor: history?.paging?.nextCursor ?? null,
          hasMore: Boolean(history?.paging?.hasMore),
        });
      })
      .catch((requestError) => {
        if (active) setError(getApiErrorMessage(requestError, t, "errors.messages"));
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [conversationId, realtimeRevision, t]);

  const clearTypingUser = useCallback((userId) => {
    setTypingUsers((current) => current.filter((user) => !isSameId(user.userId, userId)));
  }, []);

  const applyTypingEvent = useCallback(
    (data) => {
      if (data?.userId == null || isSameId(data.userId, currentUserIdRef.current)) return;

      const timerKey = String(data.userId);
      clearTimeout(typingTimersRef.current.get(timerKey));
      typingTimersRef.current.delete(timerKey);

      if (!data.typing) {
        clearTypingUser(data.userId);
        return;
      }

      setTypingUsers((current) =>
        current.some((user) => isSameId(user.userId, data.userId))
          ? current
          : [...current, { userId: data.userId, username: data.username }],
      );
      typingTimersRef.current.set(
        timerKey,
        setTimeout(() => {
          typingTimersRef.current.delete(timerKey);
          clearTypingUser(data.userId);
        }, TYPING_EXPIRY),
      );
    },
    [clearTypingUser, currentUserIdRef],
  );

  useEffect(
    () => () => {
      typingTimersRef.current.forEach((timer) => clearTimeout(timer));
      typingTimersRef.current.clear();
    },
    [],
  );

  const handleRealtimeEvent = useCallback(
    (envelope) => {
      const { eventType, data } = envelope ?? {};
      if (eventType === "presence.updated") {
        onPresenceUpdatedRef.current?.(data);
        return;
      }


      const eventConversationId =
        envelope?.conversationId ?? data?.conversationId ?? data?.message?.conversationId;
      const activeConversationId = conversationIdRef.current;
      const isActiveConversation = isSameId(eventConversationId, activeConversationId);

      if (eventType === "message.created" && data?.message) {
        const message = normalizeMessage(data.message);
        if (
          currentUserIdRef.current != null &&
          !isSameId(message.senderId, currentUserIdRef.current)
        ) {
          publishRealtime(
            `/app/messages/${message.id}/delivered`,
            {},
            { queueIfDisconnected: true },
          );
        }

        if (!isActiveConversation) {
          onOtherConversationMessageRef.current?.(message.conversationId);
          return;
        }

        setMessages((current) => sortMessages(mergeMessage(current, message)));
        applyTypingEvent({ userId: message.senderId, typing: false });
        return;
      }

      if (!isActiveConversation) return;

      switch (eventType) {
        case "message.updated":
        case "message.deleted":
          if (data?.message) {
            setMessages((current) => mergeMessage(current, normalizeMessage(data.message)));
          }
          break;
        case "message.status.updated":
          setMessages((current) =>
            updateMessageStatus(current, data, currentUserIdRef.current),
          );
          break;
        case "message.reaction.updated":
          if (data?.messageId != null) {
            getMessageReactions(data.messageId)
              .then(normalizeReactionGroups)
              .then((reactions) => {
                setMessages((current) =>
                  current.map((message) =>
                    isSameId(message.id, data.messageId)
                      ? { ...message, reactions }
                      : message,
                  ),
                );
              })
              .catch(() => {});
          }
          break;
        case "message.read":
          setMessages((current) => applyReadReceipt(current, data));
          break;
        case "message.pinned":
          if (data?.pin?.message?.id != null) {
            setMessages((current) =>
              current.map((message) =>
                isSameId(message.id, data.pin.message.id)
                  ? { ...message, pinned: true, pin: data.pin }
                  : message,
              ),
            );
          }
          break;
        case "message.unpinned":
          setMessages((current) =>
            current.map((message) =>
              isSameId(message.id, data?.messageId)
                ? { ...message, pinned: false, pin: null }
                : message,
            ),
          );
          break;
        case "typing.updated":
          applyTypingEvent(data);
          break;
        default:
          break;
      }
    },
    [
      applyTypingEvent,
      conversationIdRef,
      currentUserIdRef,
      onOtherConversationMessageRef,
      onPresenceUpdatedRef,
    ],
  );

  const handleRealtimeError = useCallback(
    (payload) => {
      const serverMessage = payload?.fieldErrors?.[0]?.message ?? payload?.message;
      const translated = serverMessage ? t(serverMessage) : "";
      setError(translated && translated !== serverMessage ? translated : t("errors.realtime"));
    },
    [t],
  );

  useRealtimeTopic(currentUserId ? USER_ERRORS_DESTINATION : null, handleRealtimeError);
  const authenticatedRealtimeStatus = useRealtimeTopic(
    currentUserId ? USER_EVENTS_DESTINATION : null,
    handleRealtimeEvent,
  );
  const publicRealtimeStatus = useRealtimeTopic(
    !currentUserId && conversationId
      ? `/topic/community/conversations/${conversationId}`
      : null,
    handleRealtimeEvent,
  );
  const realtimeStatus = currentUserId ? authenticatedRealtimeStatus : publicRealtimeStatus;

  useEffect(() => {
    if (realtimeStatus === "disconnected" || realtimeStatus === "error") {
      lostRealtimeConnectionRef.current = true;
      return;
    }
    if (realtimeStatus !== "connected" || !lostRealtimeConnectionRef.current) return;

    lostRealtimeConnectionRef.current = false;
    setRealtimeRevision((current) => current + 1);
  }, [realtimeStatus]);

  useEffect(() => {
    if (!conversationId || currentUserId == null || loading) return;

    const lastMessageId = lastIncomingMessageId(messages, currentUserId);
    const cursorKey = String(conversationId);
    if (
      lastMessageId == null ||
      isSameId(readCursorRef.current.get(cursorKey), lastMessageId)
    ) {
      return;
    }

    readCursorRef.current.set(cursorKey, lastMessageId);
    markConversationRead({ conversationId, lastReadMessageId: lastMessageId }).catch(() => {
      readCursorRef.current.delete(cursorKey);
    });
  }, [conversationId, currentUserId, loading, messages]);

  const loadOlderMessages = useCallback(async () => {
    const activeConversationId = conversationIdRef.current;
    if (
      !activeConversationId ||
      !paging?.hasMore ||
      !paging.nextCursor ||
      loadingOlder
    ) {
      return;
    }

    setLoadingOlder(true);
    try {
      const history = await getMessageHistory({
        conversationId: activeConversationId,
        limit: MESSAGE_PAGE_SIZE,
        cursor: paging.nextCursor,
      });
      const olderMessages = sortMessages((history?.items ?? []).map(normalizeMessage));
      setMessages((current) => prependMessageHistory(current, olderMessages));
      setPaging({
        nextCursor: history?.paging?.nextCursor ?? null,
        hasMore: Boolean(history?.paging?.hasMore),
      });
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, t, "errors.olderMessages"));
    } finally {
      setLoadingOlder(false);
    }
  }, [conversationIdRef, loadingOlder, paging, t]);

  const sendChannelMessage = useCallback(
    async (content, files = []) => {
      const activeConversationId = conversationIdRef.current;
      const trimmedContent = content.trim();
      const selectedFiles = Array.from(files).filter((file) => file.size > 0);
      const hasAttachments = selectedFiles.length > 0;
      if (!activeConversationId || (!trimmedContent && !hasAttachments)) return false;

      const clientMessageId = createClientId();
      const messageType = hasAttachments ? "MEDIA" : "TEXT";
      const optimisticMessage = {
        id: null,
        clientMessageId,
        conversationId: activeConversationId,
        senderId: currentUserIdRef.current,
        sender: currentUser,
        content: trimmedContent,
        attachments: selectedFiles.map((file) => ({
          fileName: file.name,
          contentType: file.type || "application/octet-stream",
          sizeBytes: file.size,
        })),
        messageType,
        status: "PENDING",
        createdAt: new Date().toISOString(),
        pending: true,
      };

      setSending(true);
      setError("");
      setMessages((current) => [...current, optimisticMessage]);

      try {
        const uploadedAssets = hasAttachments
          ? await uploadMessageAttachments(selectedFiles, {
              purpose: "COMMUNITY_ATTACHMENT",
            })
          : [];
        const response = await sendMessage({
          conversationId: activeConversationId,
          clientMessageId,
          content: trimmedContent,
          messageType,
          replyToMessageId: null,
          attachments: uploadedAssets.map((asset) => ({ assetId: asset.id })),
        });
        setMessages((current) => mergeMessage(current, normalizeMessage(response)));
        return true;
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, t, "errors.sendMessage"));
        setMessages((current) =>
          current.map((message) =>
            isSameId(message.clientMessageId, clientMessageId)
              ? { ...message, pending: false, failed: true, status: "FAILED" }
              : message,
          ),
        );
        return false;
      } finally {
        setSending(false);
      }
    },
    [conversationIdRef, currentUser, currentUserIdRef, t],
  );

  const notifyTyping = useCallback(
    (typing) => {
      const activeConversationId = conversationIdRef.current;
      if (activeConversationId) {
        publishRealtime(`/app/conversations/${activeConversationId}/typing`, { typing });
      }
    },
    [conversationIdRef],
  );

  const toggleReaction = useCallback(
    async (messageId, emoji) => {
      const message = messagesRef.current.find((item) => isSameId(item.id, messageId));
      const reaction = message?.reactions?.find((item) => item.emoji === emoji);
      try {
        if (reaction?.reactedByMe) {
          await removeMessageReaction(messageId, emoji);
        } else {
          await addMessageReaction(messageId, emoji);
        }
        const reactions = normalizeReactionGroups(await getMessageReactions(messageId));
        setMessages((current) =>
          current.map((item) => (isSameId(item.id, messageId) ? { ...item, reactions } : item)),
        );
        return reactions;
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, t, "errors.messageReaction"));
        return null;
      }
    },
    [messagesRef, t],
  );

  const editMessage = useCallback(
    async (messageId, newContent) => {
      const message = messagesRef.current.find((item) => isSameId(item.id, messageId));
      try {
        const response = await editMessageApi(messageId, {
          newContent,
          type: message?.messageType,
        });
        setMessages((current) => mergeMessage(current, normalizeMessage(response)));
        return response;
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, t, "errors.messageEdit"));
        return null;
      }
    },
    [messagesRef, t],
  );

  const deleteMessage = useCallback(
    async (messageId) => {
      try {
        const response = await deleteMessageApi(messageId);
        setMessages((current) => mergeMessage(current, normalizeMessage(response)));
        return response;
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, t, "errors.messageDelete"));
        return null;
      }
    },
    [t],
  );

  const pinMessage = useCallback(
    async (messageId) => {
      try {
        const pin = await pinMessageApi(messageId);
        setMessages((current) =>
          current.map((message) =>
            isSameId(message.id, messageId) ? { ...message, pinned: true, pin } : message,
          ),
        );
        return pin;
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, t, "errors.messagePin"));
        return null;
      }
    },
    [t],
  );

  const unpinMessage = useCallback(
    async (messageId) => {
      try {
        await unpinMessageApi(messageId);
        setMessages((current) =>
          current.map((message) =>
            isSameId(message.id, messageId)
              ? { ...message, pinned: false, pin: null }
              : message,
          ),
        );
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, t, "errors.messagePin"));
      }
    },
    [t],
  );

  const loadReadReceipts = useCallback(
    async (messageId) => {
      try {
        const receipts = (await getMessageReadReceipts(messageId))?.items ?? [];
        setMessages((current) =>
          current.map((message) =>
            isSameId(message.id, messageId)
              ? { ...message, readReceipts: receipts }
              : message,
          ),
        );
        return receipts;
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, t, "errors.messageReads"));
        return null;
      }
    },
    [t],
  );

  return {
    deleteMessage,
    dismissError: () => setError(""),
    editMessage,
    error,
    hasOlderMessages: Boolean(paging.hasMore),
    loadOlderMessages,
    loadReadReceipts,
    loading,
    loadingOlder,
    messages,
    notifyTyping,
    pinMessage,
    realtimeStatus,
    sendChannelMessage,
    sending,
    toggleReaction,
    typingUsers,
    unpinMessage,
  };
}
