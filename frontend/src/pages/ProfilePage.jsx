import { AlertCircle, ArrowLeft, Check, Image, LogOut, Mail, Save, UserRound } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ProfilePulseAsset } from "../components/assets/MicroAssets";
import Avatar from "../components/ui/Avatar";
import StatusPill from "../components/ui/StatusPill";
import { useChatStore } from "../hooks/useChatStore";
import { hasNoHtmlAngleBrackets, isOptionalHttpUrl } from "../utils/validators";

export default function ProfilePage() {
  const { currentUser, signOut, stats, updateProfile } = useChatStore();
  const [form, setForm] = useState({
    displayName: currentUser.displayName,
    email: currentUser.email,
    avatarUrl: currentUser.avatarUrl ?? "",
    bio: currentUser.bio ?? "",
  });
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState("");
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    setForm({
      displayName: currentUser.displayName,
      email: currentUser.email,
      avatarUrl: currentUser.avatarUrl ?? "",
      bio: currentUser.bio ?? "",
    });
  }, [currentUser]);

  function updateField(field, value) {
    setSaved(false);
    setError("");
    setForm((previous) => ({ ...previous, [field]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (!form.displayName.trim()) {
      setError("Display name must not be blank.");
      return;
    }

    const displayName = form.displayName.trim();
    const avatarUrl = form.avatarUrl.trim();
    const bio = form.bio.trim();

    if (displayName.length > 100 || !hasNoHtmlAngleBrackets(displayName)) {
      setError("Display name must be 100 characters or fewer and cannot contain < or >.");
      return;
    }

    if (avatarUrl.length > 500 || !isOptionalHttpUrl(avatarUrl)) {
      setError("Avatar URL must be empty or start with http:// or https://.");
      return;
    }

    if (bio.length > 500 || !hasNoHtmlAngleBrackets(bio)) {
      setError("Status message must be 500 characters or fewer and cannot contain < or >.");
      return;
    }

    setIsSaving(true);
    setError("");

    try {
      await updateProfile({
        displayName,
        avatarUrl,
        bio,
      });
      setSaved(true);
    } catch (profileError) {
      setError(profileError.message || "Could not update your profile.");
    } finally {
      setIsSaving(false);
    }
  }

  const previewUser = {
    ...currentUser,
    displayName: form.displayName.trim() || currentUser.displayName,
    avatarUrl: form.avatarUrl.trim() || null,
    bio: form.bio.trim(),
  };

  return (
    <div className="min-h-screen bg-[#0a0f1a] text-white">
      <main className="mx-auto max-w-5xl p-4 sm:p-6">
        <header className="mb-4 flex animate-enter-up items-center gap-3 rounded-2xl border border-white/5 bg-[#111827] px-4 py-3 shadow-card">
          <Link
            to="/chat"
            className="press flex h-10 w-10 items-center justify-center rounded-xl text-slate-400 hover:bg-white/5 hover:text-white"
            title="Back to chat"
          >
            <ArrowLeft size={20} />
          </Link>
          <div>
            <h1 className="text-lg font-bold text-white">Profile</h1>
            <p className="text-xs text-slate-500">Account settings</p>
          </div>
        </header>

        <div className="grid gap-4 lg:grid-cols-[0.85fr_1.15fr]">
          <section className="animate-enter-up rounded-2xl border border-white/5 bg-[#111827] p-5 shadow-card">
            <ProfilePulseAsset className="h-auto w-full" />
            <div className="mt-5 flex items-center gap-4">
              <Avatar user={previewUser} size="xl" />
              <div>
                <h1 className="bg-gradient-to-r from-white via-slate-100 to-slate-300 bg-clip-text text-3xl font-bold tracking-tight text-transparent">
                  {previewUser.displayName}
                </h1>
                <p className="mt-2 text-sm text-slate-500">@{currentUser.username}</p>
              </div>
            </div>
            <p className="mt-5 text-sm leading-6 text-slate-400">{previewUser.bio}</p>

            <div className="mt-7 grid grid-cols-3 gap-3">
              <ProfileMetric label="Chats" value={stats.activeConversations} tone="indigo" />
              <ProfileMetric label="Online" value={stats.onlineCount} tone="emerald" />
              <ProfileMetric label="Unread" value={stats.unreadTotal} tone="amber" />
            </div>

            <Link
              to="/login"
              onClick={() => signOut()}
              className="press lift mt-7 flex w-full items-center justify-center gap-2 rounded-xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 font-medium text-rose-200 hover:bg-rose-400/15"
            >
              <LogOut size={18} />
              Sign out
            </Link>
          </section>

          <section className="animate-enter-up rounded-2xl border border-white/5 bg-[#111827] p-5 shadow-card [animation-delay:80ms]">
            <div className="flex flex-col justify-between gap-4 border-b border-white/5 pb-6 sm:flex-row sm:items-center">
              <div>
                <p className="text-sm font-medium text-indigo-400">Account</p>
                <h2 className="mt-1 bg-gradient-to-r from-white via-slate-100 to-slate-300 bg-clip-text text-2xl font-bold text-transparent">
                  Profile settings
                </h2>
              </div>
              {isSaving ? (
                <StatusPill tone="indigo">Saving</StatusPill>
              ) : saved ? (
                <StatusPill tone="emerald">Saved</StatusPill>
              ) : (
                <StatusPill>Draft</StatusPill>
              )}
            </div>

            <form onSubmit={handleSubmit} className="mt-6 space-y-5">
              <label className="block">
                <span className="text-sm font-medium text-slate-300">Display name</span>
                <ProfileField
                  icon={UserRound}
                  value={form.displayName}
                  onChange={(value) => updateField("displayName", value)}
                  maxLength={100}
                />
              </label>
              <label className="block">
                <span className="text-sm font-medium text-slate-300">Avatar URL</span>
                <ProfileField
                  icon={Image}
                  type="url"
                  value={form.avatarUrl}
                  onChange={(value) => updateField("avatarUrl", value)}
                  placeholder="https://example.com/avatar.jpg"
                  maxLength={500}
                />
              </label>
              <label className="block">
                <span className="text-sm font-medium text-slate-300">Email</span>
                <ProfileField
                  icon={Mail}
                  value={form.email}
                  onChange={(value) => updateField("email", value)}
                  disabled
                />
                <p className="mt-2 text-xs text-slate-600">
                  Email changes are not supported by the current backend API.
                </p>
              </label>
              <label className="block">
                <span className="text-sm font-medium text-slate-300">Status message</span>
                <textarea
                  value={form.bio}
                  onChange={(event) => updateField("bio", event.target.value)}
                  rows={5}
                  maxLength={500}
                  className="field-shell mt-2 w-full resize-none rounded-xl border border-white/5 bg-[#1e293b] px-4 py-3 text-sm leading-6 text-white outline-none placeholder:text-slate-500"
                />
              </label>

              {error ? (
                <div className="flex animate-scale-in items-start gap-3 rounded-xl border border-rose-400/20 bg-rose-400/10 p-3 text-sm leading-5 text-rose-200">
                  <AlertCircle size={18} className="mt-0.5 shrink-0" />
                  <span>{error}</span>
                </div>
              ) : null}

              <div className="flex flex-col gap-3 pt-2 sm:flex-row">
                <button
                  type="submit"
                  disabled={isSaving}
                  className="send-button flex flex-1 items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 via-indigo-500 to-purple-600 px-5 py-3 font-semibold text-white shadow-send hover:shadow-send-hover disabled:cursor-not-allowed disabled:bg-slate-800 disabled:bg-none disabled:text-slate-500 disabled:shadow-none"
                >
                  <span className="relative z-[1] flex items-center gap-2">
                    <Save size={18} />
                    {isSaving ? "Saving..." : "Save changes"}
                  </span>
                </button>
                <Link
                  to="/chat"
                  className="press lift flex flex-1 items-center justify-center gap-2 rounded-xl border border-white/5 bg-[#1e293b] px-5 py-3 font-semibold text-slate-200 hover:bg-[#334155]"
                >
                  <Check size={18} />
                  Back to chat
                </Link>
              </div>
            </form>
          </section>
        </div>
      </main>
    </div>
  );
}

function ProfileMetric({ value, label, tone }) {
  return (
    <div className="lift rounded-xl border border-white/5 bg-gradient-to-br from-[#1e293b] to-[#111827] p-3 hover:border-white/10">
      <p className="text-2xl font-bold text-white">{value}</p>
      <div className="mt-2">
        <StatusPill tone={tone}>{label}</StatusPill>
      </div>
    </div>
  );
}

function ProfileField({
  icon: Icon,
  value,
  onChange,
  disabled = false,
  maxLength = undefined,
  placeholder = "",
  type = "text",
}) {
  return (
    <div className="field-shell mt-2 flex items-center gap-3 rounded-xl border border-white/5 bg-[#1e293b] px-4 py-3.5">
      <Icon size={18} className="text-slate-500" />
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        disabled={disabled}
        maxLength={maxLength}
        placeholder={placeholder}
        className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none disabled:cursor-not-allowed disabled:text-slate-600"
      />
    </div>
  );
}
