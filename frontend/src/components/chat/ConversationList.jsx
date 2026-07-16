import {
  BellOff,
  LogOut,
  Menu,
  MessageSquarePlus,
  Pin,
  Plus,
  Search,
  Settings,
  UserRound,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Check, MailPlus, X } from "lucide-react";
import { RefreshCw } from "lucide-react";
import { Link } from "react-router-dom";
import { useAppSettings } from "../../hooks/useAppSettings";
import { clampPreview, formatShortTime } from "../../utils/formatters";
import { isSameId } from "../../utils/chat";
import { EmptyChatsAsset } from "../assets/MicroAssets";
import Avatar from "../ui/Avatar";

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
  onAcceptInvitation,
  onRejectInvitation,
  onOpenPeople,
  onOpenProfile,
  onQueryChange,
  onRetry,
  onSignOut,
  realtimeStatus = "idle",
  selectedId,
  stats,
}) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isInvitationsOpen, setIsInvitationsOpen] = useState(false);
  const [pendingActionId, setPendingActionId] = useState(null);
  const { settings } = useAppSettings();
  const menuRef = useRef(null);
  const previousInvitationCountRef = useRef(0);
  const realtime = REALTIME_TONE[realtimeStatus] ?? REALTIME_TONE.idle;
  const invitations = conversations.filter(
    (conversation) => conversation.currentUserStatus === "PENDING" || conversation.isPendingInvitation,
  );
  const activeConversations = conversations.filter(
    (conversation) => conversation.currentUserStatus !== "PENDING" && !conversation.isPendingInvitation,
  );

  useEffect(() => {
    if (invitations.length > previousInvitationCountRef.current) {
      setIsInvitationsOpen(true);
    }
    previousInvitationCountRef.current = invitations.length;
  }, [invitations.length]);

  async function handleInvitationAction(conversationId, action) {
    if (pendingActionId) return;
    setPendingActionId(String(conversationId));
    try {
      await action?.(conversationId);
    } finally {
      setPendingActionId(null);
    }
  }

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
    <section className="conversation-list flex h-full min-h-0 w-full flex-col border-r border-white/[0.05] bg-[#111827]">
      <div className="conversation-list__header relative z-50 overflow-visible border-b border-white/[0.05] px-4 py-4">
        <div className="flex items-center gap-3">
          <div ref={menuRef} className="relative shrink-0">
            <button
              type="button"
              aria-expanded={isMenuOpen}
              onClick={() => setIsMenuOpen((open) => !open)}
              className="press flex h-10 w-10 items-center justify-center rounded-xl text-slate-400 hover:bg-white/5 hover:text-white"
              title="Menu"
            >
              <Menu size={20} />
            </button>

            {isMenuOpen ? (
              <div className="menu-pop absolute left-0 top-12 z-[90] w-60 max-w-[calc(100vw-24px)] overflow-hidden rounded-2xl border border-white/[0.08] bg-[#111a2a] py-2 shadow-panel backdrop-blur-xl">
                <button
                  type="button"
                  onClick={() => {
                    setIsMenuOpen(false);
                    onOpenProfile();
                  }}
                  className="flex w-full items-center gap-3 px-4 py-2.5 text-sm font-medium text-slate-200 transition-colors duration-150 hover:bg-white/5 hover:text-white"
                >
                  <UserRound size={17} />
                  Profile
                </button>
                <button
                  type="button"
                  onClick={handleOpenPeopleFromMenu}
                  className="flex w-full items-center gap-3 px-4 py-2.5 text-left text-sm font-medium text-slate-200 transition-colors duration-150 hover:bg-white/5 hover:text-white"
                >
                  <MessageSquarePlus size={17} />
                  New chat
                </button>
                <Link
                  to="/settings"
                  onClick={() => setIsMenuOpen(false)}
                  className="flex w-full items-center gap-3 px-4 py-2.5 text-sm font-medium text-slate-200 transition-colors duration-150 hover:bg-white/5 hover:text-white"
                >
                  <Settings size={17} />
                  Settings
                </Link>
                <div className="my-2 h-px bg-white/5" />
                <button
                  type="button"
                  onClick={handleSignOut}
                  className="flex w-full items-center gap-3 px-4 py-2.5 text-left text-sm font-medium text-rose-300 transition-colors duration-150 hover:bg-rose-400/10"
                >
                  <LogOut size={17} />
                  Sign out
                </button>
              </div>
            ) : null}
          </div>

          <button
            type="button"
            onClick={onOpenProfile}
            className="shrink-0 transition-transform duration-200 ease-out-soft hover:scale-105"
            title="Profile"
          >
            <Avatar user={currentUser} size="sm" />
          </button>

          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <h1 className="truncate bg-gradient-to-r from-white to-slate-300 bg-clip-text text-lg font-bold tracking-tight text-transparent">
                Pulse
              </h1>
              <span
                className={`h-2 w-2 rounded-full transition-colors duration-300 ${realtime.dot}`}
                title={`Realtime ${realtime.label.toLowerCase()}`}
              />
            </div>
            <p className="truncate text-xs text-slate-500">
              {settings.showOnlineStatus ? `${stats.onlineCount} online · ` : ""}
              {stats.unreadTotal} unread
            </p>
          </div>

          <button
            type="button"
            onClick={onOpenPeople}
            className="send-button flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 text-white shadow-send hover:shadow-send-hover"
            title="New chat"
          >
            <Plus size={19} />
          </button>
        </div>

        <div className="field-shell mt-4 flex items-center gap-2.5 rounded-xl border border-white/5 bg-[#1e293b] px-3.5 py-2.5">
          <Search size={16} className="text-slate-500" />
          <input
            aria-label="Search conversations"
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
            placeholder="Search conversations..."
            className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-slate-500"
          />
        </div>

        <button
          type="button"
          aria-controls="group-invitations-panel"
          aria-expanded={isInvitationsOpen}
          onClick={() => setIsInvitationsOpen((open) => !open)}
          className="press mt-3 flex min-h-11 w-full items-center gap-3 rounded-xl border border-white/[0.06] bg-white/[0.025] px-3 text-left hover:border-amber-300/15 hover:bg-amber-300/[0.04]"
        >
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-amber-300/10 text-amber-200"><MailPlus size={16} /></span>
          <span className="min-w-0 flex-1">
            <span className="block text-xs font-semibold text-slate-200">Group invitations</span>
            <span className="block text-[11px] text-slate-500">Review requests to join a group</span>
          </span>
          <span className={["min-w-6 rounded-full px-2 py-0.5 text-center text-[11px] font-bold", invitations.length ? "bg-amber-300 text-[#382b00]" : "bg-white/[0.055] text-slate-500"].join(" ")}>{invitations.length}</span>
        </button>

        {isInvitationsOpen ? (
          <div id="group-invitations-panel" className="mt-2 max-h-72 space-y-2 overflow-y-auto rounded-2xl border border-amber-300/10 bg-[#0a101b]/75 p-2 shadow-panel-soft">
            {invitations.length ? invitations.map((conversation) => {
              const participant = conversation.otherParticipant;
              const isBusy = pendingActionId === String(conversation.id);
              return (
                <article key={conversation.id} className="rounded-xl border border-white/[0.06] bg-white/[0.025] p-2.5">
                  <Link to={`/chat/${conversation.id}`} className="flex min-w-0 items-center gap-3 rounded-lg text-left">
                    <Avatar user={participant} size="sm" />
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-semibold text-white">{participant?.displayName}</span>
                      <span className="block text-xs text-slate-500">Waiting for your response</span>
                    </span>
                  </Link>
                  <div className="mt-2 grid grid-cols-2 gap-2">
                    <button type="button" disabled={isBusy} onClick={() => handleInvitationAction(conversation.id, onRejectInvitation)} className="press flex items-center justify-center gap-1.5 rounded-lg border border-rose-300/10 bg-rose-400/10 px-2 py-2 text-xs font-semibold text-rose-200 hover:bg-rose-400/15 disabled:opacity-50"><X size={14} />Decline</button>
                    <button type="button" disabled={isBusy} onClick={() => handleInvitationAction(conversation.id, onAcceptInvitation)} className="send-button flex items-center justify-center gap-1.5 rounded-lg px-2 py-2 text-xs font-semibold text-white disabled:opacity-50"><Check size={14} />{isBusy ? "Working..." : "Accept"}</button>
                  </div>
                </article>
              );
            }) : (
              <div className="flex flex-col items-center px-4 py-5 text-center">
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/[0.035] text-slate-500"><MailPlus size={18} /></span>
                <p className="mt-3 text-xs font-semibold text-slate-300">No pending invitations</p>
                <p className="mt-1 text-[11px] leading-5 text-slate-600">New group invitations will appear here automatically.</p>
              </div>
            )}
          </div>
        ) : null}
      </div>

      <div className="relative z-0 min-h-0 flex-1 overflow-y-auto px-2 py-2">
        {isLoading ? (
          <ConversationListSkeleton />
        ) : error ? (
          <div className="flex h-full flex-col items-center justify-center px-8 text-center">
            <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-400/10 text-rose-300"><RefreshCw size={20} /></span>
            <p className="mt-4 text-sm font-semibold text-white">Could not load conversations</p>
            <p className="mt-1 max-w-xs text-xs leading-5 text-slate-500">{error}</p>
            <button type="button" onClick={onRetry} className="press mt-4 rounded-xl border border-white/[0.08] bg-white/[0.035] px-4 py-2 text-xs font-semibold text-slate-200 hover:bg-white/[0.07]">Try again</button>
          </div>
        ) : conversations.length > 0 ? (
          <div className="space-y-0.5">
            {activeConversations.map((conversation, index) => {
              const participant = conversation.otherParticipant;
              const isActive = String(selectedId) === String(conversation.id);
              const preview = settings.showMessagePreviews
                ? conversation.lastMessage
                  ? clampPreview(conversation.lastMessage.contentPreview)
                  : "No messages yet"
                : "Message preview hidden";
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
                    isActive
                      ? "bg-gradient-to-r from-indigo-500/15 to-purple-500/10 shadow-[inset_0_0_0_1px_rgba(99,102,241,0.2)]"
                      : "hover:bg-white/[0.03]",
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
                          {conversation.pinned ? (
                            <Pin size={12} className="text-indigo-400" />
                          ) : null}
                          {conversation.muted ? (
                            <BellOff size={12} className="text-slate-500" />
                          ) : null}
                        </div>
                      </div>
                      <span
                        className={[
                          "shrink-0 text-[11px] font-medium",
                          isActive ? "text-indigo-300" : "text-slate-500",
                        ].join(" ")}
                      >
                        {formatShortTime(conversation.lastMessageAt || conversation.updatedAt)}
                      </span>
                    </div>

                    <div className="mt-1.5 flex items-center justify-between gap-3">
                      <p
                        className={[
                          "truncate text-[13px]",
                          isActive
                            ? "text-slate-200"
                            : conversation.unreadCount > 0
                              ? "font-medium text-slate-200"
                              : "text-slate-500",
                        ].join(" ")}
                      >
                        {settings.showMessagePreviews && isOwnLast ? (
                          <span className="text-slate-500">You: </span>
                        ) : ""}
                        {preview}
                      </p>
                      {conversation.unreadCount > 0 ? (
                        <span className="min-w-5 animate-scale-in rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 px-1.5 py-0.5 text-center text-[11px] font-bold text-white shadow-[0_4px_12px_rgba(99,102,241,0.4)]">
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
                  className="press lift w-full rounded-xl border border-white/5 bg-[#1e293b] px-4 py-2.5 text-sm font-medium text-slate-300 hover:bg-[#334155] disabled:cursor-not-allowed disabled:text-slate-500"
                >
                  {isLoadingMore ? "Loading..." : "Load more"}
                </button>
              </div>
            ) : null}
          </div>
        ) : (
          <div className="conversation-list__empty flex h-full animate-fade-in flex-col items-center justify-center px-8 text-center">
            <span className="conversation-list__empty-artwork mb-4 flex items-center justify-center rounded-[2rem] border border-white/[0.06] bg-white/[0.025]">
              <EmptyChatsAsset className="h-28 w-36 opacity-95" />
            </span>
            <p className="font-semibold text-white">
              {query ? "No matching chats" : "Your inbox is ready"}
            </p>
            <p className="mt-1.5 max-w-[250px] text-sm leading-6 text-slate-500">
              {query
                ? "Try a different name or username."
                : "Find someone and start your first conversation."}
            </p>
            {!query ? (
              <button
                type="button"
                onClick={onOpenPeople}
                className="send-button mt-5 flex min-h-11 items-center justify-center gap-2 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 px-4 text-sm font-semibold text-white shadow-send hover:shadow-send-hover"
              >
                <MessageSquarePlus size={17} />
                Start a chat
              </button>
            ) : null}
          </div>
        )}
      </div>
    </section>
  );
}

function ConversationListSkeleton() {
  return (
    <div className="space-y-1 px-1 py-2">
      {Array.from({ length: 6 }).map((_, index) => (
        <div key={index} className="flex items-center gap-3 rounded-xl px-3 py-3">
          <div className="skeleton h-11 w-11 shrink-0 rounded-full" />
          <div className="flex-1 space-y-2.5">
            <div className="skeleton h-3.5 w-2/5 rounded-md" />
            <div className="skeleton h-3 w-3/4 rounded-md" />
          </div>
        </div>
      ))}
    </div>
  );
}
