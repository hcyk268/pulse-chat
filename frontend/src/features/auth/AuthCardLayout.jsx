import { Link } from "react-router-dom";
import logoUrl from "../../assets/trader-hub-logo.png";
import { useTranslation } from "../../i18n/useTranslation.js";

export default function AuthCardLayout({ title, subtitle, children, backToLogin = false }) {
  const { t } = useTranslation();

  return (
    <main className="auth-page">
      <section className="auth-card">
        <div className="brand brand--center">
          <img src={logoUrl} alt="" className="brand__mark" width="32" height="32" />
          <span>{t("common.appName")}</span>
        </div>
        <h1>{title}</h1>
        <p>{subtitle}</p>
        {children}
        {backToLogin ? (
          <Link className="auth-switch auth-card__back" to="/login">
            {t("auth.backToSignIn")}
          </Link>
        ) : null}
      </section>
    </main>
  );
}
