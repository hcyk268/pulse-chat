import ChatWindow from "../components/chat/ChatWindow";
import ContactPanel from "../components/chat/ContactPanel";
import ConversationList from "../components/chat/ConversationList";
import { useChatPageController } from "../hooks/useChatPageController";

export default function ChatPage() {
  const {
    chatStore,
    conversationId,
    filteredConversations,
    handleStartConversation,
    query,
    selectedConversation,
    setQuery,
    setShowPeople,
    showPeople,
  } = useChatPageController();
  const {
    chatActionError,
    contacts,
    conversationError,
    conversationPaging,
    currentUser,
    clearUserSearch,
    deleteMessage,
    editMessage,
    getMessageError,
    hasMoreMessages,
    isLoadingConversations,
    isLoadingMoreConversations,
    isLoadingOlderMessages,
    isLoadingSelectedConversation,
    isSendingMessage,
    isStartingConversation,
    isSearchingUsers,
    loadConversations,
    loadMessageReactions,
    loadMoreMessages,
    reactionsByMessageId,
    realtimeStatus,
    searchUsers,
    sendMessage,
    sendTypingStatus,
    signOut,
    startConversationError,
    stats,
    toggleMessageReaction,
    toggleMessagePin,
    typingByConversation,
    userSearchError,
    userSearchResults,
  } = chatStore;

  return (
    <div className="h-screen overflow-hidden bg-[#0a0f1a] text-white">
      <main className="mx-auto flex h-full max-w-[1520px] overflow-hidden rounded-none border-x border-white/[0.03] bg-[#0a0f1a] shadow-2xl xl:my-0">
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
            onLoadMessageReactions={loadMessageReactions}
            onLoadMoreMessages={() => loadMoreMessages(conversationId)}
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
          className="sheet-overlay fixed inset-0 z-50 bg-black/60 backdrop-blur-sm"
          onClick={(event) => {
            if (event.target === event.currentTarget) setShowPeople(false);
          }}
        >
          <div className="sheet-panel ml-auto h-full w-full max-w-md border-l border-white/5 bg-[#111827] shadow-panel">
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
