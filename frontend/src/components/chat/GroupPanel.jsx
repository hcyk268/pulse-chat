import { Check, Shield, UserMinus, UserPlus, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { AlertCircle, ImagePlus, LoaderCircle, Trash2 } from "lucide-react";
import { uploadAvatar } from "../../services/uploadApi";
import { useToast } from "../../hooks/useToast";
import { isSameId } from "../../utils/chat";
import Avatar from "../ui/Avatar";
import IconButton from "../ui/IconButton";

const MAX_GROUP_AVATAR_SIZE_BYTES = 25 * 1024 * 1024;
const ACCEPTED_GROUP_AVATAR_TYPES = new Set(["image/jpeg", "image/png", "image/webp", "image/gif"]);

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
  const toast = useToast();
  const [avatarUrl, setAvatarUrl] = useState(conversation.avatarUrl ?? "");
  const [avatarFile, setAvatarFile] = useState(null);
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState(null);
  const [avatarUploadProgress, setAvatarUploadProgress] = useState(null);
  const [actionError, setActionError] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [memberQuery, setMemberQuery] = useState("");
  const [name, setName] = useState(conversation.title ?? "");
  const [selectedIds, setSelectedIds] = useState([]);
  const avatarInputRef = useRef(null);
  const memberIds = new Set(conversation.participants.map((member) => String(member.id)));
  const isOwner = conversation.currentUserRole === "OWNER";
  const isPendingInvitation = conversation.isPendingInvitation || conversation.currentUserStatus === "PENDING";
  const canManageGroup = !isPendingInvitation && conversation.currentUserStatus !== "LEFT";
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

  useEffect(() => {
    setName(conversation.title ?? "");
    setAvatarUrl(conversation.avatarUrl ?? "");
    setAvatarFile(null);
    setActionError("");
  }, [conversation.id, conversation.title, conversation.avatarUrl]);

  useEffect(() => {
    if (!avatarFile) {
      setAvatarPreviewUrl(null);
      return undefined;
    }

    const previewUrl = URL.createObjectURL(avatarFile);
    setAvatarPreviewUrl(previewUrl);
    return () => URL.revokeObjectURL(previewUrl);
  }, [avatarFile]);

  async function saveProfile() {
    if (isSaving || !name.trim()) return;
    setIsSaving(true);
    setActionError("");
    setAvatarUploadProgress(avatarFile && !avatarUrl ? 0 : null);

    try {
      let nextAvatarUrl = avatarUrl;
      if (avatarFile && !nextAvatarUrl) {
        const uploaded = await uploadAvatar(avatarFile, { onProgress: setAvatarUploadProgress });
        if (!uploaded?.url) throw new Error("Image uploaded but no public URL was returned.");
        nextAvatarUrl = uploaded.url;
      }

      const updated = await onUpdateGroup?.(conversation.id, { name, avatarUrl: nextAvatarUrl });
      if (updated) {
        setAvatarUrl(nextAvatarUrl);
        setAvatarFile(null);
        toast.success("Group details updated.");
      }
    } catch (error) {
      const message = error.message || "Could not update the group.";
      setActionError(message);
      toast.error(message);
    } finally {
      setAvatarUploadProgress(null);
      setIsSaving(false);
    }
  }

  function handleAvatarChange(event) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    if (!ACCEPTED_GROUP_AVATAR_TYPES.has(file.type)) {
      setActionError("Choose a JPG, PNG, WebP, or GIF image.");
      return;
    }
    if (file.size > MAX_GROUP_AVATAR_SIZE_BYTES) {
      setActionError("Group photo must be 25 MB or smaller.");
      return;
    }
    setActionError("");
    setAvatarUrl("");
    setAvatarFile(file);
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
    <aside className="group-panel absolute right-0 top-[65px] z-30 flex max-h-[calc(100%-65px)] w-full max-w-md flex-col overflow-hidden border-l border-white/5 bg-[#111827]/98 shadow-panel backdrop-blur-xl">
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

        {canManageGroup ? <div className="space-y-3">
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            disabled={isSaving}
            maxLength={100}
            className="field-shell w-full rounded-xl border border-white/5 bg-[#1e293b] px-3 py-2.5 text-sm text-white outline-none disabled:text-slate-500"
          />
          <div className="flex items-center gap-3 rounded-xl border border-dashed border-indigo-300/20 bg-indigo-500/[0.035] p-3">
            <Avatar user={{ displayName: name || "Group", avatarUrl: avatarPreviewUrl ?? avatarUrl }} size="md" />
            <div className="min-w-0 flex-1">
              <input ref={avatarInputRef} type="file" accept="image/jpeg,image/png,image/webp,image/gif" className="sr-only" disabled={isSaving} onChange={handleAvatarChange} />
              <button type="button" disabled={isSaving} onClick={() => avatarInputRef.current?.click()} className="press flex items-center gap-2 text-sm font-semibold text-indigo-200 hover:text-white disabled:text-slate-600">
                <ImagePlus size={16} />
                {avatarFile || avatarUrl ? "Change group photo" : "Upload group photo"}
              </button>
              <p className="mt-0.5 truncate text-xs text-slate-500">{avatarFile?.name ?? "JPG, PNG, WebP or GIF · Max 25 MB"}</p>
              {typeof avatarUploadProgress === "number" ? <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-white/5"><div className="h-full rounded-full bg-indigo-400 transition-[width]" style={{ width: `${avatarUploadProgress}%` }} /></div> : null}
            </div>
            {avatarFile || avatarUrl ? <button type="button" aria-label="Remove group photo" disabled={isSaving} onClick={() => { setAvatarFile(null); setAvatarUrl(""); }} className="press flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-slate-500 hover:bg-rose-400/10 hover:text-rose-300 disabled:text-slate-700"><Trash2 size={16} /></button> : null}
          </div>
          {actionError ? <p role="alert" className="flex items-start gap-2 rounded-xl border border-rose-400/15 bg-rose-400/10 px-3 py-2 text-xs leading-5 text-rose-200"><AlertCircle size={15} className="mt-0.5 shrink-0" />{actionError}</p> : null}
          <button type="button" onClick={saveProfile} disabled={isSaving || !name.trim()} className="send-button flex w-full items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-[#1e293b] disabled:text-slate-600">
            {isSaving ? <LoaderCircle size={16} className="animate-spin" /> : <Check size={16} />}
            {isSaving ? "Saving..." : "Save group"}
          </button>
        </div> : null}

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
                  <p className="text-xs text-slate-500">{member.status === "PENDING" ? "Invited · pending" : member.role ?? "MEMBER"}</p>
                </div>
                {isOwner && member.status !== "PENDING" && !isSameId(member.id, currentUser.id) ? (
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

        {canManageGroup ? (
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

      {!isPendingInvitation ? <div className="border-t border-white/5 p-4">
        <button type="button" onClick={() => onLeaveGroup?.(conversation.id)} className="press flex w-full items-center justify-center gap-2 rounded-xl border border-rose-400/20 bg-rose-400/10 px-4 py-2.5 text-sm font-semibold text-rose-200 hover:bg-rose-400/15">
          <UserMinus size={16} />
          Leave group
        </button>
      </div> : null}
    </aside>
  );
}
