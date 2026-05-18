import { Paperclip, SendHorizontal, Smile } from "lucide-react";
import { useEffect, useLayoutEffect, useRef, useState } from "react";

export default function Composer({ disabled = false, onSend, onTypingChange }) {
  const [value, setValue] = useState("");
  const onTypingChangeRef = useRef(onTypingChange);
  const stopTypingTimerRef = useRef(null);
  const typingRef = useRef(false);
  const textareaRef = useRef(null);

  useEffect(() => {
    onTypingChangeRef.current = onTypingChange;
  }, [onTypingChange]);

  // Auto-grow textarea up to max-height for smoother typing experience.
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

  function submit() {
    const text = value.trim();
    if (!text || disabled) return;

    stopTyping();
    onSend(text);
    setValue("");
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

  return (
    <div className="relative border-t border-black/30 bg-[#17212b]/95 px-3 py-3 backdrop-blur">
      <div className="pointer-events-none absolute inset-x-0 -top-px h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />

      <div className="mx-auto flex max-w-4xl items-end gap-2">
        <div className="field-shell flex min-h-11 flex-1 items-end rounded-2xl bg-[#242f3d] px-3 py-1.5">
          <button
            type="button"
            tabIndex={-1}
            className="press mb-1.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-slate-400 hover:bg-white/5 hover:text-slate-200"
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
            maxLength={2000}
            placeholder={disabled ? "Sending..." : "Message"}
            className="max-h-40 min-h-8 flex-1 resize-none bg-transparent px-1 py-2 text-sm leading-5 text-white outline-none placeholder:text-slate-400 disabled:cursor-not-allowed disabled:text-slate-500"
          />
          <button
            type="button"
            tabIndex={-1}
            className="press mb-1.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-slate-400 hover:bg-white/5 hover:text-slate-200"
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
            "send-button flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-white",
            canSend
              ? "bg-gradient-to-br from-[#3cb8f5] to-[#2aabee] shadow-send hover:shadow-send-hover"
              : "cursor-not-allowed bg-[#242f3d] text-slate-500 shadow-none",
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
  );
}
