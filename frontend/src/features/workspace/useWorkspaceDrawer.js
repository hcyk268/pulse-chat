import { useCallback, useEffect, useState } from "react";

/**
 * Below 900px the workspace sidebar renders as an overlay drawer. The flag is
 * inert on wider screens, where the sidebar is always part of the grid.
 */
export function useWorkspaceDrawer() {
  const [open, setOpen] = useState(false);

  const closeDrawer = useCallback(() => setOpen(false), []);
  const openDrawer = useCallback(() => setOpen(true), []);

  useEffect(() => {
    if (!open) return undefined;

    function handleKeyDown(event) {
      if (event.key === "Escape") setOpen(false);
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [open]);

  return { open, openDrawer, closeDrawer };
}

export default useWorkspaceDrawer;
