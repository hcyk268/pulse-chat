import { Check, Shield, UserMinus, UserPlus, X } from "lucide-react";
import { useEffect, useState } from "react";
import { isSameId } from "../../utils/chat";
import Avatar from "../ui/Avatar";
import IconButton from "../ui/IconButton";

export default function GroupPanel({
  contacts,
  conversation,
  currentUser,
  onAcceptInvitation,
  onAddMembers,
  onClearSearch,
  onClose,
  onLeaveGroup,
  onRejectInvitation,
  onRemoveMember,
  onSearchUsers,
  onUpdateGroup,
  onUpdateRole,
  searchResults,
}) {
  const [avatarUrl, setAvatarUrl] = useState(conversation.avatarUrl ?? "");
  const [memberQuery, setMemberQuery] = useState("");
  const [name, setName] = useState(conversation.title ?? "");
  const [selectedIds, setSelectedIds] = useState([]);
  const memberIds = new Set(conversation.participants.map((member) => String(member.id)));
  const isOwner = conversation.currentUserRole === "OWNER";
  const isPendingInvitation = conversation.isPendingInvitation || conversation.currentUserStatus === "PENDING";
  const candidates = (memberQuery.trim() ? searchResults : contacts)
    .filter((contact) => contact.id && !memberIds.has(String(contact.id)) && !String(contact.id).startsWith("group-"));

  useEffect(() => {
    const query = memberQuery.trim();
    if (!query) {
      onClearSearch?.();
      return undefined;
    }

    const timer = window.setTimeout(() => onSearchUsers?.(query), 250);
    return () => window.clearTimeout(timer);
  }, [memberQuery, onClearSearch, onSearchUsers]);

  async function saveProfile() {
    await onUpdateGroup?.(conversation.id, { name, avatarUrl });
  }

  async function addMembers() {
    if (selectedIds.length === 0) return;
    const updated = await onAddMembers?.(conversation.id, selectedIds);
    if (updated) {
      setSelectedIds([]);
      setMemberQuery("");
    }
  }

  return (
    <aside className="absolute right-0 top-[65px] z-30 flex max-h-[calc(100%-65px)] w-full max-w-md flex-col overflow-hidden border-l border-white/5 bg-[#111827]/98 shadow-panel backdrop-blur-xl">
      <div className="flex items-center justify-between gap-3 border-b border-white/5 px-4 py-3">
        <div className="flex min-w-0 items-center gap-3">
          <Avatar user={conversation.otherParticipant} size="sm" />
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-white">Group info</p>
            <p className="text-xs text-slate-500">{conversation.participantCount} members</p>
          </div>
        </div>
        <IconButton icon={X} label="Close group info" onClick={onClose} />
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto p-4">
        {isPendingInvitation ? (
          <div className="mb-4 grid grid-cols-2 gap-2">
            <button type="button" onClick={() => onAcceptInvitation?.(conversation.id)} className="send-button flex items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white">
              <Check size={16} />
              Accept
            </button>
            <button type="button" onClick={() => onRejectInvitation?.(conversation.id)} className="press flex items-center justify-center gap-2 rounded-xl border border-rose-400/20 bg-rose-400/10 px-4 py-2.5 text-sm font-semibold text-rose-200 hover:bg-rose-400/15">
              <X size={16} />
              Reject
            </button>
          </div>
        ) : null}

        <div className="space-y-3">
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            disabled={!isOwner}
            maxLength={100}
            className="field-shell w-full rounded-xl border border-white/5 bg-[#1e293b] px-3 py-2.5 text-sm text-white outline-none disabled:text-slate-500"
          />
          <input
            value={avatarUrl}
            onChange={(event) => setAvatarUrl(event.target.value)}
            disabled={!isOwner}
            maxLength={500}
            placeholder="Avatar URL"
            className="field-shell w-full rounded-xl border border-white/5 bg-[#1e293b] px-3 py-2.5 text-sm text-white outline-none placeholder:text-slate-500 disabled:text-slate-500"
          />
          {isOwner ? (
            <button type="button" onClick={saveProfile} className="send-button flex w-full items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white">
              <Check size={16} />
              Save group
            </button>
          ) : null}
        </div>

        <div className="mt-5 border-t border-white/5 pt-4">
          <div className="mb-2 flex items-center justify-between">
            <p className="text-sm font-semibold text-white">Members</p>
            <Shield size={16} className="text-slate-500" />
          </div>
          <div className="space-y-1">
            {conversation.participants.map((member) => (
              <div key={member.id} className="flex items-center gap-3 rounded-xl px-2 py-2 hover:bg-white/[0.03]">
                <Avatar user={member} size="sm" showStatus />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-white">{member.displayName}</p>
                  <p className="text-xs text-slate-500">{member.role ?? "MEMBER"}</p>
                </div>
                {isOwner && !isSameId(member.id, currentUser.id) ? (
                  <select
                    value={member.role ?? "MEMBER"}
                    onChange={(event) => onUpdateRole?.(conversation.id, member.id, event.target.value)}
                    className="rounded-lg border border-white/5 bg-[#1e293b] px-2 py-1 text-xs text-slate-200 outline-none"
                  >
                    <option value="MEMBER">Member</option>
                    <option value="OWNER">Owner</option>
                  </select>
                ) : null}
                {isOwner && !isSameId(member.id, currentUser.id) ? (
                  <IconButton icon={UserMinus} label="Remove member" onClick={() => onRemoveMember?.(conversation.id, member.id)} />
                ) : null}
              </div>
            ))}
          </div>
        </div>

        {isOwner ? (
          <div className="mt-5 border-t border-white/5 pt-4">
            <p className="mb-2 text-sm font-semibold text-white">Add members</p>
            <input
              value={memberQuery}
              onChange={(event) => setMemberQuery(event.target.value)}
              placeholder="Search people..."
              className="field-shell w-full rounded-xl border border-white/5 bg-[#1e293b] px-3 py-2.5 text-sm text-white outline-none placeholder:text-slate-500"
            />
            <div className="mt-2 max-h-44 overflow-y-auto">
              {candidates.slice(0, 8).map((contact) => {
                const selected = selectedIds.includes(contact.id);
                return (
                  <button
                    key={contact.id}
                    type="button"
                    onClick={() => setSelectedIds((previous) => selected ? previous.filter((id) => !isSameId(id, contact.id)) : [...previous, contact.id])}
                    className="flex w-full items-center gap-3 rounded-xl px-2 py-2 text-left hover:bg-white/[0.03]"
                  >
                    <Avatar user={contact} size="sm" showStatus />
                    <span className="min-w-0 flex-1 truncate text-sm text-white">{contact.displayName}</span>
                    <span className={["flex h-5 w-5 items-center justify-center rounded-md border", selected ? "border-indigo-400 bg-indigo-500 text-white" : "border-white/10"].join(" ")}>
                      {selected ? <Check size={13} /> : null}
                    </span>
                  </button>
                );
              })}
            </div>
            <button type="button" onClick={addMembers} disabled={selectedIds.length === 0} className="send-button mt-3 flex w-full items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-[#1e293b] disabled:text-slate-600">
              <UserPlus size={16} />
              Add selected
            </button>
          </div>
        ) : null}
      </div>

      <div className="border-t border-white/5 p-4">
        <button type="button" onClick={() => onLeaveGroup?.(conversation.id)} className="press flex w-full items-center justify-center gap-2 rounded-xl border border-rose-400/20 bg-rose-400/10 px-4 py-2.5 text-sm font-semibold text-rose-200 hover:bg-rose-400/15">
          <UserMinus size={16} />
          Leave group
        </button>
      </div>
    </aside>
  );
}
