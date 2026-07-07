export default function BrandMark({ className = "" }) {
  return (
    <svg
      viewBox="0 0 64 64"
      fill="none"
      className={className}
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <linearGradient id="brand-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#818cf8" />
          <stop offset="100%" stopColor="#a78bfa" />
        </linearGradient>
      </defs>
      <path
        d="M8 25.5C8 14.73 16.73 6 27.5 6h9C47.27 6 56 14.73 56 25.5v5C56 41.27 47.27 50 36.5 50H33l-9.9 8.15c-1.7 1.4-4.1.19-4.1-2.01v-7.27C12.63 45.81 8 39.31 8 31.5v-6Z"
        fill="url(#brand-gradient)"
      />
      <path
        d="M18 24.5c0-4.7 3.8-8.5 8.5-8.5h14c4.7 0 8.5 3.8 8.5 8.5v2c0 4.7-3.8 8.5-8.5 8.5H32l-7.08 5.76c-1.15.94-2.92.12-2.92-1.37V34.3c-2.42-1.44-4-4.1-4-7.08v-2.72Z"
        fill="#0f172a"
      />
      <path d="M25 25h16" stroke="#fbbf24" strokeWidth="4" strokeLinecap="round" />
      <path d="M25 32h10" stroke="#34d399" strokeWidth="4" strokeLinecap="round" />
    </svg>
  );
}
