import { isSameId } from "../../utils/chat";
import {
  getConversation,
  listConversationPins,
  listConversations,
} from "../../services/conversationApi";
import { listMessages } from "../../services/messageApi";
import {
  dedupeById,
  getNormalizedConversationContacts,
  mergeContacts,
  normalizeConversation,
  normalizeMessage,
} from "./normalizers";
import { mergeConversationList, updateConversationMessagesInList } from "./conversationState";

export function createConversationDomain({
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
}) {
  function updateConversationMessages(conversationId, updater) {
    setConversations((previous) =>
      updateConversationMessagesInList(previous, conversationId, updater),
    );
  }

  async function loadConversations({ append = false, silent = false } = {}) {
    const paging = append ? conversationPaging : null;

    if (append && !paging?.hasMore) return;
    if (conversationRequestInFlightRef.current) return;
    conversationRequestInFlightRef.current = true;

    if (append) {
      setIsLoadingMoreConversations(true);
    } else if (!silent) {
      setIsLoadingConversations(true);
    }
    if (!silent) setConversationError("");

    try {
      const response = await listConversations({
        limit: silent
          ? Math.min(Math.max(conversationsRef.current.length, 20), 50)
          : 20,
        cursor: append ? paging?.nextCursor : null,
        snapshotAt: append ? paging?.snapshotAt : null,
      });
      const normalized = (response.items ?? []).map((conversation) => {
        const existing = conversationsRef.current.find(
          (item) => String(item.id) === String(conversation.id),
        );
        return normalizeConversation(conversation, existing?.messages ?? []);
      });
      const nextContacts = normalized.flatMap(getNormalizedConversationContacts);

      setContacts((previous) => mergeContacts(previous, nextContacts));
      setConversations((previous) =>
        append ? mergeConversationList(previous, normalized) : mergeConversationList([], normalized),
      );
      setConversationPaging(response.paging ?? null);
    } catch (error) {
      if (!silent) setConversationError(error.message || "Could not load conversations.");
    } finally {
      conversationRequestInFlightRef.current = false;
      if (!append) setHasLoadedConversations(true);
      if (!silent) setIsLoadingConversations(false);
      setIsLoadingMoreConversations(false);
    }
  }

  async function loadConversation(conversationId, { force = false } = {}) {
    if (!conversationId) return null;

    setLoadingMessagesByConversation((previous) => ({ ...previous, [conversationId]: true }));
    setMessageErrorByConversation((previous) => ({ ...previous, [conversationId]: "" }));

    try {
      const conversationSummary = conversationsRef.current.find(
        (conversation) => String(conversation.id) === String(conversationId),
      );
      if (!force && (
        conversationSummary?.currentUserStatus === "PENDING" ||
        conversationSummary?.isPendingInvitation
      )) {
        replaceConversationPins(conversationId, { items: [] });
        setMessagePagingByConversation((previous) => ({
          ...previous,
          [conversationId]: null,
        }));
        return conversationSummary;
      }

      let conversationResponse = await getConversation(conversationId);
      let isPendingInvitation =
        conversationResponse.currentUserStatus === "PENDING" ||
        conversationResponse.status === "PENDING" ||
        Boolean(conversationResponse.isPendingInvitation);
      let messageResponse = { items: [], paging: null };
      let pinsResponse = { items: [] };

      if (!isPendingInvitation) {
        try {
          [messageResponse, pinsResponse] = await Promise.all([
            listMessages({ conversationId, limit: 20 }),
            listConversationPins(conversationId).catch(() => ({ items: [] })),
          ]);
        } catch (error) {
          if (conversationResponse.type !== "GROUP" || error.status !== 403) throw error;

          isPendingInvitation = true;
          conversationResponse = {
            ...conversationResponse,
            currentUserStatus: "PENDING",
            isPendingInvitation: true,
          };
        }
      }
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

  function applyConversationResponse(conversationResponse, existingMessages = null) {
    const existing = conversations.find(
      (item) => String(item.id) === String(conversationResponse.id),
    );
    const normalizedConversation = normalizeConversation(
      conversationResponse,
      existingMessages ?? existing?.messages ?? [],
    );

    mergeNormalizedConversationContacts(normalizedConversation);
    setConversations((previous) => mergeConversationList(previous, [normalizedConversation]));
    return normalizedConversation;
  }

  function getConversationById(conversationId, pinnedMessageIdsByConversation, pinnedMessagesByConversation) {
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

  return {
    updateConversationMessages,
    loadConversations,
    loadConversation,
    loadMoreMessages,
    applyConversationResponse,
    getConversationById,
  };
}
