import { ArrowLeft, MoreVertical, Pin, Search, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { formatDateSeparator, formatPresence } from "../../utils/formatters";
import { getPinnedPreview, isSameId, isSameMessageDay } from "../../utils/chat";
import ChatBackdrop from "../assets/ChatBackdrop";
import EmptyChatArtwork from "../assets/EmptyChatArtwork";
import Avatar from "../ui/Avatar";
import Composer from "./Composer";
import InteractiveEmptyState from "./InteractiveEmptyState";
import MessageBubble from "./MessageBubble";
import TypingIndicator from "./TypingIndicator";

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
  const messageRefs = useRef(new Map());
  const [editingMessage, setEditingMessage] = useState(null);
  const [isPinsOpen, setIsPinsOpen] = useState(false);
  const [replyToMessage, setReplyToMessage] = useState(null);
  const messageReactionKey = conversation?.messages.map((message) => message.id).join("|") ?? "";
  const pinnedMessages = conversation?.pinnedMessages ?? [];

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
    setIsPinsOpen(false);
    setReplyToMessage(null);
  }, [conversation?.id]);

  if (!conversation && isLoading) {
    return (
      <section className="relative flex min-h-0 flex-1 items-center justify-center overflow-hidden bg-[#0a0f1a] p-6">
        <ChatBackdrop />
        <div className="relative flex animate-scale-in items-center gap-3 rounded-2xl border border-white/5 bg-[#111827]/90 px-5 py-4 text-sm font-medium text-slate-200 shadow-panel backdrop-blur">
          <span className="relative flex h-2.5 w-2.5">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-indigo-500 opacity-75" />
            <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-indigo-500" />
          </span>
          Loading messages...
        </div>
      </section>
    );
  }

  if (!conversation) {
    return (
      <section className="relative flex min-h-0 flex-1 items-center justify-center overflow-hidden bg-[#0a0f1a] p-6">
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

  function scrollToMessage(messageId) {
    const node = messageRefs.current.get(String(messageId));

    if (node) {
      node.scrollIntoView({ behavior: "smooth", block: "center" });
    }
  }

  return (
    <section className="relative flex min-h-0 flex-1 flex-col overflow-hidden bg-[#0a0f1a]">
      <ChatBackdrop />

      <header className="relative z-10 flex animate-fade-in items-center justify-between gap-3 border-b border-white/[0.04] bg-[#111827]/95 px-4 py-3 backdrop-blur-xl">
        <div className="flex min-w-0 items-center gap-3">
          <Link
            to="/chat"
            className="press flex h-10 w-10 shrink-0 items-center justify-center rounded-xl text-slate-400 hover:bg-white/5 hover:text-white md:hidden"
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
                participant?.presence?.isOnline ? "text-indigo-400" : "text-slate-500",
              ].join(" ")}
            >
              {formatPresence(participant?.presence)}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1">
          <HeaderAction icon={Search} label="Search" />
          <HeaderAction
            icon={MoreVertical}
            label="Pinned messages"
            onClick={() => setIsPinsOpen((open) => !open)}
          />
        </div>
      </header>

      {pinnedMessages.length > 0 ? (
        <div className="relative z-10 border-b border-white/[0.04] bg-[#111827]/95 px-4 py-2 backdrop-blur-xl">
          <button
            type="button"
            onClick={() => setIsPinsOpen((open) => !open)}
            className="press flex w-full items-center gap-3 rounded-xl px-2 py-1.5 text-left hover:bg-white/5"
          >
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-indigo-500/15 text-indigo-400">
              <Pin size={15} />
            </span>
            <span className="min-w-0 flex-1">
              <span className="block text-xs font-semibold uppercase tracking-wider text-indigo-400">
                Pinned
              </span>
              <span className="block truncate text-sm text-slate-300">
                {getPinnedPreview(pinnedMessages[0]?.message)}
              </span>
            </span>
            <span className="rounded-full bg-[#1e293b] px-2 py-0.5 text-xs font-semibold text-slate-300">
              {pinnedMessages.length}
            </span>
          </button>

          {isPinsOpen ? (
            <div className="absolute left-4 right-4 top-full z-30 mt-2 overflow-hidden rounded-2xl border border-white/5 bg-[#1f2937]/98 py-2 shadow-panel backdrop-blur-xl">
              <div className="flex items-center justify-between gap-3 border-b border-white/5 px-4 pb-2">
                <p className="text-sm font-semibold text-white">Pinned messages</p>
                <button
                  type="button"
                  onClick={() => setIsPinsOpen(false)}
                  className="press flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-white/5 hover:text-white"
                  title="Close"
                >
                  <X size={16} />
                </button>
              </div>
              <div className="max-h-72 overflow-y-auto py-1">
                {pinnedMessages.map((pin) => (
                  <div
                    key={pin.pinId ?? pin.message.id}
                    className="flex items-center gap-2 px-4 py-2.5 transition-colors hover:bg-white/[0.03]"
                  >
                    <button
                      type="button"
                      onClick={() => scrollToMessage(pin.message.id)}
                      className="min-w-0 flex-1 text-left"
                    >
                      <p className="truncate text-sm font-medium text-slate-100">
                        {getPinnedPreview(pin.message)}
                      </p>
                      <p className="mt-0.5 truncate text-xs text-slate-500">
                        Pinned by {pin.pinnedBy?.displayName ?? pin.pinnedBy?.username ?? "participant"}
                      </p>
                    </button>
                    <button
                      type="button"
                      onClick={() => onToggleMessagePin?.(pin.message)}
                      className="press flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-slate-400 hover:bg-white/5 hover:text-indigo-400"
                      title="Unpin message"
                    >
                      <Pin size={14} />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          ) : null}
        </div>
      ) : null}

      <div className="relative z-[1] min-h-0 flex-1 overflow-y-auto px-2 py-4 sm:px-5">
        <div className="mx-auto flex w-full max-w-5xl flex-col">
          {hasMoreMessages ? (
            <div className="flex justify-center">
              <button
                type="button"
                onClick={onLoadMoreMessages}
                disabled={isLoadingMoreMessages}
                className="press lift rounded-full border border-white/5 bg-[#111827]/90 px-4 py-2 text-sm font-medium text-slate-300 shadow-panel-soft backdrop-blur hover:bg-[#1f2937] disabled:cursor-not-allowed disabled:text-slate-500"
              >
                {isLoadingMoreMessages ? "Loading..." : "Load earlier messages"}
              </button>
            </div>
          ) : null}

          {error ? (
            <div className="mx-auto max-w-sm animate-scale-in rounded-xl border border-rose-400/20 bg-rose-400/10 p-3 text-center text-sm leading-5 text-rose-200 backdrop-blur">
              {error}
            </div>
          ) : null}

          {conversation.messages.length === 0 ? (
            <div className="mx-auto mt-12 max-w-sm animate-scale-in rounded-2xl border border-white/5 bg-[#111827]/90 p-6 text-center shadow-panel backdrop-blur">
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
                <div
                  key={message.id}
                  ref={(node) => {
                    if (node) {
                      messageRefs.current.set(String(message.id), node);
                    } else {
                      messageRefs.current.delete(String(message.id));
                    }
                  }}
                  className={startsDay ? "mt-3 first:mt-0" : ""}
                >
                  {startsDay ? (
                    <div className="mb-3 flex justify-center">
                      <span className="rounded-full border border-white/5 bg-[#111827]/95 px-4 py-1.5 text-xs font-semibold text-slate-300 shadow-panel-soft backdrop-blur">
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
            <div className="ml-auto max-w-sm animate-scale-in rounded-xl border border-rose-400/20 bg-rose-400/10 p-3 text-sm leading-5 text-rose-200 backdrop-blur">
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

function HeaderAction({ icon: Icon, label, className = "flex", onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`${className} press h-10 w-10 items-center justify-center rounded-xl text-slate-400 hover:bg-white/5 hover:text-white`}
      title={label}
    >
      <Icon size={18} />
    </button>
  );
}
