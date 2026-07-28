import { Component } from "react";
import { AlertTriangle } from "lucide-react";
import { useTranslation } from "../../i18n/useTranslation.js";

function ErrorFallback({ onReset }) {
  const { t } = useTranslation();

  return (
    <main className="auth-page">
      <section className="auth-card">
        <span className="error-badge">
          <AlertTriangle size={22} />
        </span>
        <h1>{t("boundary.title")}</h1>
        <p>{t("boundary.body")}</p>
        <button className="button button--primary" type="button" onClick={onReset}>
          {t("boundary.action")}
        </button>
      </section>
    </main>
  );
}

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
    this.handleReset = this.handleReset.bind(this);
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    // Surface the stack in the console; there is no error reporting backend yet.
    console.error("Unhandled UI error", error, info?.componentStack);
  }

  handleReset() {
    this.setState({ error: null });
    window.location.assign("/market");
  }

  render() {
    if (!this.state.error) return this.props.children;

    return <ErrorFallback onReset={this.handleReset} />;
  }
}
