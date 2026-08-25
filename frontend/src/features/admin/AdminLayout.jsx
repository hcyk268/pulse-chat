import { ArrowLeft, Boxes, LayoutDashboard, ScrollText, ShieldAlert, Users } from "lucide-react";
import { Link, NavLink } from "react-router-dom";
import logoUrl from "../../assets/trader-hub-logo.png";
import { classNames } from "../../components/shared/utils";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useAppSelector } from "../../store/hooks";
import { selectOpenReportCount } from "../../store/slices/adminSlice";
import { selectCurrentUser } from "../../store/slices/authSlice";

const navItems = [
  { labelKey: "admin.nav.overview", path: "/admin", icon: LayoutDashboard, end: true },
  { labelKey: "admin.nav.users", path: "/admin/users", icon: Users },
  { labelKey: "admin.nav.moderation", path: "/admin/moderation", icon: ShieldAlert, badge: true },
  { labelKey: "admin.nav.communities", path: "/admin/communities", icon: Boxes },
  { labelKey: "admin.nav.audit", path: "/admin/audit", icon: ScrollText },
];

export default function AdminLayout({ title, description, actions, children }) {
  const { t } = useTranslation();
  const currentUser = useAppSelector(selectCurrentUser);
  const openReports = useAppSelector(selectOpenReportCount);

  return (
    <div className="admin-shell">
      <a className="skip-link" href="#main">
        {t("a11y.skipToContent")}
      </a>

      <aside className="admin-sidebar">
        <Link className="admin-sidebar__brand" to="/admin">
          <img src={logoUrl} alt="" width="28" height="28" />
          <span>{t("admin.console")}</span>
        </Link>

        <nav className="admin-nav" aria-label={t("admin.nav.label")}>
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.end}
              className={({ isActive }) => classNames(isActive && "active")}
            >
              <item.icon size={17} />
              {t(item.labelKey)}
              {item.badge && openReports > 0 ? <em>{openReports}</em> : null}
            </NavLink>
          ))}
        </nav>

        <div className="admin-sidebar__footer">
          <p className="admin-sample-note">{t("admin.sampleData")}</p>
          <Link className="button button--ghost" to="/market">
            <ArrowLeft size={16} /> {t("admin.backToApp")}
          </Link>
        </div>
      </aside>

      <div className="admin-main">
        <header className="admin-topbar">
          <div>
            <h1>{title}</h1>
            {description ? <p>{description}</p> : null}
          </div>
          <div className="admin-topbar__actions">
            {actions}
            <span className="meta-chip">
              {currentUser?.displayName || currentUser?.username || t("admin.signedInAdmin")}
            </span>
          </div>
        </header>

        <main className="admin-content" id="main">
          {children}
        </main>
      </div>
    </div>
  );
}
