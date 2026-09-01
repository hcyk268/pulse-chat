import { Activity, ArrowLeft, Bot, MessageCircleMore, TrendingUp } from "lucide-react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import logoUrl from "../../assets/trader-hub-logo.png";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useAppSelector } from "../../store/hooks.js";
import { selectIsAuthenticated } from "../../store/slices/authSlice.js";
import AuthForm from "./AuthForm.jsx";

const showcaseItems = [
  { key: "market", icon: TrendingUp },
  { key: "ai", icon: Bot },
  { key: "community", icon: MessageCircleMore },
];

function AuthShowcase() {
  const { t } = useTranslation();

  return (
    <section className="auth-showcase" aria-label={t("auth.showcase.label")}>
      <div className="auth-showcase__glow" aria-hidden="true" />
      <Link className="brand auth-showcase__brand" to="/market">
        <img src={logoUrl} alt="" className="brand__mark" width="34" height="34" />
        <span>{t("common.appName")}</span>
      </Link>
      <div className="auth-showcase__copy">
        <span className="auth-showcase__eyebrow"><Activity size={15} /> {t("auth.showcase.eyebrow")}</span>
        <h1>{t("auth.showcase.title")}</h1>
        <p>{t("auth.showcase.description")}</p>
      </div>
      <div className="auth-showcase__features">
        {showcaseItems.map(({ key, icon: Icon }, index) => (
          <article className={index === 0 ? "is-active" : undefined} key={key}>
            <span><Icon size={19} /></span>
            <div>
              <strong>{t(`auth.showcase.${key}.title`)}</strong>
              <small>{t(`auth.showcase.${key}.body`)}</small>
            </div>
          </article>
        ))}
      </div>
      <div className="auth-showcase__status">
        <i /> {t("auth.showcase.live")}
      </div>
    </section>
  );
}

export default function AuthPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const isRegister = location.pathname === "/register";
  const requestedRedirect = location.state?.from;
  const redirectTo = typeof requestedRedirect === "string" && requestedRedirect.startsWith("/")
    ? requestedRedirect
    : "/chat";
  const returnState = location.state?.authIntent ? { authIntent: location.state.authIntent } : undefined;

  if (isAuthenticated) return <Navigate to={redirectTo} replace state={returnState} />;

  return (
    <main className="auth-page auth-page--split">
      <AuthShowcase />
      <section className="auth-pane">
        <Link className="auth-pane__back" to="/market">
          <ArrowLeft size={16} /> {t("auth.backToMarket")}
        </Link>
        <div className="auth-card">
          <div className="brand brand--center auth-card__brand">
            <img src={logoUrl} alt="" className="brand__mark" width="32" height="32" />
            <span>{t("common.appName")}</span>
          </div>
          <h2>{t(isRegister ? "auth.registerTitle" : "auth.signInTitle")}</h2>
          <p>{t(isRegister ? "auth.registerSubtitle" : "auth.signInSubtitle")}</p>
          <AuthForm
            mode={isRegister ? "register" : "login"}
            onAuthenticated={() => navigate(redirectTo, { replace: true, state: returnState })}
            onModeChange={(mode) =>
              navigate(mode === "register" ? "/register" : "/login", {
                replace: true,
                state: location.state,
              })
            }
            onRegistrationComplete={(email) =>
              navigate("/verify-email", {
                replace: true,
                state: { ...location.state, email, registrationComplete: true },
              })
            }
            showSessionExpired
          />
        </div>
      </section>
    </main>
  );
}