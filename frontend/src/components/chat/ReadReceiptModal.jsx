import { X } from "lucide-react";
import { formatLongTime } from "../../utils/formatters";
import Avatar from "../ui/Avatar";
import IconButton from "../ui/IconButton";

export default function ReadReceiptModal({ message, onClose, receipts }) {
  return (
    <div
      className="absolute inset-0 z-40 flex items-center justify-center bg-black/55 px-4 backdrop-blur-sm"
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div className="w-full max-w-sm overflow-hidden rounded-2xl border border-white/5 bg-[#111827] shadow-panel">
        <div className="flex items-center justify-between border-b border-white/5 px-4 py-3">
          <div>
            <p className="text-sm font-semibold text-white">Read receipts</p>
            <p className="text-xs text-slate-500">Message #{message.id}</p>
          </div>
          <IconButton icon={X} label="Close" onClick={onClose} />
        </div>
        <div className="max-h-80 overflow-y-auto p-2">
          {receipts.length > 0 ? (
            receipts.map((receipt) => (
              <div key={`${receipt.user?.id}-${receipt.readAt}`} className="flex items-center gap-3 rounded-xl px-2 py-2">
                <Avatar user={receipt.user} size="sm" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-white">
                    {receipt.user?.displayName ?? receipt.user?.username ?? "User"}
                  </p>
                  <p className="text-xs text-slate-500">{formatLongTime(receipt.readAt)}</p>
                </div>
              </div>
            ))
          ) : (
            <div className="px-4 py-8 text-center text-sm text-slate-500">No reads yet</div>
          )}
        </div>
      </div>
    </div>
  );
}
