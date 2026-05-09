import BrandMark from "./BrandMark";

export default function AuthChatArtwork({ variant = "cyan" }) {
  const isWarm = variant === "warm";
  const primary = isWarm ? "#fbbf24" : "#67e8f9";
  const secondary = isWarm ? "#fb7185" : "#34d399";
  const tertiary = isWarm ? "#22d3ee" : "#fbbf24";

  return (
    <div className="relative mx-auto w-full max-w-[560px]">
      <svg
        viewBox="0 0 720 520"
        fill="none"
        className="h-auto w-full drop-shadow-[0_38px_80px_rgba(0,0,0,0.45)]"
        aria-hidden="true"
        focusable="false"
      >
        <rect x="88" y="74" width="476" height="328" rx="18" fill="#0b1220" />
        <rect x="88" y="74" width="476" height="328" rx="18" stroke="#243244" strokeWidth="2" />
        <path d="M88 120h476" stroke="#243244" strokeWidth="2" />
        <circle cx="118" cy="97" r="7" fill="#fb7185" />
        <circle cx="142" cy="97" r="7" fill="#fbbf24" />
        <circle cx="166" cy="97" r="7" fill="#34d399" />

        <rect x="122" y="151" width="130" height="214" rx="12" fill="#111827" />
        <rect x="143" y="180" width="74" height="8" rx="4" fill="#334155" />
        <rect x="143" y="203" width="86" height="8" rx="4" fill="#1f2937" />
        <rect x="143" y="226" width="61" height="8" rx="4" fill="#1f2937" />
        <rect x="143" y="283" width="78" height="8" rx="4" fill="#334155" />
        <rect x="143" y="306" width="62" height="8" rx="4" fill="#1f2937" />
        <path d="M122 260h130" stroke="#1e293b" strokeWidth="2" />
        <circle cx="187" cy="342" r="8" fill={primary} />

        <rect x="288" y="151" width="238" height="52" rx="14" fill={primary} />
        <path d="M316 177h118" stroke="#0f172a" strokeWidth="8" strokeLinecap="round" />
        <path d="M456 177h36" stroke="#0f172a" strokeWidth="8" strokeLinecap="round" />

        <rect x="288" y="230" width="164" height="54" rx="14" fill="#162033" />
        <path d="M316 256h94" stroke="#64748b" strokeWidth="8" strokeLinecap="round" />
        <path d="M316 274h62" stroke="#334155" strokeWidth="8" strokeLinecap="round" />

        <rect x="340" y="311" width="186" height="54" rx="14" fill={secondary} />
        <path d="M370 337h102" stroke="#052e2b" strokeWidth="8" strokeLinecap="round" />
        <path d="M370 355h70" stroke="#052e2b" strokeWidth="8" strokeLinecap="round" opacity=".55" />

        <rect x="498" y="232" width="122" height="222" rx="22" fill="#050914" />
        <rect x="511" y="250" width="96" height="166" rx="14" fill="#101827" />
        <rect x="528" y="276" width="54" height="8" rx="4" fill={tertiary} />
        <rect x="528" y="298" width="39" height="8" rx="4" fill="#334155" />
        <rect x="528" y="346" width="62" height="28" rx="10" fill={primary} />
        <circle cx="559" cy="434" r="7" fill="#334155" />

        <path
          d="M90 430c65-18 105-7 156 14 66 27 107 22 168-5 74-32 128-22 219 21"
          stroke="#1e293b"
          strokeWidth="3"
          strokeLinecap="round"
        />
        <path
          d="M105 457c54-14 97-9 143 9 64 24 115 20 172-6 61-28 121-28 198 4"
          stroke={primary}
          strokeWidth="3"
          strokeLinecap="round"
          opacity=".45"
        />

        <g className="animate-float-soft">
          <rect x="52" y="174" width="92" height="52" rx="14" fill="#101827" stroke="#26364a" />
          <circle cx="76" cy="200" r="8" fill={secondary} />
          <path d="M94 194h29M94 207h20" stroke="#94a3b8" strokeWidth="6" strokeLinecap="round" />
        </g>

        <g className="animate-float-soft [animation-delay:1.2s]">
          <rect x="560" y="112" width="112" height="62" rx="16" fill="#101827" stroke="#26364a" />
          <path d="M586 138h56M586 154h35" stroke={tertiary} strokeWidth="7" strokeLinecap="round" />
        </g>
      </svg>

      <div className="absolute left-8 top-8 h-14 w-14 rounded-lg border border-white/10 bg-slate-950/80 p-2 shadow-panel backdrop-blur">
        <BrandMark className="h-full w-full" />
      </div>
    </div>
  );
}
