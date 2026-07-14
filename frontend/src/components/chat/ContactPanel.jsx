import { Check, Search, UserPlus, Users, X } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useAppSettings } from "../../hooks/useAppSettings";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { formatPresence } from "../../utils/formatters";
import { PeopleSearchAsset } from "../assets/MicroAssets";
import Avatar from "../ui/Avatar";

export default function ContactPanel({
  contacts,
  isSearching = false,
  isStartingConversation = false,
  onClearSearch,
  onClose,
  onCreateGroup,
  onSearchUsers,
  onStartConversation,
  searchError = "",
  searchResults = [],
}) {
  const { settings } = useAppSettings();
  const [mode, setMode] = useState("direct");
  const [query, setQuery] = useState("");
  const [groupName, setGroupName] = useState("");
  const [groupAvatarUrl, setGroupAvatarUrl] = useState("");
  const [selectedContacts, setSelectedContacts] = useState([]);
  const normalizedQuery = query.trim();
  const debouncedQuery = useDebouncedValue(normalizedQuery, 300);
  const visibleContacts = normalizedQuery ? searchResults : contacts;
  const selectedIds = useMemo(
    () => new Set(selectedContacts.map((contact) => String(contact.backendId ?? contact.id))),
    [selectedContacts],
  );

  useEffect(() => {
    if (!debouncedQuery) {
      onClearSearch?.();
      return undefined;
    }

    onSearchUsers?.(debouncedQuery);
    return undefined;
  }, [debouncedQuery, onClearSearch, onSearchUsers]);

  function toggleContact(contact) {
    const contactId = String(contact.backendId ?? contact.id);
    setSelectedContacts((previous) => {
      if (previous.some((item) => String(item.backendId ?? item.id) === contactId)) {
        return previous.filter((item) => String(item.backendId ?? item.id) !== contactId);
      }

      return [...previous, contact];
    });
  }

  async function handleCreateGroup() {
    const memberIds = selectedContacts.map((contact) => contact.backendId ?? contact.id);
    const nextConversationId = await onCreateGroup?.({
      name: groupName,
      avatarUrl: groupAvatarUrl,
      memberIds,
    });

    if (nextConversationId) {
      setGroupName("");
      setGroupAvatarUrl("");
      setSelectedContacts([]);
    }
  }

  const canCreateGroup = groupName.trim().length > 0 && selectedContacts.length >= 2 && !isStartingConversation;

  return (
    <aside className="flex h-full min-h-0 w-full flex-col bg-[#111827]">
      <div className="border-b border-white/[0.04] px-4 py-4">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="text-xl font-bold text-white">New Message</h2>
            <p className="mt-0.5 text-xs text-slate-500">Search active users</p>
          </div>
          {onClose ? (
            <button
              type="button"
              onClick={onClose}
              className="press flex h-10 w-10 items-center justify-center rounded-xl text-slate-400 hover:bg-white/5 hover:text-white"
              title="Close"
            >
              <X size={18} />
            </button>
          ) : null}
        </div>

        <div className="mt-4 grid grid-cols-2 rounded-xl border border-white/5 bg-[#1e293b] p-1">
          <ModeButton active={mode === "direct"} icon={UserPlus} label="Direct" onClick={() => setMode("direct")} />
          <ModeButton active={mode === "group"} icon={Users} label="Group" onClick={() => setMode("group")} />
        </div>

        {mode === "group" ? (
          <div className="mt-3 space-y-2">
            <input
              value={groupName}
              onChange={(event) => setGroupName(event.target.value)}
              maxLength={100}
              placeholder="Group name"
              className="field-shell w-full rounded-xl border border-white/5 bg-[#1e293b] px-3.5 py-2.5 text-sm text-white outline-none placeholder:text-slate-500"
            />
            <input
              value={groupAvatarUrl}
              onChange={(event) => setGroupAvatarUrl(event.target.value)}
              maxLength={500}
              placeholder="Avatar URL"
              className="field-shell w-full rounded-xl border border-white/5 bg-[#1e293b] px-3.5 py-2.5 text-sm text-white outline-none placeholder:text-slate-500"
            />
          </div>
        ) : null}

        <div className="field-shell mt-4 flex items-center gap-2.5 rounded-xl border border-white/5 bg-[#1e293b] px-3.5 py-2.5">
          <Search size={16} className="text-slate-500" />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search by name or username..."
            autoFocus
            className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-slate-500"
          />
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto px-2 py-2">
        {isSearching ? (
          <ContactSkeleton />
        ) : searchError ? (
          <div className="flex h-full animate-fade-in items-center justify-center px-8 text-center text-sm leading-6 text-rose-200">
            {searchError}
          </div>
        ) : visibleContacts.length > 0 ? (
          <div className="space-y-0.5">
            {visibleContacts.map((contact, index) => {
              const contactId = String(contact.backendId ?? contact.id);
              const selected = selectedIds.has(contactId);

              return (
                <button
                  key={contact.id}
                  type="button"
                  disabled={isStartingConversation}
                  onClick={() => mode === "group" ? toggleContact(contact) : onStartConversation(contact)}
                  style={{ animationDelay: `${Math.min(index, 10) * 28}ms` }}
                  className="conv-row group flex w-full animate-enter-up items-center gap-3 px-3 py-3 text-left hover:bg-white/[0.03] disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <Avatar user={contact} size="md" showStatus />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center justify-between gap-2">
                      <p className="truncate text-sm font-semibold text-white">
                        {contact.displayName}
                      </p>
                      {mode === "group" ? (
                        <span className={["flex h-6 w-6 shrink-0 items-center justify-center rounded-lg border transition-colors", selected ? "border-indigo-400 bg-indigo-500 text-white" : "border-white/10 text-slate-500"].join(" ")}>
                          {selected ? <Check size={14} /> : null}
                        </span>
                      ) : (
                        <UserPlus
                          size={16}
                          className="shrink-0 text-slate-500 transition-all duration-200 group-hover:translate-x-0.5 group-hover:text-indigo-400"
                        />
                      )}
                    </div>
                    <p className="truncate text-xs text-slate-500">
                      {contact.role} - @{contact.username}
                    </p>
                    <p
                      className={[
                        "mt-1 truncate text-xs transition-colors duration-300",
                        contact?.presence?.isOnline ? "text-indigo-400" : "text-slate-600",
                      ].join(" ")}
                    >
                      {settings.showOnlineStatus ? formatPresence(contact.presence) : "Pulse member"}
                    </p>
                  </div>
                </button>
              );
            })}
          </div>
        ) : (
          <div className="flex h-full animate-fade-in flex-col items-center justify-center px-8 text-center">
            <PeopleSearchAsset className="mb-3 h-28 w-36 opacity-95" />
            <p className="font-medium text-white">No users found</p>
            <p className="mt-1 text-sm text-slate-500">
              {normalizedQuery
                ? "Try another name or username."
                : "Search for a user to start chatting."}
            </p>
          </div>
        )}
      </div>

      {mode === "group" ? (
        <div className="border-t border-white/[0.04] p-4">
          <button
            type="button"
            onClick={handleCreateGroup}
            disabled={!canCreateGroup}
            className="send-button flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 px-4 py-3 font-semibold text-white shadow-send hover:shadow-send-hover disabled:cursor-not-allowed disabled:bg-[#1e293b] disabled:bg-none disabled:text-slate-600 disabled:shadow-none"
          >
            <Users size={18} />
            Create group
          </button>
        </div>
      ) : null}
    </aside>
  );
}

function ModeButton({ active, icon: Icon, label, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={[
        "press flex items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-semibold transition-colors",
        active ? "bg-indigo-500 text-white" : "text-slate-400 hover:bg-white/5 hover:text-white",
      ].join(" ")}
    >
      <Icon size={16} />
      {label}
    </button>
  );
}

function ContactSkeleton() {
  return (
    <div className="space-y-1 px-1 py-2">
      {Array.from({ length: 5 }).map((_, index) => (
        <div key={index} className="flex items-center gap-3 rounded-xl px-3 py-3">
          <div className="skeleton h-11 w-11 shrink-0 rounded-full" />
          <div className="flex-1 space-y-2.5">
            <div className="skeleton h-3.5 w-1/2 rounded-md" />
            <div className="skeleton h-3 w-3/5 rounded-md" />
            <div className="skeleton h-3 w-1/3 rounded-md" />
          </div>
        </div>
      ))}
    </div>
  );
}
