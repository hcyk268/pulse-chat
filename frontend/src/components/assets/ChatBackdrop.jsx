export default function ChatBackdrop() {
  return (
    <svg
      className="chat-backdrop pointer-events-none absolute inset-0 h-full w-full opacity-[0.25]"
      viewBox="0 0 1100 760"
      preserveAspectRatio="none"
      fill="none"
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <pattern id="chat-grid" width="96" height="96" patternUnits="userSpaceOnUse">
          <path d="M96 0H0V96" stroke="#334155" strokeWidth="0.5" opacity=".2" />
          <circle cx="0" cy="0" r="1.5" fill="#6366f1" opacity=".4" />
        </pattern>
      </defs>
      <rect className="chat-backdrop-grid" width="1100" height="760" fill="url(#chat-grid)" />
      <path
        className="chat-backdrop-wave chat-backdrop-wave-cyan"
        d="M46 610c148-84 243-66 376-2 117 56 240 51 385-41 84-54 159-67 247-38"
        stroke="#6366f1"
        strokeWidth="1.5"
        opacity=".35"
      />
      <path
        className="chat-backdrop-wave chat-backdrop-wave-amber"
        d="M84 154c126 46 230 42 348-16 124-62 214-47 335 33 76 51 151 69 256 33"
        stroke="#a855f7"
        strokeWidth="1.5"
        opacity=".25"
      />
      <rect className="chat-backdrop-card chat-backdrop-card-one" x="785" y="92" width="170" height="54" rx="12" stroke="#6366f1" opacity=".3" />
      <rect className="chat-backdrop-card chat-backdrop-card-two" x="170" y="482" width="188" height="62" rx="12" stroke="#a855f7" opacity=".25" />
      <path className="chat-backdrop-card-line chat-backdrop-card-one" d="M810 120h95M810 137h56" stroke="#6366f1" strokeWidth="4" strokeLinecap="round" opacity=".35" />
      <path className="chat-backdrop-card-line chat-backdrop-card-two" d="M198 510h110M198 529h70" stroke="#a855f7" strokeWidth="4" strokeLinecap="round" opacity=".3" />
    </svg>
  );
}
