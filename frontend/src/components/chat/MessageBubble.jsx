import { Check, CheckCheck, Clock3 } from "lucide-react";
import { formatShortTime } from "../../utils/formatters";
import Avatar from "../ui/Avatar";

function StatusIcon({ status }) {
  if (status === "READ")
    return <CheckCheck size={14} className="text-cyan-200 transition-colors duration-200" />;
  if (status === "DELIVERED")
    return <CheckCheck size={14} className="text-sky-100/80 transition-colors duration-200" />;
  if (status === "SENT")
    return <Check size={14} className="text-sky-100/75 transition-colors duration-200" />;

  return <Clock3 size={14} className="text-slate-400 transition-colors duration-200" />;
}

export default function MessageBubble({ message, sender, isOwn }) {
  return (
    <div
      className={[
        "group flex gap-2",
        isOwn ? "justify-end animate-enter-bubble-right" : "justify-start animate-enter-bubble-left",
      ].join(" ")}
    >
      {!isOwn ? (
        <div className="self-end transition-transform duration-200 group-hover:-translate-y-0.5">
          <Avatar user={sender} size="sm" />
        </div>
      ) : null}

      <div
        className={[
          "flex max-w-[86%] flex-col sm:max-w-[66%]",
          isOwn ? "items-end" : "items-start",
        ].join(" ")}
      >
        <div
          className={[
            "bubble-shine relative rounded-2xl px-3.5 py-2.5 text-sm leading-6 transition-transform duration-200 ease-out-soft",
            "group-hover:-translate-y-0.5",
            isOwn
              ? "rounded-br-md bg-gradient-to-br from-[#3290d4] to-[#2a6fa6] text-white shadow-bubble-own"
              : "rounded-bl-md bg-[#182533] text-slate-100 shadow-bubble-other",
          ].join(" ")}
        >
          <span className="relative z-[1] whitespace-pre-wrap break-words">{message.content}</span>
        </div>

        <div className="mt-1 flex items-center gap-1.5 px-1 text-[11px] text-slate-500 opacity-80 transition-opacity duration-200 group-hover:opacity-100">
          <span>{formatShortTime(message.createdAt)}</span>
          {isOwn ? <StatusIcon status={message.status} /> : null}
        </div>
      </div>
    </div>
  );
}
