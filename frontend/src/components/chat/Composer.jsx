import { SendHorizontal } from "lucide-react";
import { useState } from "react";

export default function Composer({ disabled = false, onSend }) {
  const [value, setValue] = useState("");

  function submit() {
    const text = value.trim();
    if (!text || disabled) return;

    onSend(text);
    setValue("");
  }

  function handleKeyDown(event) {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      submit();
    }
  }

  return (
    <div className="border-t border-black/25 bg-[#17212b] px-3 py-3">
      <div className="mx-auto flex max-w-4xl items-end gap-2">
        <div className="flex min-h-11 flex-1 items-end rounded-2xl bg-[#242f3d] px-4 py-1.5 transition focus-within:bg-[#2b3948]">
          <textarea
            value={value}
            onChange={(event) => setValue(event.target.value)}
            onKeyDown={handleKeyDown}
            disabled={disabled}
            rows={1}
            maxLength={2000}
            placeholder={disabled ? "Sending..." : "Message"}
            className="max-h-32 min-h-8 flex-1 resize-none bg-transparent py-2 text-sm leading-5 text-white outline-none placeholder:text-slate-400 disabled:cursor-not-allowed disabled:text-slate-500"
          />
        </div>
        <button
          type="button"
          onClick={submit}
          disabled={disabled || !value.trim()}
          className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[#2aabee] text-white shadow-[0_8px_22px_rgba(42,171,238,0.22)] transition hover:bg-[#37b7f4] disabled:cursor-not-allowed disabled:bg-[#242f3d] disabled:text-slate-500 disabled:shadow-none"
          title="Send"
        >
          <SendHorizontal size={19} />
        </button>
      </div>
    </div>
  );
}
