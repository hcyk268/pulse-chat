export default function StatusPill({ tone = "neutral", children }) {
  const tones = {
    indigo:
      "border-indigo-400/25 bg-indigo-400/10 text-indigo-300 shadow-[0_0_16px_rgba(99,102,241,0.1)]",
    cyan: "border-cyan-400/25 bg-cyan-400/10 text-cyan-300 shadow-[0_0_16px_rgba(34,211,238,0.1)]",
    emerald:
      "border-emerald-400/25 bg-emerald-400/10 text-emerald-300 shadow-[0_0_16px_rgba(52,211,153,0.1)]",
    amber:
      "border-amber-400/25 bg-amber-400/10 text-amber-300 shadow-[0_0_16px_rgba(251,191,36,0.1)]",
    rose: "border-rose-400/25 bg-rose-400/10 text-rose-300 shadow-[0_0_16px_rgba(251,113,133,0.1)]",
    purple:
      "border-purple-400/25 bg-purple-400/10 text-purple-300 shadow-[0_0_16px_rgba(168,85,247,0.1)]",
    neutral: "border-slate-500/20 bg-slate-700/20 text-slate-400",
  };

  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-medium backdrop-blur transition-all duration-200 hover:-translate-y-px ${tones[tone] ?? tones.neutral}`}
    >
      {children}
    </span>
  );
}
