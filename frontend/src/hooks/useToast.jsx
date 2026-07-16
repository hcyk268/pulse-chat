import { AlertTriangle, CheckCircle2, Info, X, XCircle } from "lucide-react";
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";

const ToastContext = createContext(null);

const ICONS = {
  error: XCircle,
  info: Info,
  success: CheckCircle2,
  warning: AlertTriangle,
};

const TITLES = {
  error: "Something went wrong",
  info: "Heads up",
  success: "Done",
  warning: "Please check",
};

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const sequenceRef = useRef(0);
  const recentRef = useRef(new Map());

  const dismiss = useCallback((id) => {
    setToasts((previous) => previous.filter((toast) => toast.id !== id));
  }, []);

  const push = useCallback((message, options = {}) => {
    const normalizedMessage = String(message ?? "").trim();
    if (!normalizedMessage) return null;

    const type = options.type ?? "info";
    const signature = `${type}:${normalizedMessage}`;
    const now = Date.now();
    if (now - (recentRef.current.get(signature) ?? 0) < 1500) return null;
    recentRef.current.set(signature, now);

    sequenceRef.current += 1;
    const id = `${now}-${sequenceRef.current}`;
    const toast = {
      id,
      message: normalizedMessage,
      title: options.title ?? TITLES[type],
      type,
      duration: options.duration ?? (type === "error" ? 6000 : 4200),
    };
    setToasts((previous) => [...previous.slice(-3), toast]);
    return id;
  }, []);

  const value = useMemo(() => ({
    dismiss,
    error: (message, options) => push(message, { ...options, type: "error" }),
    info: (message, options) => push(message, { ...options, type: "info" }),
    push,
    success: (message, options) => push(message, { ...options, type: "success" }),
    warning: (message, options) => push(message, { ...options, type: "warning" }),
  }), [dismiss, push]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-viewport" aria-live="polite" aria-label="Notifications">
        {toasts.map((toast) => (
          <ToastItem key={toast.id} toast={toast} onDismiss={dismiss} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

function ToastItem({ toast, onDismiss }) {
  const Icon = ICONS[toast.type] ?? Info;

  useEffect(() => {
    const timeoutId = window.setTimeout(() => onDismiss(toast.id), toast.duration);
    return () => window.clearTimeout(timeoutId);
  }, [onDismiss, toast.duration, toast.id]);

  return (
    <div
      role={toast.type === "error" ? "alert" : "status"}
      className={`toast toast--${toast.type}`}
      style={{ "--toast-duration": `${toast.duration}ms` }}
    >
      <span className="toast__icon"><Icon size={18} /></span>
      <span className="toast__content">
        <strong>{toast.title}</strong>
        <span>{toast.message}</span>
      </span>
      <button type="button" onClick={() => onDismiss(toast.id)} aria-label="Dismiss notification" className="toast__close">
        <X size={16} />
      </button>
      <span className="toast__progress" aria-hidden="true" />
    </div>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) throw new Error("useToast must be used inside ToastProvider");
  return context;
}
