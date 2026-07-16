import { AlertCircle, Check, ImagePlus, LoaderCircle, Search, Trash2, UserPlus, Users, X } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { useAppSettings } from "../../hooks/useAppSettings";
import { useToast } from "../../hooks/useToast";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { uploadAvatar } from "../../services/uploadApi";
import { formatPresence } from "../../utils/formatters";
import { PeopleSearchAsset } from "../assets/MicroAssets";
import Avatar from "../ui/Avatar";

const MAX_GROUP_AVATAR_SIZE_BYTES = 25 * 1024 * 1024;
const ACCEPTED_GROUP_AVATAR_TYPES = new Set(["image/jpeg", "image/png", "image/webp", "image/gif"]);

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
  const toast = useToast();
  const { settings } = useAppSettings();
  const [mode, setMode] = useState("direct");
  const [query, setQuery] = useState("");
  const [groupName, setGroupName] = useState("");
  const [groupAvatarUrl, setGroupAvatarUrl] = useState("");
  const [groupAvatarFile, setGroupAvatarFile] = useState(null);
  const [groupAvatarPreviewUrl, setGroupAvatarPreviewUrl] = useState(null);
  const [groupAvatarUploadProgress, setGroupAvatarUploadProgress] = useState(null);
  const [groupActionError, setGroupActionError] = useState("");
  const [isCreatingGroup, setIsCreatingGroup] = useState(false);
  const [selectedContacts, setSelectedContacts] = useState([]);
  const groupAvatarInputRef = useRef(null);
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

  useEffect(() => {
    if (!groupAvatarFile) {
      setGroupAvatarPreviewUrl(null);
      return undefined;
    }

    const previewUrl = URL.createObjectURL(groupAvatarFile);
    setGroupAvatarPreviewUrl(previewUrl);
    return () => URL.revokeObjectURL(previewUrl);
  }, [groupAvatarFile]);

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
    if (isCreatingGroup) return;

    setIsCreatingGroup(true);
    setGroupActionError("");
    setGroupAvatarUploadProgress(groupAvatarFile && !groupAvatarUrl ? 0 : null);

    try {
      let avatarUrl = groupAvatarUrl;

      if (groupAvatarFile && !avatarUrl) {
        const uploadedAvatar = await uploadAvatar(groupAvatarFile, {
          onProgress: setGroupAvatarUploadProgress,
        });

        if (!uploadedAvatar?.url) {
          throw new Error("Image uploaded but no public URL was returned.");
        }

        avatarUrl = uploadedAvatar.url;
        setGroupAvatarUrl(avatarUrl);
      }

      const memberIds = selectedContacts.map((contact) => contact.backendId ?? contact.id);
      const nextConversationId = await onCreateGroup?.({
        name: groupName,
        avatarUrl: avatarUrl || null,
        memberIds,
      });

      if (nextConversationId) {
        setGroupName("");
        setGroupAvatarUrl("");
        setGroupAvatarFile(null);
        setSelectedContacts([]);
      }
    } catch (error) {
      const message = error.message || "Could not upload the group photo.";
      setGroupActionError(message);
      toast.error(message);
    } finally {
      setGroupAvatarUploadProgress(null);
      setIsCreatingGroup(false);
    }
  }

  function handleGroupAvatarChange(event) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;

    if (!ACCEPTED_GROUP_AVATAR_TYPES.has(file.type)) {
      setGroupActionError("Choose a JPG, PNG, WebP, or GIF image.");
      return;
    }

    if (file.size > MAX_GROUP_AVATAR_SIZE_BYTES) {
      setGroupActionError("Group photo must be 25 MB or smaller.");
      return;
    }

    setGroupActionError("");
    setGroupAvatarUrl("");
    setGroupAvatarFile(file);
  }

  function handleRemoveGroupAvatar() {
    setGroupActionError("");
    setGroupAvatarUrl("");
    setGroupAvatarFile(null);
    setGroupAvatarUploadProgress(null);
  }

  const isBusy = isStartingConversation || isCreatingGroup;
  const canCreateGroup = groupName.trim().length > 0 && selectedContacts.length >= 2 && !isBusy;
  const groupPreviewUser = {
    displayName: groupName.trim() || "Group",
    avatarUrl: (groupAvatarPreviewUrl ?? groupAvatarUrl) || null,
  };

  return (
    <aside className="contact-panel flex h-full min-h-0 w-full flex-col bg-[#111827]">
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
          <ModeButton active={mode === "direct"} disabled={isBusy} icon={UserPlus} label="Direct" onClick={() => setMode("direct")} />
          <ModeButton active={mode === "group"} disabled={isBusy} icon={Users} label="Group" onClick={() => setMode("group")} />
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
            <div className="flex items-center gap-3 rounded-xl border border-dashed border-indigo-300/20 bg-indigo-500/[0.035] p-3">
              <Avatar user={groupPreviewUser} size="md" />
              <div className="min-w-0 flex-1">
                <input
                  ref={groupAvatarInputRef}
                  accept="image/jpeg,image/png,image/webp,image/gif"
                  className="sr-only"
                  disabled={isBusy}
                  onChange={handleGroupAvatarChange}
                  type="file"
                />
                <button
                  type="button"
                  disabled={isBusy}
                  onClick={() => groupAvatarInputRef.current?.click()}
                  className="press flex min-h-8 items-center gap-2 text-sm font-semibold text-indigo-200 hover:text-white disabled:cursor-not-allowed disabled:text-slate-600"
                >
                  <ImagePlus size={16} />
                  {groupAvatarFile || groupAvatarUrl ? "Change group photo" : "Upload group photo"}
                </button>
                <p className="mt-0.5 truncate text-xs text-slate-500">
                  {groupAvatarFile?.name ?? "JPG, PNG, WebP or GIF · Max 25 MB"}
                </p>
                {typeof groupAvatarUploadProgress === "number" ? (
                  <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-white/5">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-indigo-400 to-violet-400 transition-[width] duration-200"
                      style={{ width: `${groupAvatarUploadProgress}%` }}
                    />
                  </div>
                ) : null}
              </div>
              {groupAvatarFile || groupAvatarUrl ? (
                <button
                  type="button"
                  aria-label="Remove group photo"
                  disabled={isBusy}
                  onClick={handleRemoveGroupAvatar}
                  className="press flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-slate-500 hover:bg-rose-400/10 hover:text-rose-300 disabled:cursor-not-allowed disabled:text-slate-700"
                >
                  <Trash2 size={16} />
                </button>
              ) : null}
            </div>
            {groupActionError ? (
              <p role="alert" className="flex items-start gap-2 rounded-xl border border-rose-400/15 bg-rose-400/10 px-3 py-2 text-xs leading-5 text-rose-200">
                <AlertCircle size={15} className="mt-0.5 shrink-0" />
                {groupActionError}
              </p>
            ) : null}
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
                  disabled={isBusy}
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
            {isCreatingGroup ? <LoaderCircle className="animate-spin" size={18} /> : <Users size={18} />}
            {isCreatingGroup
              ? typeof groupAvatarUploadProgress === "number"
                ? `Uploading photo ${groupAvatarUploadProgress}%`
                : "Creating group..."
              : "Create group"}
          </button>
        </div>
      ) : null}
    </aside>
  );
}

function ModeButton({ active, disabled = false, icon: Icon, label, onClick }) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className={[
        "press flex items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-50",
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
