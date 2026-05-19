import { ArrowLeft, MoreVertical, Search } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { formatDateSeparator, formatPresence } from "../../utils/formatters";
import ChatBackdrop from "../assets/ChatBackdrop";
import EmptyChatArtwork from "../assets/EmptyChatArtwork";
import Avatar from "../ui/Avatar";
import Composer from "./Composer";
import InteractiveEmptyState from "./InteractiveEmptyState";
import MessageBubble from "./MessageBubble";
import TypingIndicator from "./TypingIndicator";

function isSameId(left, right) {
  return left != null && right != null && String(left) === String(right);
}

function isSameMessageDay(left, right) {
  if (!left || !right) return false;

  return new Date(left).toDateString() === new Date(right).toDateString();
}

export default function ChatWindow({
  conversation,
  currentUser,
  error = "",
  hasMoreMessages = false,
  isLoading = false,
  isLoadingMoreMessages = false,
  isSending = false,
  isTyping,
  onDeleteMessage,
  onEditMessage,
  onLoadMessageReactions,
  onLoadMoreMessages,
  onSendMessage,
  onToggleMessageReaction,
  onToggleMessagePin,
  onTypingChange,
  reactionsByMessageId = {},
  sendError = "",
}) {
  const bottomRef = useRef(null);
  const [editingMessage, setEditingMessage] = useState(null);
  const [replyToMessage, setReplyToMessage] = useState(null);
  const messageReactionKey = conversation?.messages.map((message) => message.id).join("|") ?? "";

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [conversation?.messages.length, isTyping]);

  useEffect(() => {
    if (!conversation || !onLoadMessageReactions) return;

    conversation.messages.forEach((message) => {
      onLoadMessageReactions(message.id);
    });
  }, [conversation?.id, messageReactionKey, onLoadMessageReactions]);

  useEffect(() => {
    setEditingMessage(null);
    setReplyToMessage(null);
  }, [conversation?.id]);

  if (!conversation && isLoading) {
    return (
      <section className="relative flex min-h-0 flex-1 items-center justify-center overflow-hidden bg-[#0e1621] p-6">
        <ChatBackdrop />
        <div className="relative flex animate-scale-in items-center gap-3 rounded-xl border border-white/5 bg-[#17212b]/90 px-5 py-4 text-sm font-medium text-slate-200 shadow-panel backdrop-blur">
          <span className="relative flex h-2.5 w-2.5">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-[#2aabee] opacity-75" />
            <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-[#2aabee]" />
          </span>
          Loading messages...
        </div>
      </section>
    );
  }

  if (!conversation) {
    return (
      <section className="relative flex min-h-0 flex-1 items-center justify-center overflow-hidden bg-[#0e1621] p-6">
        <ChatBackdrop />
        <InteractiveEmptyState />
      </section>
    );
  }

  const participant = conversation.otherParticipant;

  async function handleComposerSend(content) {
    if (editingMessage) {
      const updated = await onEditMessage?.(editingMessage.id, content);
      if (updated) {
        setEditingMessage(null);
      }
      return updated;
    }

    const sent = await onSendMessage(conversation.id, content, {
      replyToMessageId: replyToMessage?.id ?? null,
    });

    if (sent) {
      setReplyToMessage(null);
    }

    return sent;
  }

  async function handleDeleteMessage(message) {
    if (!window.confirm("Delete this message?")) return;

    const deleted = await onDeleteMessage?.(message.id);
    if (deleted && editingMessage?.id === message.id) {
      setEditingMessage(null);
    }
    if (deleted && replyToMessage?.id === message.id) {
      setReplyToMessage(null);
    }
  }

  return (
    <section className="relative flex min-h-0 flex-1 flex-col overflow-hidden bg-[#0e1621]">
      <ChatBackdrop />

      <header className="relative z-10 flex animate-fade-in items-center justify-between gap-3 border-b border-black/30 bg-[#17212b]/95 px-4 py-2.5 backdrop-blur">
        <div className="flex min-w-0 items-center gap-3">
          <Link
            to="/chat"
            className="press flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-slate-300 hover:bg-white/10 hover:text-white md:hidden"
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
            <p
              className={[
                "truncate text-xs transition-colors duration-300",
                participant?.presence?.isOnline ? "text-[#6ab7ee]" : "text-slate-400",
              ].join(" ")}
            >
              {formatPresence(participant?.presence)}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1">
          <HeaderAction icon={Search} label="Search" />
          <HeaderAction icon={MoreVertical} label="More" />
        </div>
      </header>

      <div className="relative z-[1] min-h-0 flex-1 overflow-y-auto px-2 py-4 sm:px-5">
        <div className="mx-auto flex w-full max-w-5xl flex-col">
          {hasMoreMessages ? (
            <div className="flex justify-center">
              <button
                type="button"
                onClick={onLoadMoreMessages}
                disabled={isLoadingMoreMessages}
                className="press lift rounded-full border border-white/5 bg-[#17212b]/90 px-4 py-2 text-sm font-medium text-slate-200 shadow-panel-soft backdrop-blur hover:bg-[#202b36] disabled:cursor-not-allowed disabled:text-slate-500"
              >
                {isLoadingMoreMessages ? "Loading..." : "Load earlier messages"}
              </button>
            </div>
          ) : null}

          {error ? (
            <div className="mx-auto max-w-sm animate-scale-in rounded-xl border border-rose-400/25 bg-rose-400/10 p-3 text-center text-sm leading-5 text-rose-100 backdrop-blur">
              {error}
            </div>
          ) : null}

          {conversation.messages.length === 0 ? (
            <div className="mx-auto mt-12 max-w-sm animate-scale-in rounded-2xl border border-white/5 bg-[#17212b]/90 p-6 text-center shadow-panel backdrop-blur">
              <EmptyChatArtwork compact />
              <Avatar user={participant} size="xl" showStatus />
              <h3 className="mt-5 text-lg font-semibold text-white">{participant?.displayName}</h3>
              <p className="mt-2 text-sm leading-6 text-slate-400">
                This direct conversation is ready.
              </p>
            </div>
          ) : (
            conversation.messages.map((message, index) => {
              const previousMessage = conversation.messages[index - 1];
              const nextMessage = conversation.messages[index + 1];
              const isOwn = isSameId(message.senderId, currentUser.id);
              const startsDay = !isSameMessageDay(previousMessage?.createdAt, message.createdAt);
              const endsDay = !isSameMessageDay(message.createdAt, nextMessage?.createdAt);
              const startsCluster =
                startsDay ||
                !previousMessage ||
                !isSameId(previousMessage.senderId, message.senderId);
              const endsCluster =
                endsDay ||
                !nextMessage ||
                !isSameId(nextMessage.senderId, message.senderId);
              return (
                <div key={message.id} className={startsDay ? "mt-3 first:mt-0" : ""}>
                  {startsDay ? (
                    <div className="mb-3 flex justify-center">
                      <span className="rounded-full bg-[#1c2a38]/95 px-4 py-1.5 text-sm font-semibold text-white shadow-panel-soft">
                        {formatDateSeparator(message.createdAt)}
                      </span>
                    </div>
                  ) : null}
                  <MessageBubble
                    message={message}
                    reactions={reactionsByMessageId[String(message.id)] ?? []}
                    isOwn={isOwn}
                    startsCluster={startsCluster}
                    endsCluster={endsCluster}
                    onDelete={() => handleDeleteMessage(message)}
                    onEdit={() => setEditingMessage(message)}
                    onReply={() => {
                      setEditingMessage(null);
                      setReplyToMessage(message);
                    }}
                    onToggleReaction={(emoji) => onToggleMessageReaction?.(message, emoji)}
                    onTogglePin={onToggleMessagePin}
                  />
                </div>
              );
            })
          )}

          {isTyping ? <TypingIndicator user={participant} /> : null}
          {sendError ? (
            <div className="ml-auto max-w-sm animate-scale-in rounded-xl border border-rose-400/25 bg-rose-400/10 p-3 text-sm leading-5 text-rose-100 backdrop-blur">
              {sendError}
            </div>
          ) : null}
          <div ref={bottomRef} />
        </div>
      </div>

      <Composer
        disabled={isSending}
        editingMessage={editingMessage}
        onCancelEdit={() => setEditingMessage(null)}
        onCancelReply={() => setReplyToMessage(null)}
        onTypingChange={onTypingChange}
        onSend={handleComposerSend}
        replyToMessage={replyToMessage}
      />
    </section>
  );
}

function HeaderAction({ icon: Icon, label, className = "flex" }) {
  return (
    <button
      type="button"
      className={`${className} press h-10 w-10 items-center justify-center rounded-full text-slate-300 hover:bg-white/10 hover:text-white`}
      title={label}
    >
      <Icon size={18} />
    </button>
  );
}
