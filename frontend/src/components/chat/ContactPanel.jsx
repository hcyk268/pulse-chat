import { Search, UserPlus, X } from "lucide-react";
import { useMemo, useState } from "react";
import { formatPresence } from "../../utils/formatters";
import Avatar from "../ui/Avatar";

export default function ContactPanel({ contacts, onClose, onStartConversation }) {
  const [query, setQuery] = useState("");

  const filteredContacts = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return contacts;

    return contacts.filter((contact) => {
      return [contact.displayName, contact.username, contact.email, contact.role]
        .join(" ")
        .toLowerCase()
        .includes(normalized);
    });
  }, [contacts, query]);

  return (
    <aside className="flex h-full min-h-0 w-full flex-col bg-[#17212b]">
      <div className="border-b border-black/25 px-4 py-3">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold text-white">New Message</h2>
            <p className="mt-0.5 text-xs text-slate-400">Search active users</p>
          </div>
          {onClose ? (
            <button
              type="button"
              onClick={onClose}
              className="flex h-10 w-10 items-center justify-center rounded-full text-slate-300 transition hover:bg-white/10 hover:text-white"
              title="Close"
            >
              <X size={18} />
            </button>
          ) : null}
        </div>

        <div className="mt-3 flex items-center gap-2 rounded-full bg-[#242f3d] px-3 py-2 transition focus-within:bg-[#2b3948]">
          <Search size={18} className="text-slate-400" />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search"
            className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-slate-400"
          />
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto">
        <div>
          {filteredContacts.map((contact) => (
            <button
              key={contact.id}
              type="button"
              onClick={() => onStartConversation(contact)}
              className="group flex w-full items-center gap-3 px-4 py-3 text-left transition duration-150 hover:bg-[#202b36]"
            >
              <Avatar user={contact} size="md" showStatus />
              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-2">
                  <p className="truncate text-sm font-semibold text-white">{contact.displayName}</p>
                  <UserPlus
                    size={17}
                    className="shrink-0 text-slate-500 transition group-hover:text-[#2aabee]"
                  />
                </div>
                <p className="truncate text-xs text-slate-400">
                  {contact.role} - @{contact.username}
                </p>
                <p className="mt-1 truncate text-xs text-[#6ab7ee]">
                  {formatPresence(contact.presence)}
                </p>
              </div>
            </button>
          ))}
        </div>
      </div>
    </aside>
  );
}
