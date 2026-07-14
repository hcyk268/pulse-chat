export default function IconButton({ icon: Icon, label, onClick, disabled = false, className = "" }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={[
        "press flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-slate-400 hover:bg-white/5 hover:text-white disabled:cursor-not-allowed disabled:text-slate-700",
        className,
      ].filter(Boolean).join(" ")}
      title={label}
    >
      <Icon size={16} />
    </button>
  );
}
