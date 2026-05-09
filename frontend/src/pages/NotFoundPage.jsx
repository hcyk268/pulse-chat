import { ArrowLeft } from "lucide-react";
import { Link } from "react-router-dom";
import EmptyChatArtwork from "../components/assets/EmptyChatArtwork";

export default function NotFoundPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-[#0e1621] p-6 text-white">
      <section className="max-w-md rounded-lg border border-black/25 bg-[#17212b] p-8 text-center shadow-panel">
        <EmptyChatArtwork compact />
        <h1 className="mt-6 text-3xl font-semibold">Route not found</h1>
        <p className="mt-2 text-sm leading-6 text-slate-400">
          This page is not part of the current frontend route map.
        </p>
        <Link
          to="/chat"
          className="mt-7 inline-flex items-center gap-2 rounded-lg bg-[#2aabee] px-5 py-3 font-semibold text-white shadow-[0_8px_22px_rgba(42,171,238,0.22)] transition hover:bg-[#37b7f4]"
        >
          <ArrowLeft size={18} />
          Back to chat
        </Link>
      </section>
    </main>
  );
}
