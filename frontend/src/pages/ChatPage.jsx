import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import ChatWindow from "../components/chat/ChatWindow";
import ContactPanel from "../components/chat/ContactPanel";
import ConversationList from "../components/chat/ConversationList";
import { useChatStore } from "../hooks/useChatStore";

export default function ChatPage() {
  const { conversationId } = useParams();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [showPeople, setShowPeople] = useState(false);
  const {
    chatActionError,
    contacts,
    conversationError,
    conversationPaging,
    conversationSummaries,
    currentUser,
    clearUserSearch,
    deleteMessage,
    editMessage,
    getConversationById,
    getMessageError,
    hasMoreMessages,
    isLoadingConversations,
    isLoadingMoreConversations,
    isLoadingOlderMessages,
    isLoadingSelectedConversation,
    isSendingMessage,
    isStartingConversation,
    isSearchingUsers,
    loadConversation,
    loadConversations,
    loadMessageReactions,
    loadMoreMessages,
    markConversationRead,
    reactionsByMessageId,
    realtimeStatus,
    searchUsers,
    sendMessage,
    sendTypingStatus,
    setActiveConversationId,
    signOut,
    startConversation,
    startConversationError,
    stats,
    toggleMessageReaction,
    toggleMessagePin,
    typingByConversation,
    userSearchError,
    userSearchResults,
  } = useChatStore();

  const selectedConversation = conversationId ? getConversationById(conversationId) : null;

  const filteredConversations = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return conversationSummaries;

    return conversationSummaries.filter((conversation) => {
      const participant = conversation.otherParticipant;
      const searchable = [
        participant?.displayName,
        participant?.username,
        participant?.email,
        conversation.lastMessage?.contentPreview,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();

      return searchable.includes(normalized);
    });
  }, [conversationSummaries, query]);

  useEffect(() => {
    setActiveConversationId(conversationId ?? null);

    return () => setActiveConversationId(null);
  }, [conversationId, setActiveConversationId]);

  useEffect(() => {
    if (conversationId) {
      loadConversation(conversationId).then((conversation) => {
        const lastReadMessageId = conversation?.messages?.at(-1)?.id ?? conversation?.lastMessage?.id;
        markConversationRead(conversationId, lastReadMessageId);
      });
    }
  }, [conversationId]);

  async function handleStartConversation(contact) {
    const nextConversationId = await startConversation(contact.backendId ?? contact.id);
    if (!nextConversationId) return;

    setShowPeople(false);
    navigate(`/chat/${nextConversationId}`);
  }

  return (
    <div className="h-screen overflow-hidden bg-[#0e1621] text-white">
      <main className="mx-auto flex h-full max-w-[1480px] border-x border-black/20 bg-[#0e1621]">
        <div className={`${conversationId ? "hidden md:flex" : "flex"} min-h-0 w-full md:w-[380px]`}>
          <ConversationList
            conversations={filteredConversations}
            error={conversationError}
            hasMore={Boolean(conversationPaging?.hasMore)}
            isLoading={isLoadingConversations}
            isLoadingMore={isLoadingMoreConversations}
            currentUser={currentUser}
            query={query}
            onLoadMore={() => loadConversations({ append: true })}
            onOpenPeople={() => setShowPeople(true)}
            onQueryChange={setQuery}
            onSignOut={signOut}
            realtimeStatus={realtimeStatus}
            selectedId={conversationId}
            stats={stats}
          />
        </div>

        <div className={`${conversationId ? "flex" : "hidden md:flex"} min-w-0 flex-1`}>
          <ChatWindow
            conversation={selectedConversation}
            currentUser={currentUser}
            error={conversationId ? getMessageError(conversationId) : ""}
            hasMoreMessages={hasMoreMessages(conversationId)}
            isLoading={isLoadingSelectedConversation(conversationId)}
            isLoadingMoreMessages={isLoadingOlderMessages(conversationId)}
            isSending={isSendingMessage(conversationId)}
            isTyping={Boolean(conversationId && typingByConversation[conversationId])}
            onDeleteMessage={deleteMessage}
            onEditMessage={editMessage}
            onLoadMoreMessages={() => loadMoreMessages(conversationId)}
            onLoadMessageReactions={loadMessageReactions}
            onSendMessage={sendMessage}
            onToggleMessageReaction={toggleMessageReaction}
            onToggleMessagePin={(message) => toggleMessagePin(conversationId, message)}
            onTypingChange={(typing) => sendTypingStatus(conversationId, typing)}
            reactionsByMessageId={reactionsByMessageId}
            sendError={chatActionError}
          />
        </div>
      </main>

      {showPeople ? (
        <div
          className="sheet-overlay fixed inset-0 z-50 bg-black/55 backdrop-blur-sm"
          onClick={(event) => {
            if (event.target === event.currentTarget) setShowPeople(false);
          }}
        >
          <div className="sheet-panel ml-auto h-full w-full max-w-md border-l border-black/30 bg-[#17212b] shadow-panel">
            <ContactPanel
              contacts={contacts}
              isSearching={isSearchingUsers}
              isStartingConversation={isStartingConversation}
              onClearSearch={clearUserSearch}
              onClose={() => setShowPeople(false)}
              onSearchUsers={searchUsers}
              onStartConversation={handleStartConversation}
              searchError={userSearchError || startConversationError}
              searchResults={userSearchResults}
            />
          </div>
        </div>
      ) : null}
    </div>
  );
}
