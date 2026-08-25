import { Link } from "react-router-dom";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useAppSelector } from "../../store/hooks";
import { selectIsAuthenticated } from "../../store/slices/authSlice";

const footerLinks = [
  { labelKey: "footer.market", path: "/market" },
  { labelKey: "footer.watchlist", path: "/watchlist", authRequired: true },
  { labelKey: "footer.communities", path: "/community" },
  { labelKey: "footer.messages", path: "/chat", authRequired: true },
];

export default function TraderFooter() {
  const { t } = useTranslation();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const visibleLinks = footerLinks.filter((link) => !link.authRequired || isAuthenticated);

  return (
    <footer className="footer">
      <div>
        <strong>{t("common.appName")}</strong>
        <p>{t("footer.tagline")}</p>
      </div>
      <nav aria-label={t("nav.footer")}>
        {visibleLinks.map((link) => (
          <Link key={link.path} to={link.path}>
            {t(link.labelKey)}
          </Link>
        ))}
      </nav>
    </footer>
  );
}
