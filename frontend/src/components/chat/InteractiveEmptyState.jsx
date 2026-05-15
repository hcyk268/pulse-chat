import { MessageCircle, SendHorizontal, Sparkles } from "lucide-react";
import { useRef, useState } from "react";
import EmptyChatArtwork from "../assets/EmptyChatArtwork";

const chips = [
  {
    icon: MessageCircle,
    label: "Direct",
    className: "left-6 top-7 hidden md:flex",
    depth: 12,
    tone: "text-cyan-200",
  },
  {
    icon: Sparkles,
    label: "Live",
    className: "right-6 top-7 hidden md:flex",
    depth: -10,
    tone: "text-emerald-200",
  },
  {
    icon: SendHorizontal,
    label: "Send",
    className: "bottom-7 left-[calc(50%-42px)]",
    depth: 8,
    tone: "text-amber-200",
  },
];

export default function InteractiveEmptyState() {
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
      className="empty-physics-scene relative w-full max-w-[560px]"
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
            className={`empty-physics-chip absolute z-20 items-center gap-2 rounded-lg border border-white/10 bg-[#101827]/95 px-3 py-2 text-xs font-semibold text-slate-200 shadow-panel ${chip.className}`}
            style={{ "--depth": chip.depth }}
          >
            <Icon size={15} className={chip.tone} />
            <span>{chip.label}</span>
          </div>
        );
      })}

      <div className="empty-physics-panel relative rounded-lg border border-black/25 bg-[#17212b]/94 px-7 pb-9 pt-8 text-center shadow-panel sm:px-9">
        <div className="empty-physics-sheen pointer-events-none absolute inset-0 rounded-lg" />
        <div className="empty-physics-orbit pointer-events-none absolute inset-5 rounded-lg" />
        <div className="relative">
          <EmptyChatArtwork />
          <h2 className="mt-4 text-2xl font-semibold text-white">No conversation selected</h2>
          <p className="mt-2 text-sm leading-6 text-slate-400">
            Open a recent chat or start a new direct conversation from the people panel.
          </p>
        </div>
      </div>
    </div>
  );
}
