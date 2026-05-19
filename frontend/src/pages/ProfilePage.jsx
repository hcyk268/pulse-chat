import { AlertCircle, ArrowLeft, Check, LogOut, Mail, Save, UserRound } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ProfilePulseAsset } from "../components/assets/MicroAssets";
import Avatar from "../components/ui/Avatar";
import StatusPill from "../components/ui/StatusPill";
import { useChatStore } from "../hooks/useChatStore";

export default function ProfilePage() {
  const { currentUser, signOut, stats, updateProfile } = useChatStore();
  const [form, setForm] = useState({
    displayName: currentUser.displayName,
    email: currentUser.email,
    bio: currentUser.bio ?? "",
  });
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState("");
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    setForm({
      displayName: currentUser.displayName,
      email: currentUser.email,
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

    setIsSaving(true);
    setError("");

    try {
      await updateProfile({
        displayName: form.displayName,
        bio: form.bio,
      });
      setSaved(true);
    } catch (profileError) {
      setError(profileError.message || "Could not update your profile.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <div className="min-h-screen bg-[#0e1621] text-white">
      <main className="mx-auto max-w-5xl p-4 sm:p-6">
        <header className="mb-4 flex animate-enter-up items-center gap-3 rounded-2xl border border-white/5 bg-[#17212b] px-3 py-2.5 shadow-panel-soft">
          <Link
            to="/chat"
            className="press flex h-10 w-10 items-center justify-center rounded-full text-slate-300 hover:bg-white/10 hover:text-white"
            title="Back to chat"
          >
            <ArrowLeft size={20} />
          </Link>
          <div>
            <h1 className="text-lg font-semibold text-white">Profile</h1>
            <p className="text-xs text-slate-400">Account settings</p>
          </div>
        </header>

        <div className="grid gap-4 lg:grid-cols-[0.85fr_1.15fr]">
          <section className="animate-enter-up rounded-2xl border border-white/5 bg-[#17212b] p-5 shadow-panel-soft">
            <ProfilePulseAsset className="h-auto w-full" />
            <div className="mt-5 flex items-center gap-4">
              <Avatar user={currentUser} size="xl" />
              <div>
                <h1 className="bg-gradient-to-r from-white to-slate-300 bg-clip-text text-3xl font-semibold tracking-tight text-transparent">
                  {currentUser.displayName}
                </h1>
                <p className="mt-2 text-sm text-slate-400">@{currentUser.username}</p>
              </div>
            </div>
            <p className="mt-5 text-sm leading-6 text-slate-300">{currentUser.bio}</p>

            <div className="mt-7 grid grid-cols-3 gap-3">
              <ProfileMetric label="Chats" value={stats.activeConversations} tone="cyan" />
              <ProfileMetric label="Online" value={stats.onlineCount} tone="emerald" />
              <ProfileMetric label="Unread" value={stats.unreadTotal} tone="amber" />
            </div>

            <Link
              to="/login"
              onClick={() => signOut()}
              className="press lift mt-7 flex w-full items-center justify-center gap-2 rounded-xl border border-rose-400/25 bg-rose-400/10 px-4 py-3 font-medium text-rose-100 hover:bg-rose-400/20"
            >
              <LogOut size={18} />
              Sign out
            </Link>
          </section>

          <section className="animate-enter-up rounded-2xl border border-white/5 bg-[#17212b] p-5 shadow-panel-soft [animation-delay:80ms]">
            <div className="flex flex-col justify-between gap-4 border-b border-white/5 pb-6 sm:flex-row sm:items-center">
              <div>
                <p className="text-sm font-medium text-[#6ab7ee]">Account</p>
                <h2 className="mt-1 bg-gradient-to-r from-white to-slate-300 bg-clip-text text-2xl font-semibold text-transparent">
                  Profile settings
                </h2>
              </div>
              {isSaving ? (
                <StatusPill tone="cyan">Saving</StatusPill>
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
                <p className="mt-2 text-xs text-slate-500">
                  Email changes are not supported by the current backend API.
                </p>
              </label>
              <label className="block">
                <span className="text-sm font-medium text-slate-300">Status message</span>
                <textarea
                  value={form.bio}
                  onChange={(event) => updateField("bio", event.target.value)}
                  rows={5}
                  className="field-shell mt-2 w-full resize-none rounded-xl bg-[#242f3d] px-4 py-3 text-sm leading-6 text-white outline-none placeholder:text-slate-500"
                />
              </label>

              {error ? (
                <div className="flex animate-scale-in items-start gap-3 rounded-xl border border-rose-400/25 bg-rose-400/10 p-3 text-sm leading-5 text-rose-100">
                  <AlertCircle size={18} className="mt-0.5 shrink-0" />
                  <span>{error}</span>
                </div>
              ) : null}

              <div className="flex flex-col gap-3 pt-2 sm:flex-row">
                <button
                  type="submit"
                  disabled={isSaving}
                  className="send-button flex flex-1 items-center justify-center gap-2 rounded-xl bg-gradient-to-br from-[#3cb8f5] to-[#2aabee] px-5 py-3 font-semibold text-white shadow-send hover:shadow-send-hover disabled:cursor-not-allowed disabled:bg-[#242f3d] disabled:bg-none disabled:text-slate-500 disabled:shadow-none"
                >
                  <span className="relative z-[1] flex items-center gap-2">
                    <Save size={18} />
                    {isSaving ? "Saving..." : "Save changes"}
                  </span>
                </button>
                <Link
                  to="/chat"
                  className="press lift flex flex-1 items-center justify-center gap-2 rounded-xl bg-[#242f3d] px-5 py-3 font-semibold text-slate-200 hover:bg-[#2b3948]"
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
    <div className="lift rounded-xl bg-gradient-to-br from-[#242f3d] to-[#1c2733] p-3 ring-1 ring-white/5 hover:from-[#2a3645] hover:to-[#202b36]">
      <p className="text-2xl font-semibold text-white">{value}</p>
      <div className="mt-2">
        <StatusPill tone={tone}>{label}</StatusPill>
      </div>
    </div>
  );
}

function ProfileField({ icon: Icon, value, onChange, disabled = false }) {
  return (
    <div className="field-shell mt-2 flex items-center gap-3 rounded-xl bg-[#242f3d] px-4 py-3">
      <Icon size={18} className="text-slate-500" />
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        disabled={disabled}
        className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none disabled:cursor-not-allowed disabled:text-slate-500"
      />
    </div>
  );
}
