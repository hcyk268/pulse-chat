import {
  BellOff,
  LogOut,
  Menu,
  MessageCircle,
  MessageSquarePlus,
  Pin,
  Plus,
  Search,
  UserRound,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { clampPreview, formatShortTime } from "../../utils/formatters";
import Avatar from "../ui/Avatar";

function isSameId(left, right) {
  return left != null && right != null && String(left) === String(right);
}

const REALTIME_TONE = {
  connected: {
    dot: "bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.55)]",
    label: "Live",
  },
  connecting: {
    dot: "bg-amber-300 shadow-[0_0_6px_rgba(251,191,36,0.5)] animate-pulse",
    label: "Connecting",
  },
  reconnecting: {
    dot: "bg-amber-300 shadow-[0_0_6px_rgba(251,191,36,0.5)] animate-pulse",
    label: "Reconnecting",
  },
  idle: {
    dot: "bg-slate-500",
    label: "Idle",
  },
};

export default function ConversationList({
  conversations,
  currentUser,
  error = "",
  hasMore = false,
  isLoading = false,
  isLoadingMore = false,
  query,
  onLoadMore,
  onOpenPeople,
  onQueryChange,
  onSignOut,
  realtimeStatus = "idle",
  selectedId,
  stats,
}) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const menuRef = useRef(null);
  const realtime = REALTIME_TONE[realtimeStatus] ?? REALTIME_TONE.idle;

  useEffect(() => {
    if (!isMenuOpen) return undefined;

    function handlePointerDown(event) {
      if (!menuRef.current?.contains(event.target)) {
        setIsMenuOpen(false);
      }
    }

    function handleKeyDown(event) {
      if (event.key === "Escape") {
        setIsMenuOpen(false);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isMenuOpen]);

  function handleOpenPeopleFromMenu() {
    setIsMenuOpen(false);
    onOpenPeople();
  }

  function handleSignOut() {
    setIsMenuOpen(false);
    onSignOut?.();
  }

  return (
    <section className="flex h-full min-h-0 w-full flex-col border-r border-black/30 bg-[#17212b] md:max-w-[370px]">
      <div className="relative border-b border-black/30 px-3 py-3">
        <div className="flex items-center gap-3">
          <div ref={menuRef} className="relative shrink-0">
            <button
              type="button"
              aria-expanded={isMenuOpen}
              onClick={() => setIsMenuOpen((open) => !open)}
              className="press flex h-10 w-10 items-center justify-center rounded-full text-slate-300 hover:bg-white/10 hover:text-white"
              title="Menu"
            >
              <Menu size={21} />
            </button>

            {isMenuOpen ? (
              <div className="menu-pop absolute left-0 top-12 z-30 w-56 overflow-hidden rounded-xl border border-white/5 bg-[#202b36]/98 py-2 shadow-panel backdrop-blur">
                <Link
                  to="/profile"
                  onClick={() => setIsMenuOpen(false)}
                  className="flex w-full items-center gap-3 px-3 py-2.5 text-sm font-medium text-slate-200 transition-colors duration-150 hover:bg-white/10 hover:text-white"
                >
                  <UserRound size={17} />
                  Profile
                </Link>
                <button
                  type="button"
                  onClick={handleOpenPeopleFromMenu}
                  className="flex w-full items-center gap-3 px-3 py-2.5 text-left text-sm font-medium text-slate-200 transition-colors duration-150 hover:bg-white/10 hover:text-white"
                >
                  <MessageSquarePlus size={17} />
                  New chat
                </button>
                <div className="my-2 h-px bg-white/5" />
                <button
                  type="button"
                  onClick={handleSignOut}
                  className="flex w-full items-center gap-3 px-3 py-2.5 text-left text-sm font-medium text-rose-100 transition-colors duration-150 hover:bg-rose-400/10"
                >
                  <LogOut size={17} />
                  Sign out
                </button>
              </div>
            ) : null}
          </div>

          <Link
            to="/profile"
            className="shrink-0 transition-transform duration-200 ease-out-soft hover:scale-105"
            title="Profile"
          >
            <Avatar user={currentUser} size="sm" />
          </Link>

          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <h1 className="truncate bg-gradient-to-r from-white to-slate-300 bg-clip-text text-lg font-semibold tracking-tight text-transparent">
                Pulse
              </h1>
              <span
                className={`h-2 w-2 rounded-full transition-colors duration-300 ${realtime.dot}`}
                title={`Realtime ${realtime.label.toLowerCase()}`}
              />
            </div>
            <p className="truncate text-xs text-slate-400">
              {stats.onlineCount} online · {stats.unreadTotal} unread
            </p>
          </div>

          <button
            type="button"
            onClick={onOpenPeople}
            className="send-button flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-[#3cb8f5] to-[#2aabee] text-white shadow-send hover:shadow-send-hover"
            title="New chat"
          >
            <Plus size={20} />
          </button>
        </div>

        <div className="field-shell mt-3 flex items-center gap-2 rounded-full bg-[#242f3d] px-3 py-2">
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
        {isLoading ? (
          <ConversationListSkeleton />
        ) : error ? (
          <div className="flex h-full items-center justify-center px-8 text-center text-sm leading-6 text-rose-100">
            {error}
          </div>
        ) : conversations.length > 0 ? (
          <div className="py-1">
            {conversations.map((conversation, index) => {
              const participant = conversation.otherParticipant;
              const isActive = String(selectedId) === String(conversation.id);
              const preview = conversation.lastMessage
                ? clampPreview(conversation.lastMessage.contentPreview)
                : "No messages yet";
              const isOwnLast = isSameId(
                conversation.lastMessage?.senderId,
                currentUser.id,
              );

              return (
                <Link
                  key={conversation.id}
                  to={`/chat/${conversation.id}`}
                  data-active={isActive ? "true" : "false"}
                  style={{ animationDelay: `${Math.min(index, 8) * 24}ms` }}
                  className={[
                    "conv-row group flex animate-enter-up gap-3 px-3 py-3",
                    isActive ? "bg-[#2b5278]" : "hover:bg-[#202b36]",
                  ].join(" ")}
                >
                  <Avatar user={participant} size="md" showStatus />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <p
                            className={[
                              "truncate text-sm font-semibold transition-colors duration-200",
                              isActive ? "text-white" : "text-white",
                            ].join(" ")}
                          >
                            {participant?.displayName}
                          </p>
                          {conversation.pinned ? (
                            <Pin size={13} className="text-[#6ab7ee]" />
                          ) : null}
                          {conversation.muted ? (
                            <BellOff size={13} className="text-slate-400" />
                          ) : null}
                        </div>
                      </div>
                      <span
                        className={[
                          "shrink-0 text-xs transition-colors duration-200",
                          isActive ? "text-cyan-100/80" : "text-slate-400",
                        ].join(" ")}
                      >
                        {formatShortTime(conversation.lastMessageAt || conversation.updatedAt)}
                      </span>
                    </div>

                    <div className="mt-1.5 flex items-center justify-between gap-3">
                      <p
                        className={[
                          "truncate text-sm",
                          isActive
                            ? "text-cyan-50/90"
                            : conversation.unreadCount > 0
                              ? "font-medium text-slate-100"
                              : "text-slate-400",
                        ].join(" ")}
                      >
                        {isOwnLast ? <span className="text-slate-400">You: </span> : ""}
                        {preview}
                      </p>
                      {conversation.unreadCount > 0 ? (
                        <span className="min-w-5 animate-scale-in rounded-full bg-gradient-to-br from-[#3cb8f5] to-[#2aabee] px-1.5 py-0.5 text-center text-[11px] font-bold text-white shadow-[0_4px_12px_rgba(42,171,238,0.45)]">
                          {conversation.unreadCount}
                        </span>
                      ) : null}
                    </div>
                  </div>
                </Link>
              );
            })}
            {hasMore ? (
              <div className="p-3">
                <button
                  type="button"
                  onClick={onLoadMore}
                  disabled={isLoadingMore}
                  className="press lift w-full rounded-lg bg-[#242f3d] px-4 py-2.5 text-sm font-medium text-slate-200 hover:bg-[#2b3948] disabled:cursor-not-allowed disabled:text-slate-500"
                >
                  {isLoadingMore ? "Loading..." : "Load more"}
                </button>
              </div>
            ) : null}
          </div>
        ) : (
          <div className="flex h-full animate-fade-in flex-col items-center justify-center px-8 text-center">
            <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-gradient-to-br from-[#242f3d] to-[#1c2733] text-slate-400 shadow-panel-soft">
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

function ConversationListSkeleton() {
  return (
    <div className="space-y-1 px-3 py-2">
      {Array.from({ length: 6 }).map((_, index) => (
        <div key={index} className="flex items-center gap-3 px-1 py-3">
          <div className="skeleton h-11 w-11 shrink-0 rounded-full" />
          <div className="flex-1 space-y-2">
            <div className="skeleton h-3.5 w-2/5 rounded" />
            <div className="skeleton h-3 w-3/4 rounded" />
          </div>
        </div>
      ))}
    </div>
  );
}
