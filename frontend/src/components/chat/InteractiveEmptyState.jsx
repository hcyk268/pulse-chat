import { ArrowRight, MessageCircle, SendHorizontal, Sparkles } from "lucide-react";
import { useRef, useState } from "react";
import EmptyChatArtwork from "../assets/EmptyChatArtwork";

const chips = [
  {
    icon: MessageCircle,
    label: "Direct",
    className: "left-6 top-7 hidden md:flex",
    depth: 12,
    tone: "text-indigo-300",
  },
  {
    icon: Sparkles,
    label: "Live",
    className: "right-6 top-7 hidden md:flex",
    depth: -10,
    tone: "text-emerald-300",
  },
  {
    icon: SendHorizontal,
    label: "Send",
    className: "bottom-7 left-[calc(50%-42px)]",
    depth: 8,
    tone: "text-amber-300",
  },
];

export default function InteractiveEmptyState({ onOpenPeople }) {
  const panelRef = useRef(null);
  const [pointer, setPointer] = useState({ x: 0, y: 0, active: false, pressed: false });

  function updatePointer(event) {
    const rect = panelRef.current?.getBoundingClientRect();
    if (!rect) return;

    const x = ((event.clientX - rect.left) / rect.width - 0.5) * 2;
    const y = ((event.clientY - rect.top) / rect.height - 0.5) * 2;

    setPointer((previous) => ({
      x: Math.max(-1, Math.min(1, x)),
      y: Math.max(-1, Math.min(1, y)),
      active: true,
      pressed: previous.pressed,
    }));
  }

  function resetPointer() {
    setPointer({ x: 0, y: 0, active: false, pressed: false });
  }

  return (
    <div
      ref={panelRef}
      className="empty-physics-scene relative w-full max-w-[780px] animate-scale-in"
      onPointerMove={updatePointer}
      onPointerLeave={resetPointer}
      onPointerDown={() => setPointer((previous) => ({ ...previous, pressed: true }))}
      onPointerUp={() => setPointer((previous) => ({ ...previous, pressed: false }))}
      style={{
        "--mx": pointer.active ? pointer.x.toFixed(3) : "0",
        "--my": pointer.active ? pointer.y.toFixed(3) : "0",
        "--press": pointer.pressed ? "0.985" : "1",
      }}
    >
      {chips.map((chip) => {
        const Icon = chip.icon;

        return (
          <div
            key={chip.label}
            className={`empty-physics-chip absolute z-20 items-center gap-2 rounded-xl border border-white/10 bg-[#0f1629]/95 px-3 py-2 text-xs font-semibold text-slate-200 shadow-panel-soft backdrop-blur ${chip.className}`}
            style={{ "--depth": chip.depth }}
          >
            <Icon size={15} className={chip.tone} />
            <span>{chip.label}</span>
          </div>
        );
      })}

      <div className="empty-physics-panel relative rounded-[2rem] border border-white/[0.07] bg-[#111827]/94 p-6 shadow-panel sm:p-8 lg:p-10">
        <div className="empty-physics-sheen pointer-events-none absolute inset-0 rounded-3xl" />
        <div className="empty-physics-orbit pointer-events-none absolute inset-5 rounded-2xl" />
        <div className="empty-physics-content relative grid items-center gap-5 text-center md:grid-cols-[1.05fr_0.95fr] md:gap-8 md:text-left">
          <div className="min-w-0">
            <p className="mb-3 text-xs font-bold uppercase tracking-[0.2em] text-indigo-300">
              Your space to connect
            </p>
            <h2 className="empty-physics-title bg-gradient-to-r from-white via-indigo-100 to-slate-300 bg-clip-text font-bold tracking-[-0.035em] text-transparent">
              Pick up where you left off.
            </h2>
            <p className="mx-auto mt-4 max-w-md text-sm leading-6 text-slate-400 md:mx-0 sm:text-[15px]">
              Select a conversation from the sidebar or find someone new to start messaging in real time.
            </p>
            <button
              type="button"
              onClick={onOpenPeople}
              className="send-button mx-auto mt-6 flex min-h-12 items-center justify-center gap-2 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 px-5 text-sm font-semibold text-white shadow-send hover:shadow-send-hover md:mx-0"
            >
              New conversation
              <ArrowRight size={17} />
            </button>
          </div>
          <div className="empty-physics-artwork min-w-0">
            <EmptyChatArtwork />
          </div>
        </div>
      </div>
    </div>
  );
}
