import { File, Image as ImageIcon, Paperclip, Pencil, Reply, SendHorizontal, Smile, X } from "lucide-react";
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { useAppSettings } from "../../hooks/useAppSettings";
import { validateUploadFile } from "../../services/uploadApi";
import { useToast } from "../../hooks/useToast";
import { formatFileSize } from "../../utils/formatters";

const EMOJI_OPTIONS = ["\u{1F600}", "\u{1F602}", "\u{1F60D}", "\u{1F60E}", "\u{1F44D}", "\u{1F64F}", "\u{1F525}", "\u{1F389}", "\u2764\uFE0F", "\u2728"];

function getPreviewText(message) {
  if (!message) return "";
  if (message.deletedAt) return "Message deleted";

  return message.content ?? message.attachments?.[0]?.fileName ?? "Attachment";
}

export default function Composer({
  disabled = false,
  disabledReason = null,
  editingMessage = null,
  onCancelEdit,
  onCancelReply,
  onSend,
  onTypingChange,
  replyToMessage = null,
  uploadProgress = null,
}) {
  const { settings } = useAppSettings();
  const toast = useToast();
  const [value, setValue] = useState("");
  const [files, setFiles] = useState([]);
  const [fileError, setFileError] = useState("");
  const [isEmojiOpen, setIsEmojiOpen] = useState(false);
  const fileInputRef = useRef(null);
  const onTypingChangeRef = useRef(onTypingChange);
  const stopTypingTimerRef = useRef(null);
  const typingRef = useRef(false);
  const textareaRef = useRef(null);

  useEffect(() => {
    onTypingChangeRef.current = onTypingChange;
  }, [onTypingChange]);

  useEffect(() => {
    if (editingMessage) {
      setValue(editingMessage.content ?? "");
      setFiles([]);
      setFileError("");
      textareaRef.current?.focus();
    }
  }, [editingMessage]);

  useLayoutEffect(() => {
    const node = textareaRef.current;
    if (!node) return;

    node.style.height = "auto";
    const next = Math.min(node.scrollHeight, 160);
    node.style.height = `${next}px`;
  }, [value]);

  function clearStopTypingTimer() {
    if (!stopTypingTimerRef.current) return;

    window.clearTimeout(stopTypingTimerRef.current);
    stopTypingTimerRef.current = null;
  }

  function emitTyping(nextTyping) {
    if (typingRef.current === nextTyping) return;

    typingRef.current = nextTyping;
    onTypingChangeRef.current?.(nextTyping);
  }

  function scheduleTypingStop() {
    clearStopTypingTimer();
    stopTypingTimerRef.current = window.setTimeout(() => {
      emitTyping(false);
      stopTypingTimerRef.current = null;
    }, 2500);
  }

  function stopTyping() {
    clearStopTypingTimer();
    emitTyping(false);
  }

  useEffect(() => {
    if (!disabled) return;

    if (stopTypingTimerRef.current) {
      window.clearTimeout(stopTypingTimerRef.current);
      stopTypingTimerRef.current = null;
    }

    if (typingRef.current) {
      typingRef.current = false;
      onTypingChangeRef.current?.(false);
    }
  }, [disabled]);

  useEffect(() => {
    return () => {
      clearStopTypingTimer();
      if (typingRef.current) {
        onTypingChangeRef.current?.(false);
      }
    };
  }, []);

  async function submit() {
    const text = value.trim();
    if ((!text && files.length === 0) || disabled) return;

    stopTyping();
    const result = await Promise.resolve(onSend(text, { files }));
    if (result) {
      setValue("");
      setFiles([]);
      setFileError("");
      setIsEmojiOpen(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  }

  function handleChange(event) {
    const nextValue = event.target.value;
    setValue(nextValue);

    if (disabled) return;

    if (nextValue.trim()) {
      emitTyping(true);
      scheduleTypingStop();
    } else {
      stopTyping();
    }
  }

  function handleKeyDown(event) {
    if (event.key !== "Enter" || event.shiftKey) return;

    const shouldSend = settings.enterToSend || event.ctrlKey || event.metaKey;
    if (!shouldSend) return;

    event.preventDefault();
    submit();
  }

  function addEmoji(emoji) {
    setValue((previous) => `${previous}${emoji}`);
    textareaRef.current?.focus();
  }

  function addFiles(nextFiles) {
    const incoming = Array.from(nextFiles ?? []);
    const invalid = incoming.map((file) => ({ file, error: validateUploadFile(file) })).find((item) => item.error);
    if (invalid) {
      const message = `${invalid.file.name}: ${invalid.error}`;
      setFileError(message);
      toast.warning(message);
      return;
    }

    const merged = [...files, ...incoming];
    const byKey = new Map(merged.map((file) => [`${file.name}-${file.size}-${file.lastModified}`, file]));
    const uniqueFiles = Array.from(byKey.values());
    if (uniqueFiles.length > 6) {
      const message = "You can attach up to 6 files per message.";
      setFileError(message);
      toast.warning(message);
      return;
    }
    setFileError("");
    setFiles(uniqueFiles);
  }

  const canSend = !disabled && (value.trim().length > 0 || files.length > 0);
  const contextMessage = editingMessage ?? replyToMessage;
  const contextIcon = editingMessage ? Pencil : Reply;
  const ContextIcon = contextIcon;
  const contextTitle = editingMessage ? "Edit message" : "Reply";
  const contextText = getPreviewText(contextMessage);
  const isUploading = typeof uploadProgress === "number";

  return (
    <div className="chat-composer relative border-t border-white/[0.04] bg-[#111827]/95 px-3 py-3 backdrop-blur-xl">
      <div className="pointer-events-none absolute inset-x-0 -top-px h-px bg-gradient-to-r from-transparent via-indigo-500/20 to-transparent" />

      <div className="mx-auto max-w-4xl">
        {contextMessage ? (
          <div className="mb-2 flex items-center gap-3 rounded-xl border border-indigo-400/15 bg-indigo-500/5 px-3 py-2 shadow-panel-soft">
            <ContextIcon size={16} className="shrink-0 text-indigo-400" />
            <div className="min-w-0 flex-1">
              <p className="text-xs font-semibold text-indigo-400">{contextTitle}</p>
              <p className="truncate text-sm text-slate-300">{contextText}</p>
            </div>
            <button
              type="button"
              onClick={() => {
                if (editingMessage) {
                  setValue("");
                  onCancelEdit?.();
                } else {
                  onCancelReply?.();
                }
              }}
              className="press flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-slate-400 hover:bg-white/5 hover:text-white"
              title="Cancel"
            >
              <X size={16} />
            </button>
          </div>
        ) : null}

        {files.length > 0 ? (
          <div className="mb-2 flex flex-wrap gap-2">
            {files.map((file) => {
              const isImage = file.type.startsWith("image/");
              const FileIcon = isImage ? ImageIcon : File;

              return (
                <span key={`${file.name}-${file.size}-${file.lastModified}`} className="inline-flex max-w-full items-center gap-2 rounded-xl border border-white/5 bg-[#1e293b] px-3 py-1.5 text-xs text-slate-300">
                  <FileIcon size={14} className="shrink-0 text-indigo-300" />
                  <span className="max-w-48 truncate">{file.name}</span>
                  <span className="shrink-0 text-slate-500">{formatFileSize(file.size)}</span>
                  <button
                    type="button"
                    onClick={() => setFiles((previous) => previous.filter((item) => item !== file))}
                    className="press -mr-1 flex h-5 w-5 items-center justify-center rounded-md text-slate-500 hover:bg-white/5 hover:text-white"
                    title="Remove file"
                  >
                    <X size={13} />
                  </button>
                </span>
              );
            })}
          </div>
        ) : null}

        {fileError ? (
          <p role="alert" className="mb-2 rounded-xl border border-rose-400/15 bg-rose-400/10 px-3 py-2 text-xs text-rose-200">
            {fileError}
          </p>
        ) : null}

        {isUploading ? (
          <div className="mb-2 h-1.5 overflow-hidden rounded-full bg-[#1e293b]">
            <div className="h-full rounded-full bg-indigo-400 transition-all duration-200" style={{ width: `${uploadProgress}%` }} />
          </div>
        ) : null}

        <div className="flex items-end gap-2">
          <div className="field-shell flex min-h-12 flex-1 items-end rounded-2xl border border-white/5 bg-[#1e293b] px-3 py-1.5">
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept="image/jpeg,image/png,image/webp,image/gif,video/mp4,video/webm,audio/mpeg,audio/ogg,application/pdf,text/plain,application/zip"
              className="hidden"
              onChange={(event) => addFiles(event.target.files)}
            />
            <button
              type="button"
              disabled={disabled || Boolean(editingMessage)}
              onClick={() => fileInputRef.current?.click()}
              className="press mb-2 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-slate-500 hover:bg-white/5 hover:text-slate-300 disabled:cursor-not-allowed disabled:text-slate-700"
              title="Attach files"
            >
              <Paperclip size={18} />
            </button>
            <textarea
              aria-label="Message"
              ref={textareaRef}
              value={value}
              onChange={handleChange}
              onKeyDown={handleKeyDown}
              disabled={disabled}
              rows={1}
              maxLength={4000}
              placeholder={disabledReason || (disabled ? "Sending..." : editingMessage ? "Edit message" : "Type a message...")}
              className="max-h-40 min-h-8 flex-1 resize-none bg-transparent px-2 py-2 text-sm leading-5 text-white outline-none placeholder:text-slate-500 disabled:cursor-not-allowed disabled:text-slate-600"
            />
            <div className="relative mb-2">
              <button
                type="button"
                tabIndex={-1}
                onClick={() => setIsEmojiOpen((open) => !open)}
                className="press flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-slate-500 hover:bg-white/5 hover:text-slate-300"
                title="Emoji"
              >
                <Smile size={18} />
              </button>
              {isEmojiOpen ? (
                <div className="absolute bottom-10 right-0 z-30 grid grid-cols-5 gap-1 rounded-2xl border border-white/10 bg-[#1f2937]/98 p-2 shadow-panel backdrop-blur-xl">
                  {EMOJI_OPTIONS.map((emoji) => (
                    <button
                      key={emoji}
                      type="button"
                      onClick={() => addEmoji(emoji)}
                      className="press flex h-8 w-8 items-center justify-center rounded-lg text-lg hover:bg-white/10"
                      title="Insert emoji"
                    >
                      {emoji}
                    </button>
                  ))}
                </div>
              ) : null}
            </div>
          </div>
          <button
            type="button"
            onClick={submit}
            disabled={!canSend}
            aria-label="Send"
            className={[
              "send-button flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl text-white",
              canSend
                ? "bg-gradient-to-br from-indigo-500 to-purple-600 shadow-send hover:shadow-send-hover"
                : "cursor-not-allowed bg-[#1e293b] text-slate-600 shadow-none",
            ].join(" ")}
            title="Send"
          >
            <SendHorizontal
              size={19}
              className={[
                "relative z-[1] transition-transform duration-200 ease-out-soft",
                canSend ? "translate-x-px" : "",
              ].join(" ")}
            />
          </button>
        </div>
      </div>
    </div>
  );
}
