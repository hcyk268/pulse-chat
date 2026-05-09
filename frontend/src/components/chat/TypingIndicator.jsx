import Avatar from "../ui/Avatar";

export default function TypingIndicator({ user }) {
  return (
    <div className="flex animate-enter-up items-center gap-3">
      <Avatar user={user} size="sm" />
      <div className="flex items-center gap-1 rounded-2xl rounded-bl-md bg-[#182533] px-4 py-3 shadow-[0_8px_20px_rgba(0,0,0,0.18)]">
        <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-300 [animation-delay:-0.2s]" />
        <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-300 [animation-delay:-0.1s]" />
        <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-300" />
      </div>
    </div>
  );
}
