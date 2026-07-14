import { Check, CheckCheck, Clock3, Download, Eye, File, Image as ImageIcon, Pencil, Pin, Reply, SmilePlus, Trash2 } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { formatChatTime, formatFileSize } from "../../utils/formatters";
import Avatar from "../ui/Avatar";

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
    return <CheckCheck size={14} className="text-indigo-300 transition-colors duration-200" />;
  }

  if (status === "DELIVERED") {
    return <CheckCheck size={14} className="text-indigo-200/70 transition-colors duration-200" />;
  }

  if (status === "SENT") {
    return <Check size={14} className="text-indigo-200/60 transition-colors duration-200" />;
  }

  return <Clock3 size={14} className="text-slate-400 transition-colors duration-200" />;
}

function getMessageText(message) {
  if (!message) return "";
  if (message.deletedAt) return "Message deleted";

  return message.content ?? "";
}

function AttachmentList({ attachments = [], isOwn }) {
  if (attachments.length === 0) return null;

  return (
    <div className="relative z-[1] mt-2 space-y-2">
      {attachments.map((attachment) => {
        const isImage = attachment.contentType?.startsWith("image/");
        const Icon = isImage ? ImageIcon : File;
        const href = attachment.url || attachment.thumbnailUrl;

        return (
          <a
            key={`${attachment.objectKey}-${attachment.fileName}`}
            href={href}
            target="_blank"
            rel="noreferrer"
            className={[
              "block overflow-hidden rounded-xl border transition-colors",
              isOwn ? "border-white/10 bg-white/10 hover:bg-white/15" : "border-white/5 bg-[#111827]/70 hover:bg-[#111827]",
            ].join(" ")}
          >
            {isImage && href ? (
              <img
                src={attachment.thumbnailUrl || attachment.url}
                alt={attachment.fileName || "Attachment"}
                className="max-h-72 w-full object-cover"
                loading="lazy"
              />
            ) : null}
            <span className="flex items-center gap-3 px-3 py-2.5">
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-black/15 text-indigo-200">
                <Icon size={17} />
              </span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-medium text-white">
                  {attachment.fileName || "Attachment"}
                </span>
                <span className="block text-xs text-slate-400">
                  {formatFileSize(attachment.sizeBytes)}
                </span>
              </span>
              <Download size={16} className="shrink-0 text-slate-400" />
            </span>
          </a>
        );
      })}
    </div>
  );
}

export default function MessageBubble({
  message,
  isOwn,
  startsCluster = true,
  endsCluster = true,
  onDelete,
  onEdit,
  onReply,
  onShowReadReceipts,
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
  const text = getMessageText(message);

  return (
    <div
      className={[
        "group flex px-0.5",
        startsCluster ? "mt-3" : "mt-0.5",
        isOwn ? "justify-end animate-enter-bubble-right" : "justify-start animate-enter-bubble-left",
      ].join(" ")}
    >
      {!isOwn ? (
        <div className="mr-2 flex w-9 shrink-0 self-end">
          {endsCluster ? <Avatar user={message.sender} size="sm" /> : null}
        </div>
      ) : null}
      <div
        className={[
          "relative flex max-w-[82%] flex-col sm:max-w-[68%] lg:max-w-[58%]",
          isOwn ? "items-end" : "items-start",
        ].join(" ")}
      >
        <div
          className={[
            "message-bubble relative px-3.5 py-2.5 text-[15px] leading-[1.45] text-white transition-all duration-200",
            isOwn
              ? "message-bubble-own bg-gradient-to-br from-indigo-600/90 to-indigo-700/90 shadow-bubble-own"
              : "message-bubble-peer bg-[#1e293b] shadow-bubble-other",
            endsCluster && isOwn ? "message-bubble-tail-own rounded-br-md" : "rounded-br-2xl",
            endsCluster && !isOwn ? "message-bubble-tail-peer rounded-bl-md" : "rounded-bl-2xl",
            "rounded-t-2xl",
          ].join(" ")}
        >
          {message.replyTo ? (
            <div
              className={[
                "relative z-[1] mb-2 rounded-lg border-l-2 px-2.5 py-1.5",
                isOwn
                  ? "border-indigo-300/70 bg-white/10"
                  : "border-indigo-400 bg-indigo-500/10",
              ].join(" ")}
            >
              <p className="truncate text-xs font-semibold text-indigo-200">
                {message.replyTo.sender?.displayName ?? message.replyTo.sender?.username ?? "Message"}
              </p>
              <p className="truncate text-xs text-slate-300">
                {getMessageText(message.replyTo) || message.replyTo.attachments?.[0]?.fileName || "Attachment"}
              </p>
            </div>
          ) : null}
          {text ? (
            <span
              className={[
                "relative z-[1] whitespace-pre-wrap break-words",
                isDeleted ? "italic text-slate-400" : "",
              ].join(" ")}
            >
              {text}
            </span>
          ) : null}
          {!isDeleted ? <AttachmentList attachments={message.attachments} isOwn={isOwn} /> : null}
          <span
            className={[
              "relative z-[1] ml-2 inline-flex translate-y-[2px] items-center gap-1 whitespace-nowrap text-[11px] leading-none",
              isOwn ? "text-indigo-200/60" : "text-slate-500",
            ].join(" ")}
          >
            {message.pinned && !isDeleted ? (
              <Pin size={10} className="text-indigo-300" aria-label="Pinned" />
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
                    "press flex h-6 items-center gap-1 rounded-full border px-2 text-xs font-medium transition-all duration-200",
                    reaction.reactedByMe
                      ? "border-indigo-400/40 bg-indigo-500/20 text-indigo-100 shadow-[0_0_12px_rgba(99,102,241,0.15)]"
                      : "border-white/5 bg-[#1e293b]/95 text-slate-300 hover:bg-[#334155]",
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
            <ActionButton icon={Reply} label="Reply" onClick={onReply} />
          ) : null}
          {canActOnMessage && onToggleReaction ? (
            <div ref={reactionPickerRef} className="relative">
              <ActionButton
                icon={SmilePlus}
                label="React"
                onClick={() => setIsReactionPickerOpen((open) => !open)}
              />
              {isReactionPickerOpen ? (
                <div
                  className={[
                    "absolute bottom-9 z-20 flex gap-0.5 rounded-2xl border border-white/10 bg-[#1f2937]/98 p-1.5 shadow-panel backdrop-blur-xl",
                    isOwn ? "right-0" : "left-0",
                  ].join(" ")}
                >
                  {REACTION_OPTIONS.map((option) => (
                    <button
                      key={option.emoji}
                      type="button"
                      onClick={() => handleToggleReaction(option.emoji)}
                      className="press flex h-8 w-8 items-center justify-center rounded-full text-base transition-transform hover:scale-125 hover:bg-white/10"
                      title={option.label}
                    >
                      {option.symbol}
                    </button>
                  ))}
                </div>
              ) : null}
            </div>
          ) : null}
          {canActOnMessage && onShowReadReceipts ? (
            <ActionButton icon={Eye} label="Read receipts" onClick={() => onShowReadReceipts(message)} />
          ) : null}
          {canActOnMessage && onTogglePin ? (
            <ActionButton
              icon={Pin}
              label={pinLabel}
              onClick={() => onTogglePin(message)}
              active={Boolean(message.pinned)}
            />
          ) : null}
          {canActOnMessage && isOwn && onEdit && message.messageType !== "MEDIA" ? (
            <ActionButton icon={Pencil} label="Edit" onClick={onEdit} />
          ) : null}
          {canActOnMessage && isOwn && onDelete ? (
            <ActionButton icon={Trash2} label="Delete" onClick={onDelete} variant="danger" />
          ) : null}
        </div>
      </div>
    </div>
  );
}

function ActionButton({ icon: Icon, label, onClick, variant = "default", active = false }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={[
        "press flex h-7 w-7 items-center justify-center rounded-lg shadow-panel-soft transition-all duration-200",
        variant === "danger"
          ? "bg-[#1e293b]/95 text-slate-400 hover:bg-rose-500/15 hover:text-rose-300"
          : "bg-[#1e293b]/95 text-slate-400 hover:bg-indigo-500/15 hover:text-indigo-300",
      ].join(" ")}
      title={label}
    >
      <Icon size={14} />
    </button>
  );
}
