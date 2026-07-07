export function EmptyChatsAsset({ className = "" }) {
  return (
    <svg
      viewBox="0 0 160 120"
      fill="none"
      className={className}
      aria-hidden="true"
      focusable="false"
    >
      <rect x="18" y="20" width="88" height="48" rx="16" fill="#1e293b" stroke="#334155" />
      <rect x="36" y="37" width="46" height="6" rx="3" fill="#818cf8" opacity=".85" />
      <rect x="36" y="50" width="30" height="6" rx="3" fill="#475569" />
      <path d="M42 68l-13 15 3-18" fill="#1e293b" stroke="#334155" strokeLinejoin="round" />
      <rect x="62" y="52" width="80" height="46" rx="15" fill="#4338ca" />
      <rect x="82" y="68" width="42" height="6" rx="3" fill="#e0e7ff" opacity=".9" />
      <rect x="82" y="81" width="25" height="6" rx="3" fill="#a5b4fc" opacity=".7" />
      <path d="M119 96l14 14-4-17" fill="#4338ca" />
      <circle cx="111" cy="28" r="6" fill="#34d399" />
      <path d="M121 22l12-9M127 34l16 4" stroke="#6366f1" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}

export function PeopleSearchAsset({ className = "" }) {
  return (
    <svg
      viewBox="0 0 160 120"
      fill="none"
      className={className}
      aria-hidden="true"
      focusable="false"
    >
      <rect x="30" y="24" width="70" height="72" rx="18" fill="#0f1629" stroke="#1e293b" />
      <circle cx="65" cy="50" r="15" fill="#818cf8" />
      <path d="M42 82c5-20 41-20 47 0" stroke="#34d399" strokeWidth="8" strokeLinecap="round" />
      <rect x="92" y="34" width="38" height="38" rx="19" fill="#1e293b" stroke="#334155" />
      <path d="M120 63l18 18" stroke="#6366f1" strokeWidth="8" strokeLinecap="round" />
      <circle cx="111" cy="53" r="11" stroke="#e0e7ff" strokeWidth="5" />
      <path d="M22 40h15M22 55h9M122 92h16" stroke="#475569" strokeWidth="5" strokeLinecap="round" />
    </svg>
  );
}

export function ProfilePulseAsset({ className = "" }) {
  return (
    <svg
      viewBox="0 0 460 210"
      fill="none"
      className={className}
      aria-hidden="true"
      focusable="false"
    >
      <rect x="28" y="34" width="404" height="142" rx="18" fill="#050914" stroke="#1e293b" />
      <path d="M68 74h118M68 102h86M68 130h146" stroke="#334155" strokeWidth="9" strokeLinecap="round" />
      <path d="M276 126l24-46 24 68 24-42 18 20h38" stroke="#818cf8" strokeWidth="8" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx="300" cy="80" r="7" fill="#34d399" />
      <circle cx="348" cy="106" r="7" fill="#fbbf24" />
      <path d="M34 190c68-30 126-22 184 0 74 28 132 23 210-18" stroke="#a855f7" strokeWidth="3" strokeLinecap="round" opacity=".5" />
      <rect x="338" y="18" width="74" height="36" rx="10" fill="#0f1629" stroke="#1e293b" />
      <path d="M354 36h33" stroke="#fbbf24" strokeWidth="5" strokeLinecap="round" />
    </svg>
  );
}
