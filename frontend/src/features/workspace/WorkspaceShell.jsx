export default function WorkspaceShell({ className = "", children }) {
  return (
    <main className={`workspace-shell ${className}`.trim()} id="main">
      {children}
    </main>
  );
}
