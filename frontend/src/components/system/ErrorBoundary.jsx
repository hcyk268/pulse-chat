import { Component } from "react";

export class ErrorBoundary extends Component {
  state = {
    error: null,
  };

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, errorInfo) {
    this.props.onError?.(error, errorInfo);

    if (import.meta.env.DEV) {
      console.error("Unhandled application error", error, errorInfo);
    }
  }

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (!this.state.error) {
      return this.props.children;
    }

    return (
      <main className="flex min-h-screen items-center justify-center bg-[#0a0f1a] p-6 text-slate-100">
        <section
          role="alert"
          className="w-full max-w-lg rounded-3xl border border-white/10 bg-slate-900/90 p-8 shadow-2xl shadow-black/40"
        >
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-cyan-300">
            Pulse Chat
          </p>
          <h1 className="mt-4 text-2xl font-semibold">Something went wrong</h1>
          <p className="mt-3 text-sm leading-6 text-slate-400">
            The application hit an unexpected error. Reload to start from a clean state.
          </p>
          <button
            type="button"
            onClick={this.handleReload}
            className="mt-6 rounded-xl bg-cyan-400 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-cyan-300 focus:outline-none focus:ring-2 focus:ring-cyan-300 focus:ring-offset-2 focus:ring-offset-slate-900"
          >
            Reload application
          </button>
        </section>
      </main>
    );
  }
}
