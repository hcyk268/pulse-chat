/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        surface: {
          900: "#0b121a",
          800: "#0e1621",
          700: "#13202c",
          600: "#17212b",
          500: "#1c2733",
          400: "#202b36",
          300: "#242f3d",
          200: "#2b3948",
        },
        accent: {
          DEFAULT: "#2aabee",
          hover: "#3cb8f5",
          soft: "#6ab7ee",
          ring: "rgba(42, 171, 238, 0.35)",
        },
      },
      boxShadow: {
        glow: "0 0 35px rgba(34, 211, 238, 0.14)",
        "glow-lg": "0 0 60px rgba(42, 171, 238, 0.25)",
        panel: "0 24px 80px rgba(0, 0, 0, 0.45)",
        "panel-soft": "0 12px 40px rgba(0, 0, 0, 0.3)",
        "bubble-own": "0 10px 28px rgba(42, 123, 185, 0.32)",
        "bubble-other": "0 8px 22px rgba(0, 0, 0, 0.35)",
        "send": "0 8px 22px rgba(42, 171, 238, 0.32), inset 0 1px 0 rgba(255, 255, 255, 0.18)",
        "send-hover": "0 12px 30px rgba(42, 171, 238, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.22)",
        "ring-accent": "0 0 0 4px rgba(42, 171, 238, 0.18)",
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
          "0%": { opacity: "0", transform: "translateY(14px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
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
          "0%, 100%": { boxShadow: "0 0 0 0 rgba(42, 171, 238, 0.45)" },
          "50%": { boxShadow: "0 0 0 8px rgba(42, 171, 238, 0)" },
        },
        slideInRight: {
          "0%": { opacity: "0", transform: "translateX(24px)" },
          "100%": { opacity: "1", transform: "translateX(0)" },
        },
        sheen: {
          "0%": { transform: "translateX(-150%) skewX(-12deg)" },
          "100%": { transform: "translateX(250%) skewX(-12deg)" },
        },
      },
      animation: {
        "float-soft": "floatSoft 6s ease-in-out infinite",
        "pulse-ring": "pulseRing 2s ease-out infinite",
        "enter-up": "enterUp 420ms cubic-bezier(0.16, 1, 0.3, 1) both",
        "enter-bubble-right": "enterBubbleRight 360ms cubic-bezier(0.16, 1, 0.3, 1) both",
        "enter-bubble-left": "enterBubbleLeft 360ms cubic-bezier(0.16, 1, 0.3, 1) both",
        "fade-in": "fadeIn 280ms ease-out both",
        "scale-in": "scaleIn 280ms cubic-bezier(0.16, 1, 0.3, 1) both",
        shimmer: "shimmer 2.3s linear infinite",
        "typing-dot": "typingDot 1.2s ease-in-out infinite",
        "glow-pulse": "glowPulse 2.4s ease-out infinite",
        "slide-in-right": "slideInRight 320ms cubic-bezier(0.16, 1, 0.3, 1) both",
        sheen: "sheen 1.6s cubic-bezier(0.16, 1, 0.3, 1)",
      },
    },
  },
  plugins: [],
};
