import { ArrowRight, LockKeyhole, Mail, UserRound } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import AuthChatArtwork from "../components/assets/AuthChatArtwork";
import BrandMark from "../components/assets/BrandMark";
import StatusPill from "../components/ui/StatusPill";

export default function RegisterPage() {
  const navigate = useNavigate();

  function handleSubmit(event) {
    event.preventDefault();
    navigate("/chat");
  }

  return (
    <main className="grid min-h-screen bg-[#0e1621] text-white lg:grid-cols-[0.95fr_1.05fr]">
      <section className="order-2 flex items-center justify-center p-6 sm:p-10 lg:order-1">
        <form
          onSubmit={handleSubmit}
          className="w-full max-w-lg animate-enter-up rounded-lg border border-black/25 bg-[#17212b] p-6 shadow-panel sm:p-8"
        >
          <div className="mb-8">
            <p className="text-sm font-medium text-[#6ab7ee]">New workspace account</p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight text-white">Create account</h1>
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <label className="block sm:col-span-2">
              <span className="text-sm font-medium text-slate-300">Display name</span>
              <Field icon={UserRound} defaultValue="Maya Nguyen" placeholder="Your public name" />
            </label>
            <label className="block">
              <span className="text-sm font-medium text-slate-300">Username</span>
              <Field icon={UserRound} defaultValue="maya" placeholder="maya" />
            </label>
            <label className="block">
              <span className="text-sm font-medium text-slate-300">Email</span>
              <Field icon={Mail} defaultValue="maya@pulse.local" placeholder="name@company.com" />
            </label>
            <label className="block">
              <span className="text-sm font-medium text-slate-300">Password</span>
              <Field icon={LockKeyhole} type="password" defaultValue="secret123" />
            </label>
            <label className="block">
              <span className="text-sm font-medium text-slate-300">Confirm</span>
              <Field icon={LockKeyhole} type="password" defaultValue="secret123" />
            </label>
          </div>

          <button
            type="submit"
            className="mt-8 flex w-full items-center justify-center gap-2 rounded-lg bg-[#2aabee] px-5 py-3.5 font-semibold text-white shadow-[0_8px_22px_rgba(42,171,238,0.22)] transition hover:bg-[#37b7f4]"
          >
            Start chatting
            <ArrowRight size={19} />
          </button>

          <p className="mt-5 text-center text-sm text-slate-400">
            Already have an account?{" "}
            <Link to="/login" className="font-medium text-[#6ab7ee] hover:text-[#8fcaf5]">
              Sign in
            </Link>
          </p>
        </form>
      </section>

      <section className="order-1 flex min-h-[42vh] flex-col justify-between border-b border-black/25 bg-[#17212b] p-6 sm:p-10 lg:order-2 lg:min-h-screen lg:border-b-0 lg:border-l">
        <div className="max-w-xl animate-enter-up">
          <div className="mb-7 inline-flex items-center gap-3 rounded-lg bg-[#242f3d] px-4 py-3 text-cyan-100 shadow-panel">
            <BrandMark className="h-7 w-7" />
            <span className="font-semibold">Pulse Chat</span>
          </div>
          <h2 className="max-w-lg text-4xl font-semibold tracking-tight text-white sm:text-5xl">
            Start with a product-like chat shell.
          </h2>
          <div className="mt-8 flex flex-wrap gap-3">
            <StatusPill tone="cyan">Direct chat</StatusPill>
            <StatusPill tone="emerald">Presence</StatusPill>
            <StatusPill tone="amber">Read state</StatusPill>
          </div>
        </div>
        <div className="mt-8 animate-enter-up">
          <AuthChatArtwork variant="warm" />
        </div>
      </section>
    </main>
  );
}

function Field({ icon: Icon, type = "text", defaultValue = "", placeholder = "" }) {
  return (
    <div className="mt-2 flex items-center gap-3 rounded-lg bg-[#242f3d] px-4 py-3 transition focus-within:bg-[#2b3948]">
      <Icon size={18} className="text-slate-500" />
      <input
        type={type}
        defaultValue={defaultValue}
        placeholder={placeholder}
        className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-slate-500"
      />
    </div>
  );
}
