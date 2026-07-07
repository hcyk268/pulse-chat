/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        surface: {
          900: "#050810",
          800: "#0a0f1a",
          700: "#0f1629",
          600: "#111827",
          500: "#1e293b",
          400: "#1f2937",
          300: "#334155",
          200: "#475569",
        },
        accent: {
          DEFAULT: "#6366f1",
          hover: "#818cf8",
          soft: "#a5b4fc",
          ring: "rgba(99, 102, 241, 0.35)",
        },
        brand: {
          purple: "#a855f7",
          sky: "#0ea5e9",
          emerald: "#10b981",
          amber: "#f59e0b",
          rose: "#f43f5e",
        },
      },
      boxShadow: {
        glow: "0 0 35px rgba(99, 102, 241, 0.12)",
        "glow-lg": "0 0 60px rgba(99, 102, 241, 0.2)",
        "glow-purple": "0 0 40px rgba(168, 85, 247, 0.15)",
        panel: "0 24px 80px rgba(0, 0, 0, 0.5), 0 0 1px rgba(148, 163, 184, 0.1)",
        "panel-soft": "0 12px 40px rgba(0, 0, 0, 0.35)",
        "bubble-own": "0 8px 24px rgba(67, 56, 202, 0.25)",
        "bubble-other": "0 6px 20px rgba(0, 0, 0, 0.3)",
        send: "0 8px 24px rgba(99, 102, 241, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.15)",
        "send-hover": "0 12px 32px rgba(99, 102, 241, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.2)",
        "ring-accent": "0 0 0 4px rgba(99, 102, 241, 0.15)",
        card: "0 4px 24px rgba(0, 0, 0, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.03)",
      },
      transitionTimingFunction: {
        spring: "cubic-bezier(0.34, 1.56, 0.64, 1)",
        "out-expo": "cubic-bezier(0.16, 1, 0.3, 1)",
        "out-soft": "cubic-bezier(0.2, 0.9, 0.18, 1)",
      },
      keyframes: {
        floatSoft: {
          "0%, 100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-8px)" },
        },
        pulseRing: {
          "0%": { transform: "scale(0.86)", opacity: "0.9" },
          "100%": { transform: "scale(1.9)", opacity: "0" },
        },
        enterUp: {
          "0%": { opacity: "0", transform: "translateY(16px) scale(0.98)" },
          "100%": { opacity: "1", transform: "translateY(0) scale(1)" },
        },
        enterBubbleRight: {
          "0%": { opacity: "0", transform: "translate(12px, 8px) scale(0.96)" },
          "100%": { opacity: "1", transform: "translate(0, 0) scale(1)" },
        },
        enterBubbleLeft: {
          "0%": { opacity: "0", transform: "translate(-12px, 8px) scale(0.96)" },
          "100%": { opacity: "1", transform: "translate(0, 0) scale(1)" },
        },
        fadeIn: {
          "0%": { opacity: "0" },
          "100%": { opacity: "1" },
        },
        scaleIn: {
          "0%": { opacity: "0", transform: "scale(0.94)" },
          "100%": { opacity: "1", transform: "scale(1)" },
        },
        shimmer: {
          "0%": { backgroundPosition: "-240px 0" },
          "100%": { backgroundPosition: "240px 0" },
        },
        typingDot: {
          "0%, 80%, 100%": { transform: "translateY(0) scale(0.85)", opacity: "0.55" },
          "40%": { transform: "translateY(-4px) scale(1)", opacity: "1" },
        },
        glowPulse: {
          "0%, 100%": { boxShadow: "0 0 0 0 rgba(99, 102, 241, 0.45)" },
          "50%": { boxShadow: "0 0 0 8px rgba(99, 102, 241, 0)" },
        },
        slideInRight: {
          "0%": { opacity: "0", transform: "translateX(24px)" },
          "100%": { opacity: "1", transform: "translateX(0)" },
        },
        sheen: {
          "0%": { transform: "translateX(-150%) skewX(-12deg)" },
          "100%": { transform: "translateX(250%) skewX(-12deg)" },
        },
        gradient: {
          "0%, 100%": { backgroundPosition: "0% 50%" },
          "50%": { backgroundPosition: "100% 50%" },
        },
      },
      animation: {
        "float-soft": "floatSoft 6s ease-in-out infinite",
        "pulse-ring": "pulseRing 2s ease-out infinite",
        "enter-up": "enterUp 450ms cubic-bezier(0.16, 1, 0.3, 1) both",
        "enter-bubble-right": "enterBubbleRight 360ms cubic-bezier(0.16, 1, 0.3, 1) both",
        "enter-bubble-left": "enterBubbleLeft 360ms cubic-bezier(0.16, 1, 0.3, 1) both",
        "fade-in": "fadeIn 280ms ease-out both",
        "scale-in": "scaleIn 280ms cubic-bezier(0.16, 1, 0.3, 1) both",
        shimmer: "shimmer 2.3s linear infinite",
        "typing-dot": "typingDot 1.2s ease-in-out infinite",
        "glow-pulse": "glowPulse 2.4s ease-out infinite",
        "slide-in-right": "slideInRight 320ms cubic-bezier(0.16, 1, 0.3, 1) both",
        sheen: "sheen 1.6s cubic-bezier(0.16, 1, 0.3, 1)",
        gradient: "gradient 8s ease infinite",
      },
    },
  },
  plugins: [],
};
