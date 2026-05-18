import { AlertCircle, ArrowRight, LockKeyhole, Mail, ShieldCheck, UserRound } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AuthChatArtwork from "../components/assets/AuthChatArtwork";
import BrandMark from "../components/assets/BrandMark";
import StatusPill from "../components/ui/StatusPill";
import { useChatStore } from "../hooks/useChatStore";
import { register } from "../services/authApi";

export default function RegisterPage() {
  const navigate = useNavigate();
  const { setAuthenticatedSession } = useChatStore();
  const [form, setForm] = useState({
    displayName: "",
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const [rememberSession, setRememberSession] = useState(true);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function updateField(field, value) {
    setError("");
    setForm((previous) => ({ ...previous, [field]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();

    const payload = {
      displayName: form.displayName.trim(),
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
    };

    if (
      !payload.displayName ||
      !payload.username ||
      !payload.email ||
      !payload.password ||
      !payload.confirmPassword
    ) {
      setError("Please fill in all account fields.");
      return;
    }

    if (payload.password !== payload.confirmPassword) {
      setError("Password confirmation does not match.");
      return;
    }

    setIsSubmitting(true);
    setError("");

    try {
      const authSession = await register(payload);
      setAuthenticatedSession(authSession, rememberSession);
      navigate("/chat", { replace: true });
    } catch (registerError) {
      setError(registerError.message || "Registration failed. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="grid min-h-screen bg-[#0e1621] text-white lg:grid-cols-[0.95fr_1.05fr]">
      <section className="order-2 flex items-center justify-center p-6 sm:p-10 lg:order-1">
        <form
          onSubmit={handleSubmit}
          className="w-full max-w-lg animate-enter-up rounded-2xl border border-white/5 bg-[#17212b] p-6 shadow-panel sm:p-8"
        >
          <div className="mb-8">
            <p className="text-sm font-medium text-[#6ab7ee]">New workspace account</p>
            <h1 className="mt-2 bg-gradient-to-r from-white to-slate-300 bg-clip-text text-3xl font-semibold tracking-tight text-transparent">
              Create account
            </h1>
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <label className="block sm:col-span-2">
              <span className="text-sm font-medium text-slate-300">Display name</span>
              <Field
                icon={UserRound}
                value={form.displayName}
                onChange={(value) => updateField("displayName", value)}
                autoComplete="name"
                placeholder="Your public name"
              />
            </label>
            <label className="block">
              <span className="text-sm font-medium text-slate-300">Username</span>
              <Field
                icon={UserRound}
                value={form.username}
                onChange={(value) => updateField("username", value)}
                autoComplete="username"
                placeholder="maya"
              />
            </label>
            <label className="block">
              <span className="text-sm font-medium text-slate-300">Email</span>
              <Field
                icon={Mail}
                value={form.email}
                onChange={(value) => updateField("email", value)}
                autoComplete="email"
                placeholder="name@company.com"
              />
            </label>
            <label className="block">
              <span className="text-sm font-medium text-slate-300">Password</span>
              <Field
                icon={LockKeyhole}
                type="password"
                value={form.password}
                onChange={(value) => updateField("password", value)}
                autoComplete="new-password"
              />
            </label>
            <label className="block">
              <span className="text-sm font-medium text-slate-300">Confirm</span>
              <Field
                icon={LockKeyhole}
                type="password"
                value={form.confirmPassword}
                onChange={(value) => updateField("confirmPassword", value)}
                autoComplete="new-password"
              />
            </label>
          </div>

          <label className="mt-5 flex cursor-pointer items-center gap-2 text-sm text-slate-400 transition-colors hover:text-slate-200">
            <input
              type="checkbox"
              checked={rememberSession}
              onChange={(event) => setRememberSession(event.target.checked)}
              className="h-4 w-4 rounded border-white/20 bg-slate-900 accent-cyan-300"
            />
            Remember session
          </label>

          {error ? (
            <div className="mt-5 flex animate-scale-in items-start gap-3 rounded-xl border border-rose-400/25 bg-rose-400/10 p-3 text-sm leading-5 text-rose-100">
              <AlertCircle size={18} className="mt-0.5 shrink-0" />
              <span>{error}</span>
            </div>
          ) : null}

          <button
            type="submit"
            disabled={isSubmitting}
            className="send-button mt-8 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-br from-[#3cb8f5] to-[#2aabee] px-5 py-3.5 font-semibold text-white shadow-send hover:shadow-send-hover disabled:cursor-not-allowed disabled:bg-[#242f3d] disabled:bg-none disabled:text-slate-500 disabled:shadow-none"
          >
            <span className="relative z-[1] flex items-center gap-2">
              {isSubmitting ? "Creating account..." : "Start chatting"}
              <ArrowRight size={19} />
            </span>
          </button>

          <div className="mt-6 flex items-center gap-3 rounded-xl bg-[#242f3d] p-3 text-sm text-slate-300">
            <ShieldCheck size={18} className="shrink-0 text-emerald-300" />
            Your session starts after account creation.
          </div>

          <p className="mt-5 text-center text-sm text-slate-400">
            Already have an account?{" "}
            <Link
              to="/login"
              className="font-medium text-[#6ab7ee] transition-colors hover:text-[#9ed3f7]"
            >
              Sign in
            </Link>
          </p>
        </form>
      </section>

      <section className="relative order-1 flex min-h-[42vh] flex-col justify-between overflow-hidden border-b border-black/30 bg-[#17212b] p-6 sm:p-10 lg:order-2 lg:min-h-screen lg:border-b-0 lg:border-l">
        <div
          aria-hidden
          className="pointer-events-none absolute -right-32 -top-32 h-96 w-96 rounded-full bg-amber-400/10 blur-3xl"
        />
        <div
          aria-hidden
          className="pointer-events-none absolute -bottom-32 -left-32 h-96 w-96 rounded-full bg-cyan-500/10 blur-3xl"
        />

        <div className="relative max-w-xl animate-enter-up">
          <div className="mb-7 inline-flex items-center gap-3 rounded-xl bg-[#242f3d] px-4 py-3 text-cyan-100 shadow-panel-soft transition-transform duration-300 hover:-translate-y-0.5">
            <BrandMark className="h-7 w-7" />
            <span className="font-semibold">Pulse Chat</span>
          </div>
          <h2 className="max-w-lg bg-gradient-to-br from-white via-cyan-100 to-white bg-clip-text text-4xl font-semibold tracking-tight text-transparent sm:text-5xl">
            Start with a product-like chat shell.
          </h2>
          <div className="mt-8 flex flex-wrap gap-3">
            <StatusPill tone="cyan">Direct chat</StatusPill>
            <StatusPill tone="emerald">Presence</StatusPill>
            <StatusPill tone="amber">Read state</StatusPill>
          </div>
        </div>
        <div className="relative mt-8 animate-enter-up [animation-delay:120ms]">
          <AuthChatArtwork variant="warm" />
        </div>
      </section>
    </main>
  );
}

function Field({
  icon: Icon,
  type = "text",
  value,
  onChange,
  placeholder = "",
  autoComplete = undefined,
}) {
  return (
    <div className="field-shell mt-2 flex items-center gap-3 rounded-xl bg-[#242f3d] px-4 py-3">
      <Icon size={18} className="text-slate-500" />
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        autoComplete={autoComplete}
        className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-slate-500"
      />
    </div>
  );
}
