import { Compass } from "lucide-react";
import { Link, useLocation } from "react-router-dom";
import { useTranslation } from "../i18n/useTranslation.js";

export default function NotFoundPage() {
  const location = useLocation();
  const { t } = useTranslation();

  return (
    <main className="auth-page">
      <section className="auth-card">
        <span className="error-badge">
          <Compass size={22} />
        </span>
        <h1>{t("notFound.title")}</h1>
        <p>{t("notFound.body", { path: location.pathname })}</p>
        <div className="auth-card__actions">
          <Link className="button button--primary" to="/market">
            {t("notFound.market")}
          </Link>
        </div>
      </section>
    </main>
  );
}
