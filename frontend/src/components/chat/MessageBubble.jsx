import { Check, CheckCheck, Clock3 } from "lucide-react";
import { formatShortTime } from "../../utils/formatters";
import Avatar from "../ui/Avatar";

function StatusIcon({ status }) {
  if (status === "READ") return <CheckCheck size={14} className="text-sky-100" />;
  if (status === "DELIVERED") return <CheckCheck size={14} className="text-sky-100/80" />;
  if (status === "SENT") return <Check size={14} className="text-sky-100/75" />;

  return <Clock3 size={14} className="text-slate-400" />;
}

export default function MessageBubble({ message, sender, isOwn }) {
  return (
    <div className={`flex gap-2 ${isOwn ? "justify-end" : "justify-start"} animate-enter-up`}>
      {!isOwn ? <Avatar user={sender} size="sm" /> : null}
      <div className={`max-w-[86%] sm:max-w-[66%] ${isOwn ? "items-end" : "items-start"} flex flex-col`}>
        <div
          className={[
            "rounded-2xl px-3.5 py-2.5 text-sm leading-6 shadow-[0_8px_20px_rgba(0,0,0,0.18)] transition duration-150",
            isOwn
              ? "rounded-br-md bg-[#2b7bb9] text-white"
              : "rounded-bl-md bg-[#182533] text-slate-100",
          ].join(" ")}
        >
          {message.content}
        </div>
        <div className="mt-1 flex items-center gap-1.5 px-1 text-[11px] text-slate-500">
          <span>{formatShortTime(message.createdAt)}</span>
          {isOwn ? <StatusIcon status={message.status} /> : null}
        </div>
      </div>
    </div>
  );
}
