/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      boxShadow: {
        glow: "0 0 35px rgba(34, 211, 238, 0.14)",
        panel: "0 24px 80px rgba(0, 0, 0, 0.45)",
      },
      keyframes: {
        floatSoft: {
          "0%, 100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-8px)" },
        },
        pulseRing: {
          "0%": { transform: "scale(0.86)", opacity: "0.9" },
          "100%": { transform: "scale(1.8)", opacity: "0" },
        },
        enterUp: {
          "0%": { opacity: "0", transform: "translateY(14px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        shimmer: {
          "0%": { backgroundPosition: "-240px 0" },
          "100%": { backgroundPosition: "240px 0" },
        },
      },
      animation: {
        "float-soft": "floatSoft 6s ease-in-out infinite",
        "pulse-ring": "pulseRing 1.8s ease-out infinite",
        "enter-up": "enterUp 420ms ease-out both",
        shimmer: "shimmer 2.3s linear infinite",
      },
    },
  },
  plugins: [],
};
