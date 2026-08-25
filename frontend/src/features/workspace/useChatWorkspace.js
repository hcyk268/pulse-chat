import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  acceptGroupInvitation,
  addMessageReaction,
  createDirectConversation,
  createGroupConversation,
  getApiErrorMessage,
  getConversation,
  getConversations,
  deleteMessage as deleteMessageApi,
  editMessage as editMessageApi,
  getMessageHistory,
  getMessageReadReceipts,
  getMessageReactions,
  getPinnedMessages,
  getMyProfile,
  inviteGroupMembers,
  leaveGroup,
  markConversationRead,
  pinMessage as pinMessageApi,
  removeGroupMember,
  removeMessageReaction,
  rejectGroupInvitation,
  sendMessage,
  unpinMessage as unpinMessageApi,
  updateGroupMemberRole,
  updateGroupProfile,
  uploadMessageAttachments,
} from "../../api/chatApi.js";
import {
  applyIncomingMessageToList,
  applyMessagePinToList,
  applyMessageReadReceiptsToList,
  applyMessageReactionsToList,
  applyMessageResponseToList,
  applyMessageStatusToList,
  applyOutgoingMessageToList,
  applyPresenceToList,
  applyReadReceiptToList,
  mergeConversationList,
  prependMessageHistory,
  resetConversationUnread,
  updateConversationMessagesInList,
} from "../../domain/chat/conversationState.js";
import {
  createClientId,
  normalizeConversation,
  normalizeReactionGroups,
  normalizeMessage,
} from "../../domain/chat/normalizers.js";
import { useLatestRef } from "../../hooks/useLatestRef.js";
import { useRealtimeTopic } from "../../hooks/useRealtimeTopic.js";
import { useTranslation } from "../../i18n/useTranslation.js";
import { publishRealtime } from "../../services/realtimeClient.js";
import { useAppDispatch, useAppSelector } from "../../store/hooks";
import { profileLoaded, selectCurrentUser } from "../../store/slices/authSlice";
import { selectActiveConversation, setActiveConversation } from "../../store/slices/workspaceSlice";
import { isSameId } from "../../utils/chat.js";

const CONVERSATION_PAGE_SIZE = 50;
const MESSAGE_PAGE_SIZE = 50;
const TYPING_EXPIRY = 5000;
const USER_EVENTS_DESTINATION = "/user/queue/events";
const USER_ERRORS_DESTINATION = "/user/queue/errors";

function sortMessages(messages) {
  return [...messages].sort(
    (left, right) => new Date(left.createdAt ?? 0) - new Date(right.createdAt ?? 0),
  );
}

function findConversation(conversations, conversationId) {
  return conversations.find((conversation) => isSameId(conversation.id, conversationId)) ?? null;
}

function lastIncomingMessageId(conversation, currentUserId) {
  const messages = conversation?.messages ?? [];

  for (let index = messages.length - 1; index >= 0; index -= 1) {
    const message = messages[index];
    if (message.id != null && !isSameId(message.senderId, currentUserId)) return message.id;
  }

  return null;
}

export function useChatWorkspace() {
  const dispatch = useAppDispatch();
  const { t } = useTranslation();
  const activeConversationId = useAppSelector(selectActiveConversation);
  const currentUser = useAppSelector(selectCurrentUser);
  const currentUserId = currentUser?.id ?? null;

  const [conversations, setConversations] = useState([]);
  const [loadingConversations, setLoadingConversations] = useState(true);
  const [conversationPaging, setConversationPaging] = useState({});
  const [loadingOlderConversations, setLoadingOlderConversations] = useState(false);
  const [realtimeRevision, setRealtimeRevision] = useState(0);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [sending, setSending] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(null);
  const [error, setError] = useState("");
  const [pagingByConversation, setPagingByConversation] = useState({});
  const [typingByConversation, setTypingByConversation] = useState({});

  const activeIdRef = useLatestRef(activeConversationId);
  const conversationsRef = useLatestRef(conversations);
  const pagingRef = useLatestRef(pagingByConversation);
  const conversationPagingRef = useLatestRef(conversationPaging);
  const currentUserIdRef = useLatestRef(currentUserId);
  const currentUserRef = useLatestRef(currentUser);
  const typingTimersRef = useRef(new Map());
  const readCursorRef = useRef(new Map());
  const uploadAbortRef = useRef(null);
  const lostRealtimeConnectionRef = useRef(false);

  const loadConversations = useCallback(
    async ({ selectFirst = false, cursor = null, snapshotAt = null } = {}) => {
      const box = await getConversations({
        limit: CONVERSATION_PAGE_SIZE,
        cursor,
        snapshotAt,
      });
      const normalized = (box?.items ?? []).map((conversation) =>
        normalizeConversation(conversation),
      );

      setConversations((current) => mergeConversationList(current, normalized));
      setConversationPaging((current) => ({
        nextCursor: box?.paging?.nextCursor ?? null,
        hasMore: Boolean(box?.paging?.hasMore),
        snapshotAt: box?.paging?.snapshotAt ?? snapshotAt ?? current.snapshotAt ?? null,
      }));

      if (selectFirst && !activeIdRef.current && normalized[0]?.id != null) {
        dispatch(setActiveConversation(normalized[0].id));
      }

      return normalized;
    },
    [activeIdRef, dispatch],
  );

  useEffect(() => {
    let ignore = false;

    async function bootstrap() {
      setLoadingConversations(true);
      setError("");

      try {
        const [profile] = await Promise.all([
          getMyProfile().catch(() => null),
          loadConversations({ selectFirst: true }),
        ]);

        if (ignore) return;
        if (profile) dispatch(profileLoaded(profile));
      } catch (apiError) {
        if (!ignore) setError(getApiErrorMessage(apiError, t, "errors.conversations"));
      } finally {
        if (!ignore) setLoadingConversations(false);
      }
    }

    bootstrap();

    return () => {
      ignore = true;
    };
  }, [dispatch, loadConversations, t]);
  const loadOlderConversations = useCallback(async () => {
    const paging = conversationPagingRef.current;
    if (!paging?.hasMore || !paging.nextCursor || loadingOlderConversations) return;

    setLoadingOlderConversations(true);
    try {
      await loadConversations({
        cursor: paging.nextCursor,
        snapshotAt: paging.snapshotAt,
      });
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, t, "errors.conversations"));
    } finally {
      setLoadingOlderConversations(false);
    }
  }, [conversationPagingRef, loadConversations, loadingOlderConversations, t]);


  useEffect(() => {
    if (!activeConversationId) return undefined;

    let ignore = false;

    async function loadActiveConversation() {
      setLoadingMessages(true);
      setError("");

      try {
        const [detail, history, pins] = await Promise.all([
          getConversation(activeConversationId).catch(() => null),
          getMessageHistory({ conversationId: activeConversationId, limit: MESSAGE_PAGE_SIZE }),
          getPinnedMessages(activeConversationId).catch(() => null),
        ]);

        if (ignore) return;

        const pinnedByMessageId = new Map(
          (pins?.items ?? [])
            .filter((pin) => pin?.message?.id != null)
            .map((pin) => [String(pin.message.id), pin]),
        );
        const messages = sortMessages((history?.items ?? []).map(normalizeMessage)).map((message) => {
          const pin = pinnedByMessageId.get(String(message.id));
          return pin ? { ...message, pinned: true, pin } : message;
        });

        setPagingByConversation((current) => ({
          ...current,
          [activeConversationId]: {
            nextCursor: history?.paging?.nextCursor ?? null,
            hasMore: Boolean(history?.paging?.hasMore),
          },
        }));

        setConversations((current) => {
          const existing = findConversation(current, activeConversationId);
          const merged = detail
            ? mergeConversationList(current, [
                normalizeConversation(detail, existing?.messages ?? []),
              ])
            : current;

          return updateConversationMessagesInList(merged, activeConversationId, () => messages);
        });
      } catch (apiError) {
        if (!ignore) setError(getApiErrorMessage(apiError, t, "errors.messages"));
      } finally {
        if (!ignore) setLoadingMessages(false);
      }
    }

    loadActiveConversation();

    return () => {
      ignore = true;
    };
  }, [activeConversationId, realtimeRevision, t]);

  const clearTypingEntry = useCallback((conversationId, userId) => {
    setTypingByConversation((current) => {
      const entries = current[conversationId];
      if (!entries) return current;

      const next = entries.filter((entry) => !isSameId(entry.userId, userId));
      if (next.length === entries.length) return current;

      return {
        ...current,
        [conversationId]: next,
      };
    });
  }, []);

  const applyTypingEvent = useCallback(
    (conversationId, data) => {
      if (conversationId == null || data?.userId == null) return;

      const timerKey = `${conversationId}:${data.userId}`;
      const timers = typingTimersRef.current;
      clearTimeout(timers.get(timerKey));
      timers.delete(timerKey);

      if (!data.typing) {
        clearTypingEntry(conversationId, data.userId);
        return;
      }

      setTypingByConversation((current) => {
        const entries = current[conversationId] ?? [];
        if (entries.some((entry) => isSameId(entry.userId, data.userId))) return current;

        return {
          ...current,
          [conversationId]: [...entries, { userId: data.userId, username: data.username }],
        };
      });

      timers.set(
        timerKey,
        setTimeout(() => {
          timers.delete(timerKey);
          clearTypingEntry(conversationId, data.userId);
        }, TYPING_EXPIRY),
      );
    },
    [clearTypingEntry],
  );

  useEffect(
    () => () => {
      typingTimersRef.current.forEach((timer) => clearTimeout(timer));
      typingTimersRef.current.clear();
      uploadAbortRef.current?.abort();
    },
    [],
  );

  const handleRealtimeEvent = useCallback(
    (envelope) => {
      const { eventType, data, conversationId } = envelope ?? {};
      const viewerId = currentUserIdRef.current;

      switch (eventType) {
        case "message.created": {
          if (!data?.message) return;
          const message = normalizeMessage(data.message);
          if (message.id != null && !isSameId(message.senderId, viewerId)) {
            publishRealtime(
              `/app/messages/${message.id}/delivered`,
              {},
              { queueIfDisconnected: true },
            );
          }
          const isKnown = conversationsRef.current.some((conversation) =>
            isSameId(conversation.id, message.conversationId),
          );

          if (!isKnown) {
            // A conversation created elsewhere: pull the list so it appears in the sidebar.
            loadConversations().catch(() => {});
            return;
          }

          setConversations(
            (current) => applyIncomingMessageToList(current, message, viewerId).conversations,
          );
          applyTypingEvent(message.conversationId, { userId: message.senderId, typing: false });
          break;
        }
        case "message.updated":
        case "message.deleted": {
          if (!data?.message) return;
          setConversations((current) =>
            applyMessageResponseToList(current, normalizeMessage(data.message)),
          );
          break;
        }
        case "message.pinned": {
          const pin = data?.pin;
          if (pin?.message?.id != null) {
            setConversations((current) =>
              applyMessagePinToList(current, pin.message.id, pin),
            );
          }
          break;
        }
        case "message.unpinned": {
          if (data?.messageId != null) {
            setConversations((current) =>
              applyMessagePinToList(current, data.messageId, null),
            );
          }
          break;
        }
        case "message.status.updated": {
          setConversations((current) => applyMessageStatusToList(current, data, viewerId));
          break;
        }
        case "message.reaction.updated": {
          if (data?.messageId == null) break;
          getMessageReactions(data.messageId)
            .then(normalizeReactionGroups)
            .then((reactions) => {
              setConversations((current) =>
                applyMessageReactionsToList(current, data.messageId, reactions),
              );
            })
            .catch(() => {});
          break;
        }
        case "message.read": {
          setConversations((current) =>
            applyReadReceiptToList(current, {
              conversationId: conversationId ?? data?.conversationId,
              readerId: data?.readerId,
              lastReadMessageId: data?.lastReadMessageId,
              readAt: data?.readAt,
              currentUserId: viewerId,
            }),
          );
          break;
        }
        case "typing.updated": {
          applyTypingEvent(conversationId ?? data?.conversationId, data);
          break;
        }
        case "presence.updated": {
          setConversations((current) => applyPresenceToList(current, data));
          break;
        }
        case "group.member.removed": {
          if (isSameId(data?.affectedUserId, viewerId)) {
            setConversations((current) =>
              current.filter((item) => !isSameId(item.id, conversationId)),
            );
            if (isSameId(activeIdRef.current, conversationId)) {
              dispatch(setActiveConversation(null));
            }
          } else {
            loadConversations().catch(() => {});
          }
          break;
        }
        case "conversation.updated":
        case "group.created":
        case "group.updated":
        case "group.member.added": {
          if (data?.conversation) {
            setConversations((current) =>
              mergeConversationList(current, [
                normalizeConversation(
                  data.conversation,
                  findConversation(current, data.conversation.id)?.messages ?? [],
                ),
              ]),
            );
            return;
          }

          loadConversations().catch(() => {});
          break;
        }
        default:
          break;
      }
    },
    [activeIdRef, applyTypingEvent, conversationsRef, currentUserIdRef, dispatch, loadConversations],
  );


  const handleRealtimeError = useCallback(
    (payload) => {
      const serverMessage = payload?.fieldErrors?.[0]?.message ?? payload?.message;
      const translated = serverMessage ? t(serverMessage) : "";
      const looksLikeMessageKey = serverMessage && translated === serverMessage && /^[a-z][a-z\d]*(?:[._-][a-z\d]+)+$/i.test(serverMessage);
      setError(looksLikeMessageKey ? t("errors.realtime") : translated || t("errors.realtime"));
    },
    [t],
  );
  useRealtimeTopic(currentUserId ? USER_ERRORS_DESTINATION : null, handleRealtimeError);

  const realtimeStatus = useRealtimeTopic(
    currentUserId ? USER_EVENTS_DESTINATION : null,
    handleRealtimeEvent,
  );

  useEffect(() => {
    if (realtimeStatus === "disconnected" || realtimeStatus === "error") {
      lostRealtimeConnectionRef.current = true;
      return;
    }

    if (realtimeStatus !== "connected" || !lostRealtimeConnectionRef.current) return;

    lostRealtimeConnectionRef.current = false;
    loadConversations().catch((apiError) => {
      setError(getApiErrorMessage(apiError, t, "errors.conversations"));
    });
    setRealtimeRevision((current) => current + 1);
  }, [loadConversations, realtimeStatus, t]);


  const activeConversation = useMemo(
    () => findConversation(conversations, activeConversationId),
    [activeConversationId, conversations],
  );

  // Acknowledge the newest incoming message once it is on screen.
  useEffect(() => {
    if (!activeConversation?.id || loadingMessages) return;

    const lastMessageId = lastIncomingMessageId(activeConversation, currentUserId);
    if (lastMessageId == null) return;
    if (isSameId(readCursorRef.current.get(String(activeConversation.id)), lastMessageId)) return;

    readCursorRef.current.set(String(activeConversation.id), lastMessageId);
    setConversations((current) => resetConversationUnread(current, activeConversation.id));

    markConversationRead({
      conversationId: activeConversation.id,
      lastReadMessageId: lastMessageId,
    }).catch(() => {
      // A failed acknowledgement is retried the next time a message arrives.
      readCursorRef.current.delete(String(activeConversation.id));
    });
  }, [activeConversation, currentUserId, loadingMessages]);

  const selectConversation = useCallback(
    (conversationId) => {
      dispatch(setActiveConversation(conversationId));
    },
    [dispatch],
  );

  const loadOlderMessages = useCallback(async () => {
    const conversationId = activeIdRef.current;
    const paging = conversationId != null ? pagingRef.current[conversationId] : null;
    if (!conversationId || !paging?.hasMore || !paging.nextCursor) return;

    setLoadingOlder(true);

    try {
      const history = await getMessageHistory({
        conversationId,
        limit: MESSAGE_PAGE_SIZE,
        cursor: paging.nextCursor,
      });
      const olderMessages = sortMessages((history?.items ?? []).map(normalizeMessage));

      setConversations((current) =>
        updateConversationMessagesInList(current, conversationId, (messages) =>
          prependMessageHistory(messages, olderMessages),
        ),
      );
      setPagingByConversation((current) => ({
        ...current,
        [conversationId]: {
          nextCursor: history?.paging?.nextCursor ?? null,
          hasMore: Boolean(history?.paging?.hasMore),
        },
      }));
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, t, "errors.olderMessages"));
    } finally {
      setLoadingOlder(false);
    }
  }, [activeIdRef, pagingRef, t]);

  const sendDraft = useCallback(
    async (content, files = []) => {
      const conversationId = activeIdRef.current;
      const trimmedContent = content.trim();
      const selectedFiles = Array.from(files).filter((file) => file.size > 0);
      const hasAttachments = selectedFiles.length > 0;
      if (!conversationId || (!trimmedContent && !hasAttachments)) return false;

      const uploadController = hasAttachments ? new AbortController() : null;
      uploadAbortRef.current = uploadController;
      setUploadProgress(hasAttachments ? 0 : null);

      const clientMessageId = createClientId();
      const messageType = hasAttachments ? "MEDIA" : "TEXT";
      const optimisticMessage = {
        id: null,
        clientMessageId,
        conversationId,
        senderId: currentUserIdRef.current,
        sender: currentUserRef.current,
        content: trimmedContent,
        replyTo: null,
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
      setConversations((current) =>
        updateConversationMessagesInList(current, conversationId, (messages) => [
          ...messages,
          optimisticMessage,
        ]),
      );

      try {
        const uploadedAssets = hasAttachments
          ? await uploadMessageAttachments(selectedFiles, {
              purpose: "MESSAGE_ATTACHMENT",
              signal: uploadController.signal,
              onProgress: ({ progress }) => setUploadProgress(Math.round(progress * 100)),
            })
          : [];
        const response = await sendMessage({
          conversationId,
          clientMessageId,
          content: trimmedContent,
          messageType,
          replyToMessageId: null,
          attachments: uploadedAssets.map((asset) => ({ assetId: asset.id })),
        });

        setConversations((current) =>
          applyOutgoingMessageToList(current, normalizeMessage(response)),
        );
        return true;
      } catch (apiError) {
        const canceled =
          apiError?.name === "AbortError" || uploadController?.signal.aborted;
        setError(
          canceled ? t("errors.uploadCanceled") : getApiErrorMessage(apiError, t, "errors.sendMessage"),
        );
        setConversations((current) =>
          updateConversationMessagesInList(current, conversationId, (messages) =>
            messages.map((message) =>
              isSameId(message.clientMessageId, clientMessageId)
                ? { ...message, pending: false, failed: true, status: "FAILED" }
                : message,
            ),
          ),
        );
        return false;
      } finally {
        if (uploadAbortRef.current === uploadController) uploadAbortRef.current = null;
        setUploadProgress(null);
        setSending(false);
      }
    },
    [activeIdRef, currentUserIdRef, currentUserRef, t],
  );


  const applyConversationResponse = useCallback(
    (response) => {
      const existing = findConversation(conversationsRef.current, response?.id);
      const normalized = normalizeConversation(response, existing?.messages ?? []);
      setConversations((current) => mergeConversationList(current, [normalized]));
      return normalized;
    },
    [conversationsRef],
  );

  const handleActionError = useCallback(
    (apiError, fallbackKey) => {
      setError(getApiErrorMessage(apiError, t, fallbackKey));
      throw apiError;
    },
    [t],
  );

  const createGroup = useCallback(
    async (request) => {
      try {
        const normalized = applyConversationResponse(await createGroupConversation(request));
        dispatch(setActiveConversation(normalized.id));
        return normalized;
      } catch (apiError) {
        return handleActionError(apiError, "errors.createGroup");
      }
    },
    [applyConversationResponse, dispatch, handleActionError],
  );

  const inviteMembers = useCallback(
    async (memberIds) => {
      try {
        return applyConversationResponse(
          await inviteGroupMembers(activeIdRef.current, memberIds),
        );
      } catch (apiError) {
        return handleActionError(apiError, "errors.groupMembers");
      }
    },
    [activeIdRef, applyConversationResponse, handleActionError],
  );

  const acceptInvitation = useCallback(async () => {
    try {
      return applyConversationResponse(await acceptGroupInvitation(activeIdRef.current));
    } catch (apiError) {
      return handleActionError(apiError, "errors.groupInvitation");
    }
  }, [activeIdRef, applyConversationResponse, handleActionError]);

  const rejectInvitation = useCallback(async () => {
    const conversationId = activeIdRef.current;
    try {
      await rejectGroupInvitation(conversationId);
      setConversations((current) => current.filter((item) => !isSameId(item.id, conversationId)));
      dispatch(setActiveConversation(null));
    } catch (apiError) {
      return handleActionError(apiError, "errors.groupInvitation");
    }
  }, [activeIdRef, dispatch, handleActionError]);

  const removeMember = useCallback(
    async (memberId) => {
      try {
        return applyConversationResponse(
          await removeGroupMember(activeIdRef.current, memberId),
        );
      } catch (apiError) {
        return handleActionError(apiError, "errors.groupMembers");
      }
    },
    [activeIdRef, applyConversationResponse, handleActionError],
  );

  const leaveActiveGroup = useCallback(async () => {
    const conversationId = activeIdRef.current;
    try {
      await leaveGroup(conversationId);
      setConversations((current) => current.filter((item) => !isSameId(item.id, conversationId)));
      dispatch(setActiveConversation(null));
    } catch (apiError) {
      return handleActionError(apiError, "errors.groupLeave");
    }
  }, [activeIdRef, dispatch, handleActionError]);

  const saveGroupProfile = useCallback(
    async (request) => {
      try {
        return applyConversationResponse(
          await updateGroupProfile(activeIdRef.current, request),
        );
      } catch (apiError) {
        return handleActionError(apiError, "errors.groupProfile");
      }
    },
    [activeIdRef, applyConversationResponse, handleActionError],
  );

  const changeMemberRole = useCallback(
    async (memberId, role) => {
      try {
        return applyConversationResponse(
          await updateGroupMemberRole(activeIdRef.current, memberId, role),
        );
      } catch (apiError) {
        return handleActionError(apiError, "errors.groupMembers");
      }
    },
    [activeIdRef, applyConversationResponse, handleActionError],
  );

  const refreshMessageReactions = useCallback(
    async (messageId) => {
      const response = await getMessageReactions(messageId);
      const reactions = normalizeReactionGroups(response);
      setConversations((current) =>
        applyMessageReactionsToList(current, messageId, reactions),
      );
      return reactions;
    },
    [],
  );

  const toggleReaction = useCallback(
    async (messageId, emoji) => {
      const message = conversationsRef.current
        .flatMap((conversation) => conversation.messages ?? [])
        .find((item) => isSameId(item.id, messageId));
      const currentReaction = message?.reactions?.find((item) => item.emoji === emoji);

      try {
        if (currentReaction?.reactedByMe) {
          await removeMessageReaction(messageId, emoji);
        } else {
          await addMessageReaction(messageId, emoji);
        }
        return await refreshMessageReactions(messageId);
      } catch (apiError) {
        return handleActionError(apiError, "errors.messageReaction");
      }
    },
    [conversationsRef, handleActionError, refreshMessageReactions],
  );

  const pinActiveMessage = useCallback(
    async (messageId) => {
      try {
        const pin = await pinMessageApi(messageId);
        setConversations((current) => applyMessagePinToList(current, messageId, pin));
        return pin;
      } catch (apiError) {
        return handleActionError(apiError, "errors.messagePin");
      }
    },
    [handleActionError],
  );

  const unpinActiveMessage = useCallback(
    async (messageId) => {
      try {
        await unpinMessageApi(messageId);
        setConversations((current) => applyMessagePinToList(current, messageId, null));
      } catch (apiError) {
        return handleActionError(apiError, "errors.messagePin");
      }
    },
    [handleActionError],
  );

  const editActiveMessage = useCallback(
    async (messageId, newContent) => {
      const message = conversationsRef.current
        .flatMap((conversation) => conversation.messages ?? [])
        .find((item) => isSameId(item.id, messageId));
      try {
        const response = await editMessageApi(messageId, {
          newContent,
          type: message?.messageType,
        });
        setConversations((current) =>
          applyMessageResponseToList(current, normalizeMessage(response)),
        );
        return response;
      } catch (apiError) {
        return handleActionError(apiError, "errors.messageEdit");
      }
    },
    [conversationsRef, handleActionError],
  );

  const deleteActiveMessage = useCallback(
    async (messageId) => {
      try {
        const response = await deleteMessageApi(messageId);
        setConversations((current) =>
          applyMessageResponseToList(current, normalizeMessage(response)),
        );
        return response;
      } catch (apiError) {
        return handleActionError(apiError, "errors.messageDelete");
      }
    },
    [handleActionError],
  );

  const loadMessageReadReceipts = useCallback(
    async (messageId) => {
      try {
        const response = await getMessageReadReceipts(messageId);
        const receipts = response?.items ?? [];
        setConversations((current) =>
          applyMessageReadReceiptsToList(current, messageId, receipts),
        );
        return receipts;
      } catch (apiError) {
        return handleActionError(apiError, "errors.messageReads");
      }
    },
    [handleActionError],
  );

  const notifyTyping = useCallback(
    (typing) => {
      const conversationId = activeIdRef.current;
      if (!conversationId) return;

      publishRealtime(`/app/conversations/${conversationId}/typing`, { typing });
    },
    [activeIdRef],
  );

  const startDirectConversation = useCallback(
    async (targetUserId) => {
      const conversation = await createDirectConversation(targetUserId);
      const normalized = normalizeConversation(conversation);

      setConversations((current) => mergeConversationList(current, [normalized]));
      dispatch(setActiveConversation(normalized.id));

      return normalized;
    },
    [dispatch],
  );

  const cancelUpload = useCallback(() => {
    uploadAbortRef.current?.abort();
  }, []);

  const activeTypingUsers = activeConversation
    ? typingByConversation[activeConversation.id] ?? []
    : [];

  return {
    acceptInvitation,
    activeConversation,
    activeTypingUsers,
    cancelUpload,
    changeMemberRole,
    createGroup,
    conversations,
    currentUser,
    deleteActiveMessage,
    dismissError: () => setError(""),
    editActiveMessage,
    error,
    getMessageReactions: refreshMessageReactions,
    hasOlderMessages: Boolean(
      activeConversation && pagingByConversation[activeConversation.id]?.hasMore,
    ),
    hasMoreConversations: Boolean(conversationPaging.hasMore),
    inviteMembers,
    leaveActiveGroup,
    loadMessageReadReceipts,
    loadOlderMessages,
    loadOlderConversations,
    loadingConversations,
    loadingMessages,
    loadingOlder,
    loadingOlderConversations,
    pinActiveMessage,
    notifyTyping,
    realtimeStatus,
    rejectInvitation,
    removeMember,
    saveGroupProfile,
    selectConversation,
    sendDraft,
    sending,
    uploadProgress,
    toggleReaction,
    startDirectConversation,
    unpinActiveMessage,
  };
}

export default useChatWorkspace;
