import { AlertCircle, ArrowRight, LockKeyhole, Mail, ShieldCheck, Sparkles } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import AuthChatArtwork from "../components/assets/AuthChatArtwork";
import BrandMark from "../components/assets/BrandMark";
import StatusPill from "../components/ui/StatusPill";
import { useChatStore } from "../hooks/useChatStore";
import { login } from "../services/authApi";
import { useToast } from "../hooks/useToast";

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { setAuthenticatedSession } = useChatStore();
  const toast = useToast();
  const [form, setForm] = useState({
    usernameOrEmail: "",
    password: "",
  });
  const [rememberSession, setRememberSession] = useState(true);
  const [error, setError] = useState(location.state?.authMessage || "");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (location.state?.authMessage) {
      setError(location.state.authMessage);
    }
  }, [location.state]);

  function updateField(field, value) {
    setError("");
    setForm((previous) => ({ ...previous, [field]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();

    const usernameOrEmail = form.usernameOrEmail.trim();
    const password = form.password;

    if (!usernameOrEmail || !password) {
      const message = "Please enter your username/email and password.";
      setError(message);
      toast.warning(message);
      return;
    }

    setIsSubmitting(true);
    setError("");

    try {
      const authSession = await login({ usernameOrEmail, password });
      const redirectTo = location.state?.from?.pathname || "/chat";
      setAuthenticatedSession(authSession, rememberSession);
      navigate(redirectTo, { replace: true });
    } catch (loginError) {
      const message = loginError.message || "Login failed. Please try again.";
      setError(message);
      toast.error(message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="auth-page grid min-h-screen bg-[#0a0f1a] text-white lg:grid-cols-[1.08fr_0.92fr]">
      <section className="auth-visual relative flex min-h-[46vh] flex-col justify-between overflow-hidden border-b border-white/5 bg-[#111827] p-6 sm:p-10 lg:min-h-screen lg:border-b-0 lg:border-r">
        <div
          aria-hidden
          className="pointer-events-none absolute -left-40 -top-40 h-[500px] w-[500px] rounded-full bg-indigo-500/15 blur-[100px]"
        />
        <div
          aria-hidden
          className="pointer-events-none absolute -bottom-40 -right-40 h-[500px] w-[500px] rounded-full bg-purple-500/10 blur-[100px]"
        />
        <div
          aria-hidden
          className="pointer-events-none absolute left-1/2 top-1/2 h-[300px] w-[300px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-sky-500/5 blur-[80px]"
        />

        <div className="relative z-10 max-w-xl animate-enter-up">
          <div className="mb-7 inline-flex items-center gap-3 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-indigo-100 shadow-card backdrop-blur-sm transition-all duration-300 hover:-translate-y-0.5 hover:border-indigo-400/30 hover:shadow-glow">
            <BrandMark className="h-7 w-7" />
            <span className="font-semibold tracking-wide">Pulse Chat</span>
          </div>
          <h1 className="max-w-lg bg-gradient-to-br from-white via-indigo-100 to-purple-200 bg-clip-text text-4xl font-bold tracking-tight text-transparent sm:text-5xl">
            Realtime chat that feels alive.
          </h1>
          <p className="mt-5 max-w-lg text-base leading-7 text-slate-400">
            Sign in to your live workspace, pick up conversations, and keep presence in sync.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <StatusPill tone="indigo">Secure session</StatusPill>
            <StatusPill tone="emerald">Realtime sync</StatusPill>
            <StatusPill tone="amber">Direct chat</StatusPill>
          </div>
        </div>
        <div className="relative z-10 mt-8 animate-enter-up [animation-delay:120ms]">
          <AuthChatArtwork />
        </div>
      </section>

      <section className="auth-form-shell flex items-center justify-center p-6 sm:p-10">
        <form
          onSubmit={handleSubmit}
          className="glass-card w-full max-w-md animate-enter-up rounded-3xl p-6 sm:p-8"
        >
          <div className="mb-8">
            <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-indigo-400/20 bg-indigo-400/10 px-3 py-1.5 text-xs font-medium text-indigo-300">
              <Sparkles size={12} />
              Welcome back
            </div>
            <h2 className="bg-gradient-to-r from-white via-slate-100 to-slate-300 bg-clip-text text-3xl font-bold tracking-tight text-transparent">
              Sign in
            </h2>
          </div>

          <label className="block">
            <span className="text-sm font-medium text-slate-300">Username or email</span>
            <div className="field-shell mt-2 flex items-center gap-3 rounded-xl border border-white/5 bg-[#1e293b] px-4 py-3.5">
              <Mail size={18} className="text-slate-500" />
              <input
                type="text"
                value={form.usernameOrEmail}
                onChange={(event) => updateField("usernameOrEmail", event.target.value)}
                autoComplete="username"
                className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-slate-500"
                placeholder="maya@pulse.local"
              />
            </div>
          </label>

          <label className="mt-5 block">
            <span className="text-sm font-medium text-slate-300">Password</span>
            <div className="field-shell mt-2 flex items-center gap-3 rounded-xl border border-white/5 bg-[#1e293b] px-4 py-3.5">
              <LockKeyhole size={18} className="text-slate-500" />
              <input
                type="password"
                value={form.password}
                onChange={(event) => updateField("password", event.target.value)}
                autoComplete="current-password"
                className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-slate-500"
                placeholder="At least 8 characters"
              />
            </div>
          </label>

          <div className="mt-5 flex items-center justify-between text-sm">
            <label className="flex cursor-pointer items-center gap-2 text-slate-400 transition-colors hover:text-slate-200">
              <input
                type="checkbox"
                checked={rememberSession}
                onChange={(event) => setRememberSession(event.target.checked)}
                className="h-4 w-4 rounded border-white/20 bg-slate-800 accent-indigo-400"
              />
              Remember session
            </label>
            <Link
              to="/register"
              className="font-medium text-indigo-400 transition-colors hover:text-indigo-300"
            >
              Create account
            </Link>
          </div>

          {error ? (
            <div className="mt-5 flex animate-scale-in items-start gap-3 rounded-xl border border-rose-400/20 bg-rose-400/10 p-3 text-sm leading-5 text-rose-200">
              <AlertCircle size={18} className="mt-0.5 shrink-0" />
              <span>{error}</span>
            </div>
          ) : null}

          <button
            type="submit"
            disabled={isSubmitting}
            className="send-button mt-8 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 via-indigo-500 to-purple-600 px-5 py-3.5 font-semibold text-white shadow-send hover:shadow-send-hover disabled:cursor-not-allowed disabled:bg-slate-800 disabled:bg-none disabled:text-slate-500 disabled:shadow-none"
          >
            <span className="relative z-[1] flex items-center gap-2">
              {isSubmitting ? "Signing in..." : "Enter app"}
              <ArrowRight
                size={19}
                className="transition-transform duration-300 ease-out-soft"
              />
            </span>
          </button>

          <div className="mt-6 flex items-center gap-3 rounded-xl border border-emerald-400/15 bg-emerald-400/5 p-3 text-sm text-slate-300">
            <ShieldCheck size={18} className="shrink-0 text-emerald-400" />
            Your session is protected and ready for realtime chat.
          </div>
        </form>
      </section>
    </main>
  );
}
