import { useEffect } from "react";
import { useLatestRef } from "./useLatestRef.js";

/**
 * Closes a popover when the pointer lands outside `elementRef` or Escape is pressed.
 */
export function useOutsideDismiss(elementRef, onDismiss, enabled = true) {
  const dismissRef = useLatestRef(onDismiss);

  useEffect(() => {
    if (!enabled) return undefined;

    function handlePointerDown(event) {
      if (!elementRef.current || elementRef.current.contains(event.target)) return;
      dismissRef.current?.();
    }

    function handleKeyDown(event) {
      if (event.key === "Escape") dismissRef.current?.();
    }

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [dismissRef, elementRef, enabled]);
}

export default useOutsideDismiss;
