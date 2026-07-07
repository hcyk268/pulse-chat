import { Paperclip, Pencil, Reply, SendHorizontal, Smile, X } from "lucide-react";
import { useEffect, useLayoutEffect, useRef, useState } from "react";

function getPreviewText(message) {
  if (!message) return "";
  if (message.deletedAt) return "Message deleted";

  return message.content ?? "";
}

export default function Composer({
  disabled = false,
  editingMessage = null,
  onCancelEdit,
  onCancelReply,
  onSend,
  onTypingChange,
  replyToMessage = null,
}) {
  const [value, setValue] = useState("");
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
    if (disabled) {
      stopTyping();
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
    if (!text || disabled) return;

    stopTyping();
    const result = await Promise.resolve(onSend(text));
    if (result) {
      setValue("");
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
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      submit();
    }
  }

  const canSend = !disabled && value.trim().length > 0;
  const contextMessage = editingMessage ?? replyToMessage;
  const contextIcon = editingMessage ? Pencil : Reply;
  const ContextIcon = contextIcon;
  const contextTitle = editingMessage ? "Edit message" : "Reply";
  const contextText = getPreviewText(contextMessage);

  return (
    <div className="relative border-t border-white/[0.04] bg-[#111827]/95 px-3 py-3 backdrop-blur-xl">
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

        <div className="flex items-end gap-2">
          <div className="field-shell flex min-h-12 flex-1 items-end rounded-2xl border border-white/5 bg-[#1e293b] px-3 py-1.5">
            <button
              type="button"
              tabIndex={-1}
              className="press mb-2 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-slate-500 hover:bg-white/5 hover:text-slate-300"
              title="Attach"
            >
              <Paperclip size={18} />
            </button>
            <textarea
              ref={textareaRef}
              value={value}
              onChange={handleChange}
              onKeyDown={handleKeyDown}
              disabled={disabled}
              rows={1}
              maxLength={4000}
              placeholder={disabled ? "Sending..." : editingMessage ? "Edit message" : "Type a message..."}
              className="max-h-40 min-h-8 flex-1 resize-none bg-transparent px-2 py-2 text-sm leading-5 text-white outline-none placeholder:text-slate-500 disabled:cursor-not-allowed disabled:text-slate-600"
            />
            <button
              type="button"
              tabIndex={-1}
              className="press mb-2 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-slate-500 hover:bg-white/5 hover:text-slate-300"
              title="Emoji"
            >
              <Smile size={18} />
            </button>
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
