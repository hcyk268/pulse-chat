import { isSameId } from "../../utils/chat";
import {
  deleteMessage as deleteMessageApi,
  editMessage as editMessageApi,
  getMessageReadReceipts,
  getMessageReactions,
  markMessagesRead,
  pinMessage as pinMessageApi,
  reactToMessage,
  removeMessageReaction,
  sendMessage as sendMessageApi,
  unpinMessage as unpinMessageApi,
} from "../../services/messageApi";
import { uploadMessageAttachment } from "../../services/uploadApi";
import {
  createClientId,
  dedupeById,
  getMessagePreview,
  getPinnedMessageIds,
  normalizeMessage,
  normalizePin,
  normalizeReactionGroups,
} from "./normalizers";
import { mergeConversationList } from "./conversationState";

export function createMessageDomain({
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
}) {
  function setMessagePinned(conversationId, messageId, pinned) {
    if (!conversationId || !messageId) return;

    const key = String(conversationId);
    const messageKey = String(messageId);

    setPinnedMessageIdsByConversation((previous) => {
      const ids = new Set(previous[key] ?? []);
      if (pinned) ids.add(messageKey);
      else ids.delete(messageKey);

      return { ...previous, [key]: Array.from(ids) };
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

      return { ...previous, [key]: [normalizedPin, ...nextPins] };
    });
  }

  function removePinnedMessageDetail(conversationId, messageId) {
    if (!conversationId || !messageId) return;

    const key = String(conversationId);
    setPinnedMessagesByConversation((previous) => ({
      ...previous,
      [key]: (previous[key] ?? []).filter(
        (pin) => !isSameId(pin.message?.id, messageId),
      ),
    }));
  }

  function updatePinnedMessageDetail(message) {
    if (!message?.conversationId || !message.id) return;

    const key = String(message.conversationId);
    setPinnedMessagesByConversation((previous) => ({
      ...previous,
      [key]: (previous[key] ?? []).map((pin) =>
        isSameId(pin.message?.id, message.id)
          ? { ...pin, message: { ...pin.message, ...message } }
          : pin,
      ),
    }));
  }

  function replaceConversationPins(conversationId, pinResponse) {
    if (!conversationId) return;

    const key = String(conversationId);
    setPinnedMessageIdsByConversation((previous) => ({
      ...previous,
      [key]: getPinnedMessageIds(pinResponse),
    }));
    setPinnedMessagesByConversation((previous) => ({
      ...previous,
      [key]: (pinResponse?.items ?? []).map(normalizePin),
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
      setReadReceiptsByMessageId((previous) => ({ ...previous, [key]: receipts }));
      return receipts;
    } catch (error) {
      setChatActionError(error.message || "Could not load read receipts.");
      return [];
    } finally {
      setLoadingReadReceiptsByMessageId((previous) => ({ ...previous, [key]: false }));
    }
  }

  async function markConversationRead(conversationId, explicitLastReadMessageId = null) {
    const conversation = conversations.find(
      (item) => String(item.id) === String(conversationId),
    );
    const lastReadMessageId =
      explicitLastReadMessageId ??
      conversation?.messages?.at(-1)?.id ??
      conversation?.lastMessage?.id;

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

  async function sendMessage(
    conversationId,
    content,
    { replyToMessageId = null, files = [] } = {},
  ) {
    const trimmed = content.trim();
    const selectedFiles = Array.from(files ?? []);
    if (!trimmed && selectedFiles.length === 0) return null;

    setSendingByConversation((previous) => ({ ...previous, [conversationId]: true }));
    setUploadProgressByConversation((previous) => ({
      ...previous,
      [conversationId]: selectedFiles.length ? 0 : null,
    }));
    setChatActionError("");

    try {
      const attachments = [];
      const totalUploadBytes = selectedFiles.reduce((total, file) => total + file.size, 0);
      let completedUploadBytes = 0;

      for (const file of selectedFiles) {
        const uploaded = await uploadMessageAttachment(file, {
          onProgress: (progress) => {
            const currentFileBytes = file.size * (progress / 100);
            const overallProgress = totalUploadBytes
              ? Math.round(((completedUploadBytes + currentFileBytes) / totalUploadBytes) * 100)
              : 100;
            setUploadProgressByConversation((previous) => ({
              ...previous,
              [conversationId]: overallProgress,
            }));
          },
        });
        attachments.push(uploaded);
        completedUploadBytes += file.size;
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
        const targetConversationId = response?.conversationId ?? conversationId;
        const targetMessageId = response?.messageId ?? messageId;
        setMessagePinned(targetConversationId, targetMessageId, false);
        removePinnedMessageDetail(targetConversationId, targetMessageId);
        return response;
      }

      const response = await pinMessageApi(messageId);
      const targetConversationId = response?.message?.conversationId ?? conversationId;
      const targetMessageId = response?.message?.id ?? messageId;
      setMessagePinned(targetConversationId, targetMessageId, true);
      setPinnedMessageDetail(targetConversationId, response);
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
      if (reactedByMe) await removeMessageReaction(messageId, emoji);
      else await reactToMessage(messageId, emoji);

      loadedReactionMessageIdsRef.current.delete(String(messageId));
      return loadMessageReactions(messageId, { force: true });
    } catch (error) {
      setChatActionError(error.message || "Could not update message reaction.");
      return null;
    }
  }

  return {
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
  };
}
