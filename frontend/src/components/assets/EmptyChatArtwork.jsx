import BrandMark from "./BrandMark";

export default function EmptyChatArtwork({ compact = false }) {
  return (
    <svg
      viewBox="0 0 420 300"
      fill="none"
      className={compact ? "mx-auto h-36 w-full" : "mx-auto h-52 w-full max-w-sm"}
      aria-hidden="true"
      focusable="false"
    >
      <rect x="68" y="54" width="284" height="166" rx="18" fill="#050914" stroke="#1e293b" />
      <path d="M68 96h284" stroke="#1e293b" />
      <rect x="93" y="120" width="122" height="34" rx="11" fill="#1e293b" />
      <path d="M112 138h76" stroke="#64748b" strokeWidth="6" strokeLinecap="round" />
      <rect x="205" y="170" width="120" height="34" rx="11" fill="#6366f1" />
      <path d="M225 188h65" stroke="#0f172a" strokeWidth="6" strokeLinecap="round" />
      <rect x="96" y="170" width="84" height="34" rx="11" fill="#1e293b" />
      <path d="M115 188h39" stroke="#fbbf24" strokeWidth="6" strokeLinecap="round" />
      <circle cx="96" cy="75" r="6" fill="#fb7185" />
      <circle cx="116" cy="75" r="6" fill="#fbbf24" />
      <circle cx="136" cy="75" r="6" fill="#34d399" />
      <g className="animate-float-soft">
        <rect x="256" y="24" width="78" height="44" rx="12" fill="#0f1629" stroke="#1e293b" />
        <path d="M275 46h35M275 58h20" stroke="#34d399" strokeWidth="5" strokeLinecap="round" />
      </g>
      <g className="animate-float-soft [animation-delay:1s]">
        <rect x="42" y="205" width="90" height="48" rx="12" fill="#0f1629" stroke="#1e293b" />
        <path d="M62 227h46M62 240h27" stroke="#818cf8" strokeWidth="5" strokeLinecap="round" />
      </g>
      <foreignObject x="179" y="220" width="62" height="62">
        <div className="h-full w-full rounded-xl border border-white/10 bg-slate-950/90 p-2">
          <BrandMark className="h-full w-full" />
        </div>
      </foreignObject>
    </svg>
  );
}
