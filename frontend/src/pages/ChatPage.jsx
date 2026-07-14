import ChatWindow from "../components/chat/ChatWindow";
import ContactPanel from "../components/chat/ContactPanel";
import ConversationList from "../components/chat/ConversationList";
import ProfileModal from "../components/chat/ProfileModal";
import { useChatPageController } from "../hooks/useChatPageController";

export default function ChatPage() {
  const {
    chatStore,
    conversationId,
    filteredConversations,
    handleCreateGroup,
    handleStartConversation,
    query,
    selectedConversation,
    setQuery,
    setShowPeople,
    setShowProfile,
    showPeople,
    showProfile,
  } = useChatPageController();
  const {
    acceptGroupInvitation,
    addMembersToGroup,
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
    leaveCurrentGroup,
    loadConversations,
    loadMessageReactions,
    loadMessageReadReceipts,
    loadMoreMessages,
    readReceiptsByMessageId,
    reactionsByMessageId,
    realtimeStatus,
    rejectGroupInvitation,
    removeMemberFromGroup,
    searchUsers,
    sendMessage,
    sendTypingStatus,
    signOut,
    startConversationError,
    stats,
    toggleMessageReaction,
    toggleMessagePin,
    typingByConversation,
    updateGroup,
    updateGroupMemberRole,
    updateProfile,
    uploadProgressByConversation,
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
            onOpenProfile={() => setShowProfile(true)}
            onQueryChange={setQuery}
            onSignOut={signOut}
            realtimeStatus={realtimeStatus}
            selectedId={conversationId}
            stats={stats}
          />
        </div>

        <div className={`${conversationId ? "flex" : "hidden md:flex"} min-w-0 flex-1`}>
          <ChatWindow
            acceptGroupInvitation={acceptGroupInvitation}
            addMembersToGroup={addMembersToGroup}
            clearUserSearch={clearUserSearch}
            contacts={contacts}
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
            leaveCurrentGroup={leaveCurrentGroup}
            loadMessageReadReceipts={loadMessageReadReceipts}
            onLoadMessageReactions={loadMessageReactions}
            onLoadMoreMessages={() => loadMoreMessages(conversationId)}
            onSendMessage={sendMessage}
            onToggleMessageReaction={toggleMessageReaction}
            onToggleMessagePin={(message) => toggleMessagePin(conversationId, message)}
            onTypingChange={(typing) => sendTypingStatus(conversationId, typing)}
            readReceiptsByMessageId={readReceiptsByMessageId}
            reactionsByMessageId={reactionsByMessageId}
            rejectGroupInvitation={rejectGroupInvitation}
            removeMemberFromGroup={removeMemberFromGroup}
            searchUsers={searchUsers}
            sendError={chatActionError}
            updateGroup={updateGroup}
            updateGroupMemberRole={updateGroupMemberRole}
            uploadProgress={conversationId ? uploadProgressByConversation[conversationId] : null}
            userSearchResults={userSearchResults}
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
              onCreateGroup={handleCreateGroup}
              onSearchUsers={searchUsers}
              onStartConversation={handleStartConversation}
              searchError={userSearchError || startConversationError}
              searchResults={userSearchResults}
            />
          </div>
        </div>
      ) : null}
      {showProfile ? (
        <ProfileModal
          currentUser={currentUser}
          onClose={() => setShowProfile(false)}
          onSave={updateProfile}
        />
      ) : null}
    </div>
  );
}
