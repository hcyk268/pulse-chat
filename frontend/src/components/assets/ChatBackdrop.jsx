export default function ChatBackdrop() {
  return (
    <svg
      className="pointer-events-none absolute inset-0 h-full w-full opacity-[0.28]"
      viewBox="0 0 1100 760"
      preserveAspectRatio="none"
      fill="none"
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <pattern id="chat-grid" width="96" height="96" patternUnits="userSpaceOnUse">
          <path d="M96 0H0V96" stroke="#334155" strokeWidth="1" opacity=".24" />
          <circle cx="0" cy="0" r="2" fill="#67e8f9" opacity=".55" />
        </pattern>
      </defs>
      <rect width="1100" height="760" fill="url(#chat-grid)" />
      <path
        d="M46 610c148-84 243-66 376-2 117 56 240 51 385-41 84-54 159-67 247-38"
        stroke="#22d3ee"
        strokeWidth="2"
        opacity=".45"
      />
      <path
        d="M84 154c126 46 230 42 348-16 124-62 214-47 335 33 76 51 151 69 256 33"
        stroke="#f59e0b"
        strokeWidth="2"
        opacity=".34"
      />
      <rect x="785" y="92" width="170" height="54" rx="12" stroke="#34d399" opacity=".5" />
      <rect x="170" y="482" width="188" height="62" rx="12" stroke="#67e8f9" opacity=".35" />
      <path d="M810 120h95M810 137h56" stroke="#34d399" strokeWidth="5" strokeLinecap="round" opacity=".5" />
      <path d="M198 510h110M198 529h70" stroke="#67e8f9" strokeWidth="5" strokeLinecap="round" opacity=".4" />
    </svg>
  );
}
