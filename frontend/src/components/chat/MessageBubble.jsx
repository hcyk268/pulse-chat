import { Check, CheckCheck, Clock3, Pencil, Pin, Reply, SmilePlus, Trash2 } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { formatChatTime } from "../../utils/formatters";

const REACTION_OPTIONS = [
  { emoji: "LIKE", symbol: "\u{1F44D}", label: "Like" },
  { emoji: "LOVE", symbol: "\u2764\uFE0F", label: "Love" },
  { emoji: "HAHA", symbol: "\u{1F602}", label: "Haha" },
  { emoji: "WOW", symbol: "\u{1F62E}", label: "Wow" },
  { emoji: "SAD", symbol: "\u{1F622}", label: "Sad" },
  { emoji: "ANGRY", symbol: "\u{1F621}", label: "Angry" },
];

function getReactionOption(emoji) {
  return REACTION_OPTIONS.find((option) => option.emoji === emoji) ?? {
    emoji,
    symbol: emoji,
    label: emoji,
  };
}

function StatusIcon({ status }) {
  if (status === "READ") {
    return <CheckCheck size={14} className="text-cyan-200 transition-colors duration-200" />;
  }

  if (status === "DELIVERED") {
    return <CheckCheck size={14} className="text-sky-100/80 transition-colors duration-200" />;
  }

  if (status === "SENT") {
    return <Check size={14} className="text-sky-100/75 transition-colors duration-200" />;
  }

  return <Clock3 size={14} className="text-slate-400 transition-colors duration-200" />;
}

function getMessageText(message) {
  if (!message) return "";
  if (message.deletedAt) return "Message deleted";

  return message.content ?? "";
}

export default function MessageBubble({
  message,
  isOwn,
  startsCluster = true,
  endsCluster = true,
  onDelete,
  onEdit,
  onReply,
  onTogglePin,
  onToggleReaction,
  reactions = [],
}) {
  const [isReactionPickerOpen, setIsReactionPickerOpen] = useState(false);
  const reactionPickerRef = useRef(null);
  const pinLabel = message.pinned ? "Unpin message" : "Pin message";

  useEffect(() => {
    if (!isReactionPickerOpen) return undefined;

    function handlePointerDown(event) {
      if (!reactionPickerRef.current?.contains(event.target)) {
        setIsReactionPickerOpen(false);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, [isReactionPickerOpen]);

  function handleToggleReaction(emoji) {
    onToggleReaction?.(emoji);
    setIsReactionPickerOpen(false);
  }

  const isDeleted = Boolean(message.deletedAt);
  const canActOnMessage = !isDeleted;

  return (
    <div
      className={[
        "group flex px-0.5",
        startsCluster ? "mt-2.5" : "mt-1",
        isOwn ? "justify-end animate-enter-bubble-right" : "justify-start animate-enter-bubble-left",
      ].join(" ")}
    >
      <div
        className={[
          "relative flex max-w-[82%] flex-col sm:max-w-[68%] lg:max-w-[58%]",
          isOwn ? "items-end" : "items-start",
        ].join(" ")}
      >
        <div
          className={[
            "message-bubble relative px-3.5 py-2 text-[15px] leading-5 text-white shadow-none transition-colors duration-200",
            isOwn ? "message-bubble-own bg-[#315f8f]" : "message-bubble-peer bg-[#172636]",
            endsCluster && isOwn ? "message-bubble-tail-own rounded-br-md" : "rounded-br-2xl",
            endsCluster && !isOwn ? "message-bubble-tail-peer rounded-bl-md" : "rounded-bl-2xl",
            "rounded-t-2xl",
          ].join(" ")}
        >
          {message.replyTo ? (
            <div
              className={[
                "relative z-[1] mb-1.5 rounded-lg border-l-2 px-2 py-1",
                isOwn
                  ? "border-cyan-200/70 bg-white/10"
                  : "border-[#6ab7ee] bg-black/10",
              ].join(" ")}
            >
              <p className="truncate text-xs font-semibold text-cyan-100">
                {message.replyTo.sender?.displayName ?? message.replyTo.sender?.username ?? "Message"}
              </p>
              <p className="truncate text-xs text-slate-300">
                {getMessageText(message.replyTo)}
              </p>
            </div>
          ) : null}
          <span
            className={[
              "relative z-[1] whitespace-pre-wrap break-words",
              isDeleted ? "italic text-slate-300" : "",
            ].join(" ")}
          >
            {getMessageText(message)}
          </span>
          <span
            className={[
              "relative z-[1] ml-2 inline-flex translate-y-[2px] items-center gap-1 whitespace-nowrap text-[12px] leading-none",
              isOwn ? "text-cyan-100/70" : "text-slate-400",
            ].join(" ")}
          >
            {message.pinned && !isDeleted ? (
              <Pin size={11} className="text-[#6ab7ee]" aria-label="Pinned" />
            ) : null}
            {message.editedAt && !isDeleted ? <span>edited</span> : null}
            {formatChatTime(message.createdAt)}
            {isOwn ? <StatusIcon status={message.status} /> : null}
          </span>
        </div>

        {!isDeleted && reactions.length > 0 ? (
          <div
            className={[
              "mt-1 flex flex-wrap gap-1",
              isOwn ? "justify-end" : "justify-start",
            ].join(" ")}
          >
            {reactions.map((reaction) => {
              const option = getReactionOption(reaction.emoji);

              return (
                <button
                  key={reaction.emoji}
                  type="button"
                  onClick={() => handleToggleReaction(reaction.emoji)}
                  className={[
                    "press flex h-6 items-center gap-1 rounded-full border px-2 text-xs font-medium shadow-panel-soft transition-colors duration-200",
                    reaction.reactedByMe
                      ? "border-[#6ab7ee]/45 bg-[#2aabee]/20 text-cyan-50"
                      : "border-white/5 bg-[#17212b]/95 text-slate-300 hover:bg-[#202b36]",
                  ].join(" ")}
                  title={option.label}
                >
                  <span>{option.symbol}</span>
                  <span>{reaction.count}</span>
                </button>
              );
            })}
          </div>
        ) : null}

        <div
          className={[
            "absolute top-1/2 z-20 flex -translate-y-1/2 items-center gap-1 opacity-0 transition-opacity duration-150 group-hover:opacity-100 focus-within:opacity-100",
            isOwn ? "right-full mr-2" : "left-full ml-2",
          ].join(" ")}
        >
          {canActOnMessage && onReply ? (
            <button
              type="button"
              onClick={onReply}
              className="press flex h-7 w-7 items-center justify-center rounded-full bg-[#172636]/95 text-slate-400 shadow-panel-soft transition-all duration-200 hover:bg-[#203246] hover:text-[#6ab7ee]"
              title="Reply"
            >
              <Reply size={14} />
            </button>
          ) : null}
          {canActOnMessage && onToggleReaction ? (
            <div ref={reactionPickerRef} className="relative">
              <button
                type="button"
                onClick={() => setIsReactionPickerOpen((open) => !open)}
                className="press flex h-7 w-7 items-center justify-center rounded-full bg-[#172636]/95 text-slate-400 shadow-panel-soft transition-all duration-200 hover:bg-[#203246] hover:text-[#6ab7ee]"
                title="React"
              >
                <SmilePlus size={14} />
              </button>
              {isReactionPickerOpen ? (
                <div
                  className={[
                    "absolute bottom-8 z-20 flex gap-1 rounded-full border border-white/10 bg-[#202b36]/95 p-1 shadow-panel backdrop-blur",
                    isOwn ? "right-0" : "left-0",
                  ].join(" ")}
                >
                  {REACTION_OPTIONS.map((option) => (
                    <button
                      key={option.emoji}
                      type="button"
                      onClick={() => handleToggleReaction(option.emoji)}
                      className="press flex h-8 w-8 items-center justify-center rounded-full text-base hover:bg-white/10"
                      title={option.label}
                    >
                      {option.symbol}
                    </button>
                  ))}
                </div>
              ) : null}
            </div>
          ) : null}
          {canActOnMessage && onTogglePin ? (
            <button
              type="button"
              aria-pressed={Boolean(message.pinned)}
              onClick={() => onTogglePin(message)}
              className="press flex h-7 w-7 items-center justify-center rounded-full bg-[#172636]/95 text-slate-400 shadow-panel-soft transition-all duration-200 hover:bg-[#203246] hover:text-[#6ab7ee]"
              title={pinLabel}
            >
              <Pin size={14} />
            </button>
          ) : null}
          {canActOnMessage && isOwn && onEdit ? (
            <button
              type="button"
              onClick={onEdit}
              className="press flex h-7 w-7 items-center justify-center rounded-full bg-[#172636]/95 text-slate-400 shadow-panel-soft transition-all duration-200 hover:bg-[#203246] hover:text-[#6ab7ee]"
              title="Edit"
            >
              <Pencil size={14} />
            </button>
          ) : null}
          {canActOnMessage && isOwn && onDelete ? (
            <button
              type="button"
              onClick={onDelete}
              className="press flex h-7 w-7 items-center justify-center rounded-full bg-[#172636]/95 text-slate-400 shadow-panel-soft transition-all duration-200 hover:bg-rose-400/10 hover:text-rose-200"
              title="Delete"
            >
              <Trash2 size={14} />
            </button>
          ) : null}
        </div>
      </div>
    </div>
  );
}
