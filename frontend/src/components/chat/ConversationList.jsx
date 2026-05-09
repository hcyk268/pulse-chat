import {
  BellOff,
  Menu,
  MessageCircle,
  Pin,
  Plus,
  Search,
} from "lucide-react";
import { Link } from "react-router-dom";
import { clampPreview, formatShortTime } from "../../utils/formatters";
import Avatar from "../ui/Avatar";

export default function ConversationList({
  conversations,
  currentUser,
  query,
  onOpenPeople,
  onQueryChange,
  selectedId,
  stats,
}) {
  return (
    <section className="flex h-full min-h-0 w-full flex-col border-r border-black/30 bg-[#17212b] md:max-w-[370px]">
      <div className="border-b border-black/25 px-3 py-3">
        <div className="flex items-center gap-3">
          <button
            type="button"
            className="flex h-10 w-10 items-center justify-center rounded-full text-slate-300 transition hover:bg-white/10 hover:text-white"
            title="Menu"
          >
            <Menu size={21} />
          </button>
          <Link to="/profile" className="shrink-0" title="Profile">
            <Avatar user={currentUser} size="sm" />
          </Link>
          <div className="min-w-0 flex-1">
            <h1 className="truncate text-lg font-semibold tracking-tight text-white">Pulse</h1>
            <p className="truncate text-xs text-slate-400">
              {stats.onlineCount} online - {stats.unreadTotal} unread
            </p>
          </div>
          <button
            type="button"
            onClick={onOpenPeople}
            className="flex h-10 w-10 items-center justify-center rounded-full bg-[#2aabee] text-white shadow-[0_8px_22px_rgba(42,171,238,0.24)] transition hover:bg-[#37b7f4]"
            title="New chat"
          >
            <Plus size={20} />
          </button>
        </div>

        <div className="mt-3 flex items-center gap-2 rounded-full bg-[#242f3d] px-3 py-2 transition focus-within:bg-[#2b3948]">
          <Search size={18} className="text-slate-400" />
          <input
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
            placeholder="Search"
            className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-slate-400"
          />
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto">
        {conversations.length > 0 ? (
          <div>
            {conversations.map((conversation) => {
              const participant = conversation.otherParticipant;
              const isActive = String(selectedId) === String(conversation.id);
              const preview = conversation.lastMessage
                ? clampPreview(conversation.lastMessage.contentPreview)
                : "No messages yet";

              return (
                <Link
                  key={conversation.id}
                  to={`/chat/${conversation.id}`}
                  className={[
                    "group flex gap-3 px-3 py-3 transition duration-150",
                    isActive
                      ? "bg-[#2b5278]"
                      : "hover:bg-[#202b36]",
                  ].join(" ")}
                >
                  <Avatar user={participant} size="md" showStatus />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <p className="truncate text-sm font-semibold text-white">
                            {participant?.displayName}
                          </p>
                          {conversation.pinned ? <Pin size={13} className="text-[#2aabee]" /> : null}
                          {conversation.muted ? (
                            <BellOff size={13} className="text-slate-400" />
                          ) : null}
                        </div>
                      </div>
                      <span className="shrink-0 text-xs text-slate-400">
                        {formatShortTime(conversation.lastMessageAt || conversation.updatedAt)}
                      </span>
                    </div>

                    <div className="mt-2 flex items-center justify-between gap-3">
                      <p
                        className={[
                          "truncate text-sm",
                          conversation.unreadCount > 0 ? "font-medium text-slate-100" : "text-slate-400",
                        ].join(" ")}
                      >
                        {conversation.lastMessage?.senderId === 1 ? "You: " : ""}
                        {preview}
                      </p>
                      {conversation.unreadCount > 0 ? (
                        <span className="min-w-5 rounded-full bg-[#2aabee] px-1.5 py-0.5 text-center text-[11px] font-bold text-white">
                          {conversation.unreadCount}
                        </span>
                      ) : null}
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        ) : (
          <div className="flex h-full flex-col items-center justify-center px-8 text-center">
            <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-[#242f3d] text-slate-400">
              <MessageCircle size={24} />
            </div>
            <p className="font-medium text-white">No matching chats</p>
            <p className="mt-1 text-sm text-slate-500">Try a different name or username.</p>
          </div>
        )}
      </div>
    </section>
  );
}
