import { useDeferredValue, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useChatStore } from "./useChatStore";
import { useLatestRef } from "./useLatestRef";
import { filterConversations } from "../utils/chat";

export function useChatPageController() {
  const { conversationId } = useParams();
  const navigate = useNavigate();
  const chatStore = useChatStore();
  const chatStoreRef = useLatestRef(chatStore);
  const [query, setQuery] = useState("");
  const [showPeople, setShowPeople] = useState(false);
  const deferredQuery = useDeferredValue(query);

  const selectedConversation = conversationId
    ? chatStore.getConversationById(conversationId)
    : null;

  const filteredConversations = useMemo(
    () => filterConversations(chatStore.conversationSummaries, deferredQuery),
    [chatStore.conversationSummaries, deferredQuery],
  );

  useEffect(() => {
    chatStoreRef.current.setActiveConversationId(conversationId ?? null);

    return () => chatStoreRef.current.setActiveConversationId(null);
  }, [chatStoreRef, conversationId]);

  useEffect(() => {
    let cancelled = false;

    async function hydrateConversation() {
      if (!conversationId) {
        return;
      }

      const conversation = await chatStoreRef.current.loadConversation(conversationId);
      if (cancelled) {
        return;
      }

      const lastReadMessageId =
        conversation?.messages?.at(-1)?.id ?? conversation?.lastMessage?.id;

      chatStoreRef.current.markConversationRead(conversationId, lastReadMessageId);
    }

    hydrateConversation();

    return () => {
      cancelled = true;
    };
  }, [chatStoreRef, conversationId]);

  async function handleStartConversation(contact) {
    if (contact.directConversationId) {
      setShowPeople(false);
      navigate(`/chat/${contact.directConversationId}`);
      return;
    }

    const nextConversationId = await chatStore.startConversation(
      contact.backendId ?? contact.id,
    );

    if (!nextConversationId) {
      return;
    }

    setShowPeople(false);
    navigate(`/chat/${nextConversationId}`);
  }

  return {
    chatStore,
    conversationId,
    filteredConversations,
    query,
    selectedConversation,
    setQuery,
    setShowPeople,
    showPeople,
    handleStartConversation,
  };
}
