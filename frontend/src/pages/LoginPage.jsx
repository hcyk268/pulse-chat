import { AlertCircle, ArrowRight, LockKeyhole, Mail, ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import AuthChatArtwork from "../components/assets/AuthChatArtwork";
import BrandMark from "../components/assets/BrandMark";
import StatusPill from "../components/ui/StatusPill";
import { useChatStore } from "../hooks/useChatStore";
import { login } from "../services/authApi";

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { setAuthenticatedSession } = useChatStore();
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
      setError("Please enter your username/email and password.");
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
      setError(loginError.message || "Login failed. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="grid min-h-screen bg-[#0e1621] text-white lg:grid-cols-[1.08fr_0.92fr]">
      <section className="relative flex min-h-[46vh] flex-col justify-between overflow-hidden border-b border-black/25 bg-[#17212b] p-6 sm:p-10 lg:min-h-screen lg:border-b-0 lg:border-r">
        <div className="relative z-10 max-w-xl animate-enter-up">
          <div className="mb-7 inline-flex items-center gap-3 rounded-lg bg-[#242f3d] px-4 py-3 text-cyan-100 shadow-panel">
            <BrandMark className="h-7 w-7" />
            <span className="font-semibold">Pulse Chat</span>
          </div>
          <h1 className="max-w-lg text-4xl font-semibold tracking-tight text-white sm:text-5xl">
            Realtime chat that feels alive.
          </h1>
          <p className="mt-5 max-w-lg text-base leading-7 text-slate-300">
            A sharper mock frontend with drawn product assets, live-feeling message states, and clean routing.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <StatusPill tone="cyan">JWT ready</StatusPill>
            <StatusPill tone="emerald">Mock realtime</StatusPill>
            <StatusPill tone="amber">Route based</StatusPill>
          </div>
        </div>
        <div className="relative z-10 mt-8 animate-enter-up">
          <AuthChatArtwork />
        </div>
      </section>

      <section className="flex items-center justify-center p-6 sm:p-10">
        <form
          onSubmit={handleSubmit}
          className="w-full max-w-md animate-enter-up rounded-lg border border-black/25 bg-[#17212b] p-6 shadow-panel sm:p-8"
        >
          <div className="mb-8">
            <p className="text-sm font-medium text-[#6ab7ee]">Welcome back</p>
            <h2 className="mt-2 text-3xl font-semibold tracking-tight text-white">Sign in</h2>
          </div>

          <label className="block">
            <span className="text-sm font-medium text-slate-300">Username or email</span>
            <div className="mt-2 flex items-center gap-3 rounded-lg bg-[#242f3d] px-4 py-3 transition focus-within:bg-[#2b3948]">
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
            <div className="mt-2 flex items-center gap-3 rounded-lg bg-[#242f3d] px-4 py-3 transition focus-within:bg-[#2b3948]">
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
            <label className="flex items-center gap-2 text-slate-400">
              <input
                type="checkbox"
                checked={rememberSession}
                onChange={(event) => setRememberSession(event.target.checked)}
                className="h-4 w-4 rounded border-white/20 bg-slate-900 accent-cyan-300"
              />
              Remember session
            </label>
            <Link to="/register" className="font-medium text-[#6ab7ee] hover:text-[#8fcaf5]">
              Create account
            </Link>
          </div>

          {error ? (
            <div className="mt-5 flex items-start gap-3 rounded-lg border border-rose-400/20 bg-rose-400/10 p-3 text-sm leading-5 text-rose-100">
              <AlertCircle size={18} className="mt-0.5 shrink-0" />
              <span>{error}</span>
            </div>
          ) : null}

          <button
            type="submit"
            disabled={isSubmitting}
            className="mt-8 flex w-full items-center justify-center gap-2 rounded-lg bg-[#2aabee] px-5 py-3.5 font-semibold text-white shadow-[0_8px_22px_rgba(42,171,238,0.22)] transition hover:bg-[#37b7f4] disabled:cursor-not-allowed disabled:bg-[#242f3d] disabled:text-slate-500 disabled:shadow-none"
          >
            {isSubmitting ? "Signing in..." : "Enter app"}
            <ArrowRight size={19} />
          </button>

          <div className="mt-6 flex items-center gap-3 rounded-lg bg-[#242f3d] p-3 text-sm text-slate-300">
            <ShieldCheck size={18} className="shrink-0" />
            Login now uses the backend auth API.
          </div>
        </form>
      </section>
    </main>
  );
}
