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
    contacts,
    conversationSummaries,
    currentUser,
    getConversationById,
    markConversationRead,
    sendMessage,
    startConversation,
    stats,
    typingByConversation,
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
    if (conversationId) {
      markConversationRead(conversationId);
    }
  }, [conversationId]);

  function handleStartConversation(contact) {
    const nextConversationId = startConversation(contact.id);
    setShowPeople(false);
    navigate(`/chat/${nextConversationId}`);
  }

  return (
    <div className="h-screen overflow-hidden bg-[#0e1621] text-white">
      <main className="mx-auto flex h-full max-w-[1480px] border-x border-black/20 bg-[#0e1621]">
        <div className={`${conversationId ? "hidden md:flex" : "flex"} min-h-0 w-full md:w-[380px]`}>
          <ConversationList
            conversations={filteredConversations}
            currentUser={currentUser}
            query={query}
            onOpenPeople={() => setShowPeople(true)}
            onQueryChange={setQuery}
            selectedId={conversationId}
            stats={stats}
          />
        </div>

        <div className={`${conversationId ? "flex" : "hidden md:flex"} min-w-0 flex-1`}>
          <ChatWindow
            conversation={selectedConversation}
            currentUser={currentUser}
            isTyping={Boolean(conversationId && typingByConversation[conversationId])}
            onSendMessage={sendMessage}
          />
        </div>
      </main>

      {showPeople ? (
        <div className="fixed inset-0 z-50 bg-black/55 backdrop-blur-sm">
          <div className="ml-auto h-full w-full max-w-md border-l border-black/30 bg-[#17212b] shadow-panel">
            <ContactPanel
              contacts={contacts}
              onClose={() => setShowPeople(false)}
              onStartConversation={handleStartConversation}
            />
          </div>
        </div>
      ) : null}
    </div>
  );
}
