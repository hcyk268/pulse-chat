import { ArrowLeft } from "lucide-react";
import { Link } from "react-router-dom";
import EmptyChatArtwork from "../components/assets/EmptyChatArtwork";

export default function NotFoundPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-[#0a0f1a] p-6 text-white">
      <section className="glass-card max-w-md animate-scale-in rounded-3xl p-8 text-center">
        <EmptyChatArtwork compact />
        <h1 className="mt-6 bg-gradient-to-r from-white via-indigo-100 to-white bg-clip-text text-3xl font-bold text-transparent">
          Route not found
        </h1>
        <p className="mt-2 text-sm leading-6 text-slate-400">
          This page is not part of the current frontend route map.
        </p>
        <Link
          to="/chat"
          className="send-button mt-7 inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-purple-600 px-5 py-3 font-semibold text-white shadow-send hover:shadow-send-hover"
        >
          <span className="relative z-[1] flex items-center gap-2">
            <ArrowLeft size={18} />
            Back to chat
          </span>
        </Link>
      </section>
    </main>
  );
}
