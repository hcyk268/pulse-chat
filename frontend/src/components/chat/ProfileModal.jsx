import { AlertCircle, AtSign, Check, LoaderCircle, Mail, Save, Trash2, Upload, UserRound, X } from "lucide-react";
import { forwardRef, useEffect, useRef, useState } from "react";
import { uploadAvatar } from "../../services/uploadApi";
import { hasNoHtmlAngleBrackets } from "../../utils/validators";
import Avatar from "../ui/Avatar";
import IconButton from "../ui/IconButton";

const MAX_AVATAR_SIZE_BYTES = 25 * 1024 * 1024;
const ACCEPTED_AVATAR_TYPES = new Set(["image/jpeg", "image/png", "image/webp", "image/gif"]);

function profileFormFromUser(user) {
  return {
    displayName: user.displayName ?? "",
    avatarUrl: user.avatarUrl ?? "",
    bio: user.bio ?? "",
  };
}

export default function ProfileModal({ currentUser, onClose, onSave }) {
  const [form, setForm] = useState(() => profileFormFromUser(currentUser));
  const [avatarFile, setAvatarFile] = useState(null);
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState(null);
  const [avatarUploadProgress, setAvatarUploadProgress] = useState(null);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const displayNameInputRef = useRef(null);
  const avatarInputRef = useRef(null);

  useEffect(() => {
    displayNameInputRef.current?.focus();

    function handleKeyDown(event) {
      if (event.key === "Escape" && !isSaving) onClose();
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [isSaving, onClose]);

  useEffect(() => {
    if (!avatarFile) {
      setAvatarPreviewUrl(null);
      return undefined;
    }

    const previewUrl = URL.createObjectURL(avatarFile);
    setAvatarPreviewUrl(previewUrl);
    return () => URL.revokeObjectURL(previewUrl);
  }, [avatarFile]);

  function updateField(field, value) {
    setSaved(false);
    setError("");
    setForm((previous) => ({ ...previous, [field]: value }));
  }

  function handleAvatarChange(event) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;

    if (!ACCEPTED_AVATAR_TYPES.has(file.type)) {
      setError("Choose a JPG, PNG, WebP, or GIF image.");
      return;
    }

    if (file.size > MAX_AVATAR_SIZE_BYTES) {
      setError("Avatar image must be 25 MB or smaller.");
      return;
    }

    setError("");
    setSaved(false);
    setAvatarFile(file);
  }

  function handleRemoveAvatar() {
    setAvatarFile(null);
    setAvatarUploadProgress(null);
    updateField("avatarUrl", "");
  }

  async function handleSubmit(event) {
    event.preventDefault();

    const displayName = form.displayName.trim();
    const bio = form.bio.trim();

    if (!displayName) {
      setError("Display name must not be blank.");
      return;
    }

    if (displayName.length > 100 || !hasNoHtmlAngleBrackets(displayName)) {
      setError("Display name must be 100 characters or fewer and cannot contain < or >.");
      return;
    }

    if (bio.length > 500 || !hasNoHtmlAngleBrackets(bio)) {
      setError("Bio must be 500 characters or fewer and cannot contain < or >.");
      return;
    }

    setIsSaving(true);
    setAvatarUploadProgress(avatarFile ? 0 : null);
    try {
      const uploadedAvatar = avatarFile
        ? await uploadAvatar(avatarFile, { onProgress: setAvatarUploadProgress })
        : null;
      if (avatarFile && !uploadedAvatar?.url) {
        throw new Error("Avatar upload completed but no public URL was returned. Configure R2_PUBLIC_BASE_URL.");
      }
      await onSave({
        displayName,
        avatarUrl: uploadedAvatar?.url ?? form.avatarUrl.trim(),
        bio,
      });
      setAvatarFile(null);
      setSaved(true);
    } catch (profileError) {
      setError(profileError.message || "Could not update your profile.");
    } finally {
      setAvatarUploadProgress(null);
      setIsSaving(false);
    }
  }

  const previewUser = {
    ...currentUser,
    displayName: form.displayName.trim() || currentUser.displayName,
    avatarUrl: (avatarPreviewUrl ?? form.avatarUrl.trim()) || null,
  };
  const avatarActionLabel = avatarFile || form.avatarUrl ? "Change photo" : "Upload photo";

  return (
    <div
      className="sheet-overlay fixed inset-0 z-50 flex items-center justify-center bg-slate-950/75 px-4 py-6 backdrop-blur-sm"
      onClick={(event) => {
        if (event.target === event.currentTarget && !isSaving) onClose();
      }}
    >
      <section
        aria-labelledby="profile-modal-title"
        aria-modal="true"
        role="dialog"
        className="profile-modal-panel w-full max-w-xl overflow-hidden rounded-3xl border border-white/10 bg-[#111827] shadow-panel"
      >
        <header className="relative overflow-hidden border-b border-white/5 px-5 pb-5 pt-5 sm:px-6">
          <div className="absolute inset-x-0 top-0 h-24 bg-[radial-gradient(ellipse_at_top,rgba(99,102,241,0.28),transparent_68%)]" />
          <div className="relative flex items-start justify-between gap-4">
            <div className="flex min-w-0 items-center gap-3">
              <Avatar user={previewUser} size="lg" />
              <div className="min-w-0">
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-indigo-300">Your account</p>
                <h2 id="profile-modal-title" className="mt-1 truncate text-xl font-bold text-white">
                  {previewUser.displayName}
                </h2>
                <p className="mt-1 flex items-center gap-1.5 text-sm text-slate-400">
                  <AtSign size={14} />
                  {currentUser.username}
                </p>
              </div>
            </div>
            <IconButton disabled={isSaving} icon={X} label="Close profile" onClick={onClose} />
          </div>
        </header>

        <form onSubmit={handleSubmit} className="space-y-4 p-5 sm:p-6">
          <label className="block">
            <span className="text-sm font-medium text-slate-200">Display name</span>
            <ProfileInput
              ref={displayNameInputRef}
              icon={UserRound}
              maxLength={100}
              onChange={(value) => updateField("displayName", value)}
              value={form.displayName}
            />
          </label>

          <div>
            <span className="text-sm font-medium text-slate-200">Avatar</span>
            <div className="mt-2 flex items-center gap-3 rounded-2xl border border-dashed border-indigo-300/20 bg-indigo-500/[0.035] p-3.5">
              <Avatar user={previewUser} size="md" />
              <div className="min-w-0 flex-1">
                <input
                  ref={avatarInputRef}
                  accept="image/jpeg,image/png,image/webp,image/gif"
                  className="sr-only"
                  onChange={handleAvatarChange}
                  type="file"
                />
                <button
                  className="press flex items-center gap-2 text-sm font-semibold text-indigo-200 hover:text-white disabled:cursor-not-allowed disabled:text-slate-500"
                  disabled={isSaving}
                  onClick={() => avatarInputRef.current?.click()}
                  type="button"
                >
                  <Upload size={16} />
                  {avatarActionLabel}
                </button>
                <p className="mt-1 truncate text-xs text-slate-500">
                  {avatarFile?.name ?? "JPG, PNG, WebP or GIF. Maximum 25 MB."}
                </p>
                {typeof avatarUploadProgress === "number" ? (
                  <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-white/5">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-indigo-400 to-violet-400 transition-[width] duration-200"
                      style={{ width: `${avatarUploadProgress}%` }}
                    />
                  </div>
                ) : null}
              </div>
              {avatarFile || form.avatarUrl ? (
                <IconButton disabled={isSaving} icon={Trash2} label="Remove avatar" onClick={handleRemoveAvatar} />
              ) : null}
            </div>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <ReadOnlyField icon={AtSign} label="Username" value={currentUser.username} />
            <ReadOnlyField icon={Mail} label="Email" value={currentUser.email} />
          </div>

          <label className="block">
            <span className="text-sm font-medium text-slate-200">Bio</span>
            <textarea
              className="field-shell mt-2 min-h-28 w-full resize-y rounded-xl border border-white/5 bg-[#1e293b] px-4 py-3 text-sm leading-6 text-white outline-none placeholder:text-slate-500"
              maxLength={500}
              onChange={(event) => updateField("bio", event.target.value)}
              placeholder="Add a short status or introduction"
              value={form.bio}
            />
            <p className="mt-1.5 text-right text-xs text-slate-500">{form.bio.length}/500</p>
          </label>

          {error ? (
            <div className="flex items-start gap-3 rounded-xl border border-rose-400/20 bg-rose-400/10 p-3 text-sm leading-5 text-rose-100">
              <AlertCircle size={18} className="mt-0.5 shrink-0" />
              <span>{error}</span>
            </div>
          ) : null}

          {saved ? (
            <div className="flex items-center gap-2 text-sm text-emerald-300">
              <Check size={17} />
              Profile updated.
            </div>
          ) : null}

          <div className="flex flex-col-reverse gap-3 border-t border-white/5 pt-5 sm:flex-row sm:justify-end">
            <button
              className="press rounded-xl px-4 py-2.5 text-sm font-semibold text-slate-300 hover:bg-white/5 hover:text-white disabled:cursor-not-allowed disabled:text-slate-600"
              disabled={isSaving}
              onClick={onClose}
              type="button"
            >
              Cancel
            </button>
            <button
              className="send-button flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-5 py-2.5 text-sm font-semibold text-white shadow-send hover:shadow-send-hover disabled:cursor-not-allowed disabled:bg-slate-800 disabled:bg-none disabled:text-slate-500 disabled:shadow-none"
              disabled={isSaving}
              type="submit"
            >
              {isSaving ? <LoaderCircle className="animate-spin" size={17} /> : <Save size={17} />}
              {isSaving ? (avatarFile ? "Uploading avatar..." : "Saving...") : "Save changes"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

const ProfileInput = forwardRef(function ProfileInput({ icon: Icon, onChange, ...inputProps }, ref) {
  return (
    <span className="field-shell mt-2 flex items-center gap-3 rounded-xl border border-white/5 bg-[#1e293b] px-3.5 py-3">
      <Icon aria-hidden="true" size={17} className="shrink-0 text-slate-500" />
      <input
        {...inputProps}
        ref={ref}
        className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-slate-500"
        onChange={(event) => onChange(event.target.value)}
      />
    </span>
  );
});

function ReadOnlyField({ icon: Icon, label, value }) {
  return (
    <div>
      <p className="text-sm font-medium text-slate-400">{label}</p>
      <div className="mt-2 flex min-w-0 items-center gap-3 rounded-xl border border-white/5 bg-slate-900/40 px-3.5 py-3 text-sm text-slate-500">
        <Icon aria-hidden="true" size={17} className="shrink-0" />
        <span className="truncate">{value}</span>
      </div>
    </div>
  );
}
