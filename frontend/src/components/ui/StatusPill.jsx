export default function StatusPill({ tone = "neutral", children }) {
  const tones = {
    cyan: "border-cyan-400/30 bg-cyan-400/10 text-cyan-200 shadow-[0_0_18px_rgba(34,211,238,0.12)]",
    emerald:
      "border-emerald-400/30 bg-emerald-400/10 text-emerald-200 shadow-[0_0_18px_rgba(52,211,153,0.12)]",
    amber:
      "border-amber-400/30 bg-amber-400/10 text-amber-200 shadow-[0_0_18px_rgba(251,191,36,0.12)]",
    rose: "border-rose-400/30 bg-rose-400/10 text-rose-200 shadow-[0_0_18px_rgba(251,113,133,0.12)]",
    neutral: "border-slate-500/25 bg-slate-700/30 text-slate-300",
  };

  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-medium backdrop-blur transition-all duration-200 hover:-translate-y-px ${tones[tone]}`}
    >
      {children}
    </span>
  );
}
