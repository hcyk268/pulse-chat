import Avatar from "../ui/Avatar";

export default function TypingIndicator({ user }) {
  return (
    <div className="flex animate-enter-bubble-left items-end gap-2">
      <Avatar user={user} size="sm" />
      <div className="flex items-center gap-1.5 rounded-2xl rounded-bl-md bg-[#1e293b] px-4 py-3 shadow-bubble-other">
        <span className="typing-dot" />
        <span className="typing-dot" />
        <span className="typing-dot" />
      </div>
    </div>
  );
}
