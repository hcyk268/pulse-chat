import { ArrowLeft, MoreVertical, Search } from "lucide-react";
import { useEffect, useRef } from "react";
import { Link } from "react-router-dom";
import { formatPresence } from "../../utils/formatters";
import ChatBackdrop from "../assets/ChatBackdrop";
import EmptyChatArtwork from "../assets/EmptyChatArtwork";
import Avatar from "../ui/Avatar";
import Composer from "./Composer";
import MessageBubble from "./MessageBubble";
import TypingIndicator from "./TypingIndicator";

export default function ChatWindow({
  conversation,
  currentUser,
  isTyping,
  onSendMessage,
}) {
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [conversation?.messages.length, isTyping]);

  if (!conversation) {
    return (
      <section className="relative flex min-h-0 flex-1 items-center justify-center overflow-hidden bg-[#0e1621] p-6">
        <ChatBackdrop />
        <div className="relative max-w-lg rounded-lg border border-black/25 bg-[#17212b]/90 p-8 text-center shadow-panel">
          <EmptyChatArtwork />
          <h2 className="mt-4 text-2xl font-semibold text-white">No conversation selected</h2>
          <p className="mt-2 text-sm leading-6 text-slate-400">
            Open a recent chat or start a new direct conversation from the people panel.
          </p>
        </div>
      </section>
    );
  }

  const participant = conversation.otherParticipant;

  return (
    <section className="relative flex min-h-0 flex-1 flex-col overflow-hidden bg-[#0e1621]">
      <ChatBackdrop />
      <header className="relative flex items-center justify-between gap-3 border-b border-black/25 bg-[#17212b] px-4 py-2.5">
        <div className="flex min-w-0 items-center gap-3">
          <Link
            to="/chat"
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-slate-300 transition hover:bg-white/10 md:hidden"
            title="Back"
          >
            <ArrowLeft size={19} />
          </Link>
          <Avatar user={participant} size="md" showStatus />
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <h2 className="truncate text-base font-semibold text-white">
                {participant?.displayName}
              </h2>
            </div>
            <p className="truncate text-xs text-[#6ab7ee]">{formatPresence(participant?.presence)}</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <HeaderAction icon={Search} label="Search" />
          <HeaderAction icon={MoreVertical} label="More" />
        </div>
      </header>

      <div className="relative min-h-0 flex-1 overflow-y-auto px-4 py-5 sm:px-6">
        <div className="mx-auto flex max-w-4xl flex-col gap-5">
          {conversation.messages.length === 0 ? (
            <div className="mx-auto mt-12 max-w-sm rounded-lg border border-black/25 bg-[#17212b]/90 p-6 text-center shadow-panel">
              <EmptyChatArtwork compact />
              <Avatar user={participant} size="xl" showStatus />
              <h3 className="mt-5 text-lg font-semibold text-white">{participant?.displayName}</h3>
              <p className="mt-2 text-sm leading-6 text-slate-400">
                This direct conversation is ready.
              </p>
            </div>
          ) : (
            conversation.messages.map((message) => {
              const isOwn = message.senderId === currentUser.id;
              const sender = isOwn ? currentUser : participant;

              return (
                <MessageBubble
                  key={message.id}
                  message={message}
                  sender={sender}
                  isOwn={isOwn}
                />
              );
            })
          )}

          {isTyping ? <TypingIndicator user={participant} /> : null}
          <div ref={bottomRef} />
        </div>
      </div>

      <Composer onSend={(content) => onSendMessage(conversation.id, content)} />
    </section>
  );
}

function HeaderAction({ icon: Icon, label, className = "flex" }) {
  return (
    <button
      type="button"
      className={`${className} h-10 w-10 items-center justify-center rounded-full text-slate-300 transition hover:bg-white/10 hover:text-white`}
      title={label}
    >
      <Icon size={18} />
    </button>
  );
}
