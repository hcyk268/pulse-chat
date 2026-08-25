import { AlertCircle, Check, CheckCheck, Clock, FileText, Languages, LockKeyhole, MessageSquareReply, MoreHorizontal, Paperclip, Pencil, Pin, Search, Send, ShieldCheck, Smile, Sparkles, Trash2, Users, WandSparkles, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { askSmartAssistant, getApiErrorMessage, moderateCommunityContent } from "../../api/aiApi.js";
import Avatar from "../../components/shared/Avatar";
import { classNames } from "../../components/shared/utils";
import { useLatestRef } from "../../hooks/useLatestRef.js";
import TypingIndicator from "./TypingIndicator";
import { useTranslation } from "../../i18n/useTranslation.js";
import { formatChatTime, formatDayLabel } from "../../utils/formatters";
import { isSameId, isSameMessageDay } from "../../utils/chat.js";
import {
  ATTACHMENT_ACCEPT,
  MAX_ATTACHMENTS_PER_MESSAGE,
  mergeAttachmentFiles,
} from "../../domain/chat/attachmentFiles.js";
import SelectedAttachmentPreview from "./SelectedAttachmentPreview.jsx";

function renderHighlightedText(text, searchQuery) {
  if (!searchQuery || !text) return text;
  const escaped = searchQuery.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const parts = text.split(new RegExp(`(${escaped})`, "gi"));
  return parts.map((part, i) =>
    part.toLowerCase() === searchQuery.toLowerCase() ? (
      <mark className="message-text-match" key={String(i)}>
        {part}
      </mark>
    ) : (
      part
    ),
  );
}

const TYPING_IDLE_DELAY = 2000;
// Attachment limits and validation are shared with tests through the domain helper.
const FILE_SIZE_UNITS = ["B", "KB", "MB", "GB"];
const REACTION_SYMBOLS = {
  LIKE: "+1",
  LOVE: "<3",
  HAHA: ":D",
  WOW: "!?",
  SAD: ":(",
  ANGRY: "!!",
};
const REACTION_TYPES = Object.keys(REACTION_SYMBOLS);

function formatFileSize(sizeBytes) {
  const size = Number(sizeBytes);
  if (!Number.isFinite(size) || size <= 0) return "";

  let value = size;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < FILE_SIZE_UNITS.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }

  const precision = value >= 10 || unitIndex === 0 ? 0 : 1;
  return `${value.toFixed(precision)} ${FILE_SIZE_UNITS[unitIndex]}`;
}

function MessageStatus({ status, t }) {
  if (status === "FAILED") {
    return (
      <span className="message__status message__status--failed">
        <AlertCircle size={13} /> {t("chat.status.failed")}
      </span>
    );
  }

  if (status === "PENDING") {
    return (
      <span className="message__status" title={t("chat.status.pending")}>
        <Clock size={13} />
      </span>
    );
  }

  if (status === "READ") {
    return (
      <span className="message__status message__status--read" title={t("chat.status.read")}>
        <CheckCheck size={13} />
      </span>
    );
  }

  if (status === "DELIVERED") {
    return (
      <span className="message__status" title={t("chat.status.delivered")}>
        <CheckCheck size={13} />
      </span>
    );
  }

  return (
    <span className="message__status" title={t("chat.status.sent")}>
      <Check size={13} />
    </span>
  );
}

function getAttachmentUrl(attachment) {
  return attachment.url || attachment.publicUrl || attachment.previewUrl || "";
}

function isImageAttachment(attachment) {
  return (attachment.contentType || "").toLowerCase().startsWith("image/");
}

function isVideoAttachment(attachment) {
  return (attachment.contentType || "").toLowerCase().startsWith("video/");
}

function MessageAttachments({ attachments = [], t }) {
  if (!attachments.length) return null;

  return (
    <div className="message-attachments">
      {attachments.map((attachment, index) => {
        const fileName =
          attachment.fileName || attachment.name || attachment.objectKey || t("chat.attachmentFile");
        const fileSize = formatFileSize(attachment.sizeBytes ?? attachment.size);
        const href = getAttachmentUrl(attachment);
        const key = attachment.objectKey || href || `${fileName}-${index}`;

        if (href && isImageAttachment(attachment)) {
          return (
            <a
              className="message-media message-media--image"
              href={href}
              key={key}
              target="_blank"
              rel="noreferrer"
            >
              <img src={attachment.thumbnailUrl || href} alt={fileName} loading="lazy" />
              <span>{fileName}</span>
            </a>
          );
        }

        if (href && isVideoAttachment(attachment)) {
          return (
            <div className="message-media message-media--video" key={key}>
              <video src={href} poster={attachment.thumbnailUrl || undefined} controls preload="metadata" />
              <span>{fileName}</span>
            </div>
          );
        }

        const content = (
          <>
            <FileText size={16} aria-hidden="true" />
            <span>{fileName}</span>
            {fileSize ? <small>{fileSize}</small> : null}
          </>
        );

        return href ? (
          <a
            className="message-attachment"
            href={href}
            key={key}
            target="_blank"
            rel="noreferrer"
          >
            {content}
          </a>
        ) : (
          <span className="message-attachment" key={key}>
            {content}
          </span>
        );
      })}
    </div>
  );
}

function buildRows(messages, dayLabels) {
  const rows = [];

  messages.forEach((message, index) => {
    const previous = messages[index - 1];

    if (!previous || !isSameMessageDay(previous.createdAt, message.createdAt)) {
      rows.push({
        type: "separator",
        key: `day-${message.id ?? message.clientMessageId ?? index}`,
        label: formatDayLabel(message.createdAt, dayLabels),
      });
    }

    rows.push({
      type: "message",
      key: `message-${message.id ?? message.clientMessageId ?? index}`,
      message,
    });
  });

  return rows;
}

// File selection is normalized by mergeAttachmentFiles.

export default function MessageStream({
  currentUserId = null,
  disabled = false,
  emptyLabel,
  hasOlderMessages = false,
  initialDraft = "",
  loading = false,
  loadingOlder = false,
  messages = [],
  participants = [],
  conversationId = null,
  communitySlug = "",
  onLoadOlder,
  onCancelUpload,
  onSend,
  onTyping,
  onPin,
  onUnpin,
  onReact,
  onEdit,
  onDelete,
  onGuestAction,
  onReadReceipts,
  placeholder,
  sending = false,
  typingUsers = [],
  uploadProgress = null,
  readOnly = false,
}) {
  const { t, locale } = useTranslation();
  const [draft, setDraft] = useState(initialDraft);
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [fileError, setFileError] = useState("");
  const [editingMessageId, setEditingMessageId] = useState(null);
  const [editDraft, setEditDraft] = useState("");
  const [composerAi, setComposerAi] = useState({ answer: "", error: "", loading: false });
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [mentionState, setMentionState] = useState({ active: false, query: "", index: 0, startPos: 0 });
  const inputRef = useRef(null);
  const streamRef = useRef(null);
  const fileInputRef = useRef(null);
  const onTypingRef = useLatestRef(onTyping);
  const typingStateRef = useRef({ active: false, timer: null });
  const stickToBottomRef = useRef(true);
  const initialScrollRef = useRef(false);
  const previousScrollKeyRef = useRef("");
  const dayLabels = useMemo(
    () => ({ today: t("chat.day.today"), yesterday: t("chat.day.yesterday") }),
    [t],
  );

  const mentionCandidates = useMemo(() => {
    const map = new Map();
    if (Array.isArray(participants)) {
      participants.forEach((p) => {
        const user = p?.user || p;
        if (user?.id && !isSameId(user.id, currentUserId)) {
          map.set(user.id, user);
        }
      });
    }
    messages.forEach((m) => {
      if (m.sender?.id && !isSameId(m.sender.id, currentUserId)) {
        map.set(m.sender.id, m.sender);
      }
    });
    return Array.from(map.values());
  }, [currentUserId, messages, participants]);

  const filteredMentions = useMemo(() => {
    if (!mentionState.active) return [];
    const q = mentionState.query.toLowerCase();
    return mentionCandidates.filter((u) => {
      const uname = (u.username || "").toLowerCase();
      const dname = (u.displayName || "").toLowerCase();
      return uname.includes(q) || dname.includes(q);
    });
  }, [mentionCandidates, mentionState.active, mentionState.query]);

  const normalizedSearch = searchQuery.trim().toLowerCase();
  const matchingMessages = useMemo(() => {
    if (!normalizedSearch) return messages;
    return messages.filter((m) => (m.content || "").toLowerCase().includes(normalizedSearch));
  }, [messages, normalizedSearch]);

  const rows = useMemo(() => buildRows(matchingMessages, dayLabels), [dayLabels, matchingMessages]);
  const lastMessage = messages.at(-1);
  const lastSenderId = lastMessage?.senderId;
  // Prepending history keeps the same key; new messages change it.
  const scrollKey = lastMessage?.id ?? lastMessage?.clientMessageId ?? "";
  useEffect(() => {
    if (!initialDraft) return;
    setDraft((current) => current || initialDraft);
  }, [initialDraft]);

  useEffect(() => {
    const stream = streamRef.current;
    if (!stream) return undefined;
    if (loading) {
      initialScrollRef.current = false;
      return undefined;
    }

    const hasNewMessage = previousScrollKeyRef.current !== scrollKey;
    const latestIsOwn = isSameId(lastSenderId, currentUserId);
    const shouldScroll =
      !initialScrollRef.current ||
      (hasNewMessage && (stickToBottomRef.current || latestIsOwn)) ||
      (!hasNewMessage && stickToBottomRef.current);

    previousScrollKeyRef.current = scrollKey;
    initialScrollRef.current = true;
    if (!shouldScroll) return undefined;

    const frame = requestAnimationFrame(() => {
      stream.scrollTop = stream.scrollHeight;
      stickToBottomRef.current = true;
    });
    return () => cancelAnimationFrame(frame);
  }, [currentUserId, lastSenderId, loading, scrollKey, typingUsers.length]);

  function handleStreamScroll(event) {
    const stream = event.currentTarget;
    const distanceFromBottom = stream.scrollHeight - stream.scrollTop - stream.clientHeight;
    stickToBottomRef.current = distanceFromBottom < 96;
  }

  async function handleLoadOlder() {
    const stream = streamRef.current;
    if (!stream || !onLoadOlder) return;

    const previousHeight = stream.scrollHeight;
    const previousTop = stream.scrollTop;
    await onLoadOlder();
    requestAnimationFrame(() => {
      const nextStream = streamRef.current;
      if (!nextStream) return;
      nextStream.scrollTop = previousTop + (nextStream.scrollHeight - previousHeight);
    });
  }

  const stopTypingSignal = useCallback(() => {
    const state = typingStateRef.current;
    clearTimeout(state.timer);
    state.timer = null;

    if (state.active) {
      state.active = false;
      onTypingRef.current?.(false);
    }
  }, [onTypingRef]);

  useEffect(() => stopTypingSignal, [stopTypingSignal]);

  function handleDraftChange(event) {
    const nextDraft = event.target.value;
    const cursor = event.target.selectionStart;
    setDraft(nextDraft);

    const textBeforeCursor = nextDraft.slice(0, cursor);
    const match = textBeforeCursor.match(/(?:^|\s)@([a-zA-Z0-9._-]*)$/);
    if (match) {
      const matchIndex = textBeforeCursor.lastIndexOf("@");
      setMentionState({
        active: true,
        query: match[1] || "",
        index: 0,
        startPos: matchIndex,
      });
    } else {
      setMentionState({ active: false, query: "", index: 0, startPos: 0 });
    }

    if (!onTyping) return;

    const state = typingStateRef.current;
    if (!state.active) {
      state.active = true;
      onTyping(true);
    }

    clearTimeout(state.timer);
    state.timer = setTimeout(() => {
      state.active = false;
      state.timer = null;
      onTyping(false);
    }, TYPING_IDLE_DELAY);
  }

  function insertMention(user) {
    if (!user?.username) return;
    const before = draft.slice(0, mentionState.startPos);
    const after = draft.slice(mentionState.startPos + 1 + mentionState.query.length);
    const nextText = `${before}@${user.username} ${after}`;
    setDraft(nextText);
    setMentionState({ active: false, query: "", index: 0, startPos: 0 });
    requestAnimationFrame(() => {
      if (inputRef.current) {
        inputRef.current.focus();
        const nextPos = before.length + user.username.length + 2;
        inputRef.current.setSelectionRange(nextPos, nextPos);
      }
    });
  }

  function handleComposerKeyDown(event) {
    if (mentionState.active && filteredMentions.length > 0) {
      if (event.key === "ArrowDown") {
        event.preventDefault();
        setMentionState((curr) => ({
          ...curr,
          index: (curr.index + 1) % filteredMentions.length,
        }));
        return;
      }
      if (event.key === "ArrowUp") {
        event.preventDefault();
        setMentionState((curr) => ({
          ...curr,
          index: (curr.index - 1 + filteredMentions.length) % filteredMentions.length,
        }));
        return;
      }
      if (event.key === "Enter" || event.key === "Tab") {
        event.preventDefault();
        insertMention(filteredMentions[mentionState.index]);
        return;
      }
      if (event.key === "Escape") {
        event.preventDefault();
        setMentionState((curr) => ({ ...curr, active: false }));
        return;
      }
    }
  }

  function handleFileSelection(event) {
    const incomingFiles = Array.from(event.target.files ?? []);
    const merged = mergeAttachmentFiles(selectedFiles, incomingFiles);
    setFileError(merged.error ? t(merged.error.key, merged.error.params) : "");
    setSelectedFiles(merged.files);
    event.target.value = "";
  }

  function removeSelectedFile(fileToRemove) {
    setFileError("");
    setSelectedFiles((current) => current.filter((file) => file !== fileToRemove));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const content = draft.trim();
    const files = selectedFiles;
    if ((!content && files.length === 0) || sending || !onSend) return;

    stopTypingSignal();
    const sent = await onSend(content, files);
    if (sent !== false) {
      setDraft("");
      setSelectedFiles([]);
      setFileError("");
    }
  }

  async function handleComposerAi(mode, event) {
    event.currentTarget.closest("details")?.removeAttribute("open");
    const content = draft.trim();
    if (mode !== "reply" && !content) {
      setComposerAi({ answer: "", error: t("chat.ai.draftRequired"), loading: false });
      return;
    }

    const replyLanguage = locale === "vi" ? "Vietnamese" : "English";
    const targetLanguage = locale === "vi" ? "English" : "Vietnamese";
    const prompts = {
      reply: `Suggest one concise, natural reply based on the recent conversation. Write it in ${replyLanguage} and return only the message to send.`,
      rephrase: `Rewrite this message to be clear, concise, and professional. Return only the rewritten message:\n\n${content}`,
      translate: `Translate this message into ${targetLanguage}. Return only the translation:\n\n${content}`,
    };

    setComposerAi({ answer: "", error: "", loading: true });
    try {
      if (mode === "moderation") {
        const response = await moderateCommunityContent({ content, communitySlug });
        const decision = t("ai.moderation.decision." + (response?.decision ?? "REVIEW"));
        const tags = response?.suggestedTags?.length
          ? t("chat.ai.moderationTags", { tags: response.suggestedTags.map((tag) => "#" + tag).join(", ") })
          : "";
        setComposerAi({
          answer: t("chat.ai.moderationResult", {
            decision,
            reason: response?.reason ?? "",
            tags,
          }),
          error: "",
          loading: false,
        });
      } else {
        const response = await askSmartAssistant({
          question: prompts[mode],
          conversationId: Number(conversationId),
        });
        setComposerAi({ answer: response?.answer ?? "", error: "", loading: false });
      }
    } catch (requestError) {
      setComposerAi({
        answer: "",
        error: getApiErrorMessage(requestError, t, "errors.ai"),
        loading: false,
      });
    }
  }

  const composerDisabled = disabled || !onSend;
  const canAttach = !composerDisabled && !sending && selectedFiles.length < MAX_ATTACHMENTS_PER_MESSAGE;
  const canSend = !composerDisabled && !sending && Boolean(draft.trim() || selectedFiles.length);

  async function handleEditSubmit(event, message) {
    event.preventDefault();
    const content = editDraft.trim();
    if (!content || !onEdit) return;
    await onEdit(message.id, content);
    setEditingMessageId(null);
    setEditDraft("");
  }

  return (
    <>
      <div
        className={classNames(
          "message-stream",
          (loading || messages.length === 0) && "message-stream--empty",
        )}
        ref={streamRef}
        onScroll={handleStreamScroll}
      >
        <button
          type="button"
          className={classNames("message-stream__search-toggle", searchOpen && "is-active")}
          aria-label={t("chat.search.toggle")}
          title={t("chat.search.toggle")}
          onClick={() => setSearchOpen((prev) => !prev)}
        >
          <Search size={15} />
        </button>
        {searchOpen ? (
          <div className="message-stream__search-bar">
            <Search size={14} />
            <input
              value={searchQuery}
              placeholder={t("chat.search.placeholder")}
              onChange={(e) => setSearchQuery(e.target.value)}
              autoFocus
            />
            {normalizedSearch ? (
              <small>{t("chat.search.matchCount", { count: matchingMessages.length })}</small>
            ) : null}
            <button
              type="button"
              aria-label={t("chat.search.clear")}
              onClick={() => {
                setSearchQuery("");
                setSearchOpen(false);
              }}
            >
              <X size={14} />
            </button>
          </div>
        ) : null}
        {!loading && hasOlderMessages && onLoadOlder ? (
          <button
            className="message-history-button"
            type="button"
            disabled={loadingOlder}
            onClick={handleLoadOlder}
          >
            {loadingOlder ? t("common.loading") : t("chat.loadEarlier")}
          </button>
        ) : null}
        {loading ? <p className="workspace-empty">{t("common.loading")}</p> : null}
        {!loading && messages.length === 0 ? (
          <p className="workspace-empty">{emptyLabel ?? t("chat.empty")}</p>
        ) : null}
        {!loading && messages.length > 0 && matchingMessages.length === 0 ? (
          <p className="workspace-empty">{t("chat.search.noMatches")}</p>
        ) : null}
        {!loading
          ? rows.map((row) => {
              if (row.type === "separator") {
                return (
                  <div className="message-day" key={row.key}>
                    <span>{row.label}</span>
                  </div>
                );
              }

              const { message } = row;
              const senderName =
                message.sender?.displayName ?? message.sender?.username ?? "";
              const isOwn = isSameId(message.senderId, currentUserId);
              const side = isOwn ? "right" : "left";
              const hasContent = Boolean(message.content);
              const hasAttachments = !message.deletedAt && message.attachments?.length > 0;

              return (
                <article
                  key={row.key}
                  className={classNames(
                    "message",
                    `message--${side}`,
                    message.pending && "message--pending",
                    message.failed && "message--failed",
                  )}
                >
                  {side === "left" ? (
                    <Avatar name={senderName} seed={message.senderId ?? senderName} />
                  ) : null}
                  <div className="message__bubble">
                    {side === "left" ? <strong>{senderName}</strong> : null}
                    {editingMessageId === message.id ? (
                      <form className="message-edit-form" onSubmit={(event) => handleEditSubmit(event, message)}>
                        <input
                          value={editDraft}
                          onChange={(event) => setEditDraft(event.target.value)}
                          aria-label={t("chat.editMessage")}
                          autoFocus
                        />
                        <div className="message-edit-form__actions">
                          <button type="button" onClick={() => setEditingMessageId(null)}>
                            <X size={14} />
                          </button>
                          <button type="submit" disabled={!editDraft.trim()}>
                            <Check size={14} />
                          </button>
                        </div>
                      </form>
                    ) : (
                      <>
                        {message.deletedAt || hasContent ? (
                          <p>{message.deletedAt ? t("chat.messageDeleted") : renderHighlightedText(message.content, normalizedSearch)}</p>
                        ) : null}
                        {hasAttachments ? <MessageAttachments attachments={message.attachments} t={t} /> : null}
                      </>
                    )}
                    {message.reactions?.length && onReact ? (
                      <div className="message-reactions">
                        {message.reactions.map((reaction) => (
                          <button
                            key={reaction.emoji}
                            type="button"
                            className={classNames("message-reaction", reaction.reactedByMe && "is-active")}
                            title={t("chat.toggleReaction", { emoji: REACTION_SYMBOLS[reaction.emoji] ?? reaction.emoji })}
                            onClick={() => onReact?.(message.id, reaction.emoji)}
                          >
                            <span>{REACTION_SYMBOLS[reaction.emoji] ?? reaction.emoji}</span>
                            <small>{reaction.count}</small>
                          </button>
                        ))}
                      </div>
                    ) : null}
                    <div className="message__meta">
                      <time>
                        {formatChatTime(message.createdAt)}
                        {message.editedAt ? <span>{t("chat.edited")}</span> : null}
                        {isOwn ? <MessageStatus status={message.status} t={t} /> : null}
                      </time>
                      {isOwn && message.readReceipts?.length ? (
                        <span className="message__reads" title={t("chat.readBy", { count: message.readReceipts.length })}>
                          <Users size={12} /> {message.readReceipts.length}
                        </span>
                      ) : null}
                    </div>
                    {message.id != null && !readOnly ? (
                      <div className="message__actions">
                        {onPin ? (
                          <button
                            type="button"
                            className={classNames(message.pinned && "is-active")}
                            aria-label={message.pinned ? t("chat.unpinMessage") : t("chat.pinMessage")}
                            title={message.pinned ? t("chat.unpinMessage") : t("chat.pinMessage")}
                            onClick={() => (message.pinned ? onUnpin?.(message.id) : onPin(message.id))}
                          >
                            <Pin size={14} />
                          </button>
                        ) : null}
                        <details className="message-actions-menu">
                          <summary aria-label={t("chat.messageActions")} title={t("chat.messageActions")}>
                            <MoreHorizontal size={15} />
                          </summary>
                          <div className="message-actions-menu__content">
                            <span>{t("chat.reactions")}</span>
                            <div className="message-reaction-picker">
                              {REACTION_TYPES.map((emoji) => (
                                <button
                                  key={emoji}
                                  type="button"
                                  aria-label={REACTION_SYMBOLS[emoji]}
                                  onClick={() => onReact?.(message.id, emoji)}
                                >
                                  <Smile size={13} /> {REACTION_SYMBOLS[emoji]}
                                </button>
                              ))}
                            </div>
                            {isOwn && !message.deletedAt && onEdit ? (
                              <button
                                type="button"
                                onClick={() => {
                                  setEditingMessageId(message.id);
                                  setEditDraft(message.content ?? "");
                                }}
                              >
                                <Pencil size={13} /> {t("chat.editMessage")}
                              </button>
                            ) : null}
                            {isOwn && !message.deletedAt && onDelete ? (
                              <button
                                type="button"
                                onClick={() => {
                                  if (window.confirm(t("chat.confirmDelete"))) onDelete(message.id);
                                }}
                              >
                                <Trash2 size={13} /> {t("chat.deleteMessage")}
                              </button>
                            ) : null}
                            {isOwn && onReadReceipts ? (
                              <button type="button" onClick={() => onReadReceipts(message.id)}>
                                <Users size={13} /> {t("chat.readReceipts")}
                              </button>
                            ) : null}
                          </div>
                        </details>
                      </div>
                    ) : null}
                  </div>
                </article>
              );
            })
          : null}
      </div>

      {!readOnly ? <TypingIndicator users={typingUsers} /> : null}

      {readOnly ? (
        <button className="message-composer message-composer--guest" type="button" onClick={onGuestAction}>
          <LockKeyhole size={19} />
          <span><strong>{t("guest.chat.composerTitle")}</strong><small>{t("guest.chat.composerBody")}</small></span>
          <span className="button button--primary">{t("guest.chat.joinAction")}</span>
        </button>
      ) : (
      <form className={classNames("message-composer", conversationId != null && "message-composer--ai")} onSubmit={handleSubmit}>
        {composerAi.loading ? (
          <div className="message-composer__ai-preview" role="status">
            <Sparkles size={15} /> {t("chat.ai.thinking")}
          </div>
        ) : null}
        {composerAi.error ? (
          <div className="message-composer__ai-preview message-composer__ai-preview--error" role="alert">
            <AlertCircle size={15} />
            <span>{composerAi.error}</span>
            <button
              type="button"
              aria-label={t("common.dismiss")}
              onClick={() => setComposerAi({ answer: "", error: "", loading: false })}
            >
              <X size={14} />
            </button>
          </div>
        ) : null}
        {composerAi.answer ? (
          <div className="message-composer__ai-preview">
            <Sparkles size={15} />
            <span>{composerAi.answer}</span>
            <button
              type="button"
              onClick={() => {
                setDraft(composerAi.answer);
                setComposerAi({ answer: "", error: "", loading: false });
              }}
            >
              {t("chat.ai.use")}
            </button>
            <button
              type="button"
              aria-label={t("common.dismiss")}
              onClick={() => setComposerAi({ answer: "", error: "", loading: false })}
            >
              <X size={14} />
            </button>
          </div>
        ) : null}
        {selectedFiles.length ? (
          <div className="message-composer__attachments">
            {selectedFiles.map((file) => (
              <SelectedAttachmentPreview
                key={`${file.name}:${file.size}:${file.lastModified}`}
                disabled={sending}
                file={file}
                fileSize={formatFileSize(file.size)}
                onRemove={removeSelectedFile}
                t={t}
              />
            ))}
          </div>
        ) : null}
        {fileError ? (
          <p className="message-composer__error" role="alert">
            {fileError}
          </p>
        ) : null}
        {sending && uploadProgress != null ? (
          <div className="message-composer__upload" role="status" aria-live="polite">
            <progress max="100" value={uploadProgress} />
            <span>{t("chat.uploadProgress", { percent: uploadProgress })}</span>
            <button type="button" onClick={onCancelUpload}>
              {t("chat.cancelUpload")}
            </button>
          </div>
        ) : null}
        <button
          type="button"
          aria-label={t("chat.attachments")}
          disabled={!canAttach}
          title={t("chat.attachments")}
          onClick={() => fileInputRef.current?.click()}
        >
          <Paperclip size={19} />
        </button>
        <input
          ref={fileInputRef}
          className="sr-only"
          type="file"
          accept={ATTACHMENT_ACCEPT}
          aria-invalid={Boolean(fileError)}
          multiple
          disabled={!canAttach}
          onChange={handleFileSelection}
          aria-label={t("chat.attachments")}
        />
        {conversationId != null ? (
          <details className="message-composer__ai-menu">
            <summary aria-label={t("chat.ai.actions")} title={t("chat.ai.actions")}>
              <Sparkles size={18} />
            </summary>
            <div>
              <button
                type="button"
                disabled={composerAi.loading}
                onClick={(event) => handleComposerAi("reply", event)}
              >
                <MessageSquareReply size={15} /> {t("chat.ai.smartReply")}
              </button>
              <button
                type="button"
                disabled={composerAi.loading || !draft.trim()}
                onClick={(event) => handleComposerAi("rephrase", event)}
              >
                <WandSparkles size={15} /> {t("chat.ai.rephrase")}
              </button>
              <button
                type="button"
                disabled={composerAi.loading || !draft.trim()}
                onClick={(event) => handleComposerAi("translate", event)}
              >
                <Languages size={15} /> {t("chat.ai.translate")}
              </button>
              {communitySlug ? (
                <button
                  type="button"
                  disabled={composerAi.loading || !draft.trim()}
                  onClick={(event) => handleComposerAi("moderation", event)}
                >
                  <ShieldCheck size={15} /> {t("chat.ai.moderate")}
                </button>
              ) : null}
            </div>
          </details>
        ) : null}
        {mentionState.active && filteredMentions.length > 0 ? (
          <div className="message-composer__mentions" role="listbox">
            <span className="message-composer__mention-header">{t("chat.mention.title")}</span>
            {filteredMentions.map((user, idx) => (
              <button
                key={user.id}
                type="button"
                className={classNames(
                  "message-composer__mention-item",
                  idx === mentionState.index && "is-selected",
                )}
                role="option"
                aria-selected={idx === mentionState.index}
                onClick={() => insertMention(user)}
              >
                <Avatar
                  name={user.displayName || user.username}
                  seed={user.username || user.id}
                  src={user.avatarUrl}
                  size="sm"
                />
                <strong>{user.displayName || user.username}</strong>
                <small>@{user.username}</small>
              </button>
            ))}
          </div>
        ) : null}
        <input
          ref={inputRef}
          placeholder={placeholder ?? t("chat.composerPlaceholder")}
          value={draft}
          onChange={handleDraftChange}
          onKeyDown={handleComposerKeyDown}
          onBlur={stopTypingSignal}
          disabled={composerDisabled || sending}
          aria-label={t("chat.composerAria")}
        />
        <button
          className="send-circle"
          type="submit"
          aria-label={sending ? t("chat.uploadingAttachments") : t("chat.send")}
          disabled={!canSend}
        >
          <Send size={18} />
        </button>
      </form>
      )}
    </>
  );
}
