import { initials } from "../../utils/formatters";

const sizeClasses = {
  sm: "h-9 w-9 text-xs",
  md: "h-11 w-11 text-sm",
  lg: "h-14 w-14 text-base",
  xl: "h-20 w-20 text-2xl",
};

export default function Avatar({ user, size = "md", showStatus = false }) {
  const online = user?.presence?.isOnline;
  const accent = user?.accent ?? "from-slate-400 to-cyan-400";

  return (
    <div className={`relative shrink-0 ${sizeClasses[size] ?? sizeClasses.md}`}>
      <div
        className={`flex h-full w-full items-center justify-center rounded-full bg-gradient-to-br ${accent} font-semibold text-slate-950 shadow-[0_10px_24px_rgba(0,0,0,0.28)]`}
      >
        {initials(user?.displayName || user?.username || "U")}
      </div>
      {showStatus ? (
        <span className="absolute -bottom-0.5 -right-0.5 flex h-3.5 w-3.5 items-center justify-center rounded-full bg-slate-950">
          <span
            className={`h-2.5 w-2.5 rounded-full ${online ? "bg-emerald-400" : "bg-slate-500"}`}
          />
          {online ? (
            <span className="absolute h-2.5 w-2.5 animate-pulse-ring rounded-full bg-emerald-400" />
          ) : null}
        </span>
      ) : null}
    </div>
  );
}
