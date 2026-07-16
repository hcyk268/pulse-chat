import ChatWindow from "../components/chat/ChatWindow";
import ContactPanel from "../components/chat/ContactPanel";
import ConversationList from "../components/chat/ConversationList";
import ProfileModal from "../components/chat/ProfileModal";
import { useChatPageController } from "../hooks/useChatPageController";
import { useNavigate } from "react-router-dom";
import { useToast } from "../hooks/useToast";

export default function ChatPage() {
  const navigate = useNavigate();
  const toast = useToast();
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
    loadConversation,
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

  async function handleAcceptInvitation(targetConversationId) {
    const accepted = await acceptGroupInvitation(targetConversationId);
    if (accepted) {
      toast.success("Group invitation accepted.");
      navigate(`/chat/${targetConversationId}`);
    }
    return accepted;
  }

  async function handleRejectInvitation(targetConversationId) {
    const rejected = await rejectGroupInvitation(targetConversationId);
    if (rejected) {
      toast.info("Group invitation declined.");
      if (String(conversationId) === String(targetConversationId)) navigate("/chat");
    }
    return rejected;
  }

  async function handleLeaveGroup(targetConversationId) {
    const left = await leaveCurrentGroup(targetConversationId);
    if (left) {
      toast.info("You left the group.");
      if (String(conversationId) === String(targetConversationId)) navigate("/chat");
    }
    return left;
  }

  return (
    <div className="chat-page-shell text-white">
      <main className="chat-app-layout">
        <div
          className={[
            "conversation-pane",
            conversationId ? "conversation-pane--mobile-hidden" : "",
          ].join(" ")}
        >
          <ConversationList
            conversations={filteredConversations}
            error={conversationError}
            hasMore={Boolean(conversationPaging?.hasMore)}
            isLoading={isLoadingConversations}
            isLoadingMore={isLoadingMoreConversations}
            currentUser={currentUser}
            query={query}
            onLoadMore={() => loadConversations({ append: true })}
            onAcceptInvitation={handleAcceptInvitation}
            onRejectInvitation={handleRejectInvitation}
            onOpenPeople={() => setShowPeople(true)}
            onOpenProfile={() => setShowProfile(true)}
            onQueryChange={setQuery}
            onRetry={() => loadConversations()}
            onSignOut={signOut}
            realtimeStatus={realtimeStatus}
            selectedId={conversationId}
            stats={stats}
          />
        </div>

        <div
          className={[
            "chat-pane",
            conversationId ? "" : "chat-pane--mobile-hidden",
          ].join(" ")}
        >
          <ChatWindow
            acceptGroupInvitation={handleAcceptInvitation}
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
            leaveCurrentGroup={handleLeaveGroup}
            loadMessageReadReceipts={loadMessageReadReceipts}
            onLoadMessageReactions={loadMessageReactions}
            onLoadMoreMessages={() => loadMoreMessages(conversationId)}
            onOpenPeople={() => setShowPeople(true)}
            onRetry={() => loadConversation(conversationId, { force: true })}
            onSendMessage={sendMessage}
            onToggleMessageReaction={toggleMessageReaction}
            onToggleMessagePin={(message) => toggleMessagePin(conversationId, message)}
            onTypingChange={(typing) => sendTypingStatus(conversationId, typing)}
            readReceiptsByMessageId={readReceiptsByMessageId}
            reactionsByMessageId={reactionsByMessageId}
            rejectGroupInvitation={handleRejectInvitation}
            removeMemberFromGroup={removeMemberFromGroup}
            searchUsers={searchUsers}
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
          <div className="sheet-panel contact-sheet-panel ml-auto h-full border-l border-white/5 bg-[#111827] shadow-panel">
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
