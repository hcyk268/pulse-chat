import { useCallback, useEffect, useRef, useState } from "react";
import { Link, NavLink, useLocation } from "react-router-dom";
import {
  Bell,
  ChevronDown,
  Gauge,
  LogOut,
  Menu,
  MessageCircle,
  Moon,
  Bot,
  Star,
  Sun,
  TrendingUp,
  User,
  Users,
  X,
} from "lucide-react";
import logoUrl from "../../assets/trader-hub-logo.png";
import { getUnreadNotificationCount } from "../../api/notificationApi.js";
import { normalizeNotification } from "../../domain/notifications/notifications.js";
import GlobalSearch from "../../features/market/GlobalSearch";
import { useSignOut } from "../../features/auth/useSignOut";
import { useOutsideDismiss } from "../../hooks/useOutsideDismiss";
import { useRealtimeTopic } from "../../hooks/useRealtimeTopic.js";
import { useTranslation } from "../../i18n/useTranslation.js";
import { storeLocale } from "../../i18n/index.js";
import { useAppDispatch, useAppSelector } from "../../store/hooks";
import { selectCurrentUser, selectIsAuthenticated } from "../../store/slices/authSlice";
import {
  applyNotificationReadAll,
  removeNotification,
  resetNotifications,
  selectUnreadNotificationCount,
  setNotificationUnreadCount,
  upsertNotification,
} from "../../store/slices/notificationsSlice";
import {
  closeMobileMenu,
  openMobileMenu,
  selectMobileMenuOpen,
  selectTheme,
  setLocale,
  setTheme,
} from "../../store/slices/uiSlice";
import { storeTheme } from "../../utils/theme.js";
import { runtimeConfig } from "../../config/runtimeConfig.js";
import Avatar from "../shared/Avatar";
import { classNames, pageGroups } from "../shared/utils";

const navItems = [
  { labelKey: "nav.market", path: "/market", key: "market", icon: TrendingUp },
  { labelKey: "nav.community", path: "/community", key: "community", icon: Users },
  { labelKey: "nav.chat", path: "/chat", key: "chat", icon: MessageCircle },
  { labelKey: "nav.ai", path: "/ai", key: "ai", icon: Bot },
];

const accountLinks = [
  { labelKey: "nav.profile", path: "/profile", icon: User },
  { labelKey: "nav.watchlist", path: "/watchlist", icon: Star },
  { labelKey: "nav.notifications", path: "/notifications", icon: Bell },
  ...(runtimeConfig.enableAdminDemo
    ? [{ labelKey: "admin.consoleLink", path: "/admin", icon: Gauge }]
    : []),
];

export default function TraderHeader({ active = "market" }) {
  const dispatch = useAppDispatch();
  const location = useLocation();
  const { t, locale } = useTranslation();
  const menuOpen = useAppSelector(selectMobileMenuOpen);
  const theme = useAppSelector(selectTheme);
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const currentUser = useAppSelector(selectCurrentUser);
  const unreadCount = useAppSelector(selectUnreadNotificationCount);
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);
  const accountMenuRef = useRef(null);
  const mobileMenuRef = useRef(null);
  const signOut = useSignOut();

  useOutsideDismiss(accountMenuRef, () => setAccountMenuOpen(false), accountMenuOpen);

  useEffect(() => {
    dispatch(closeMobileMenu());
    setAccountMenuOpen(false);
  }, [dispatch, location.pathname]);

  useEffect(() => {
    if (!menuOpen) return undefined;

    const menu = mobileMenuRef.current;
    const previouslyFocused = document.activeElement;
    const focusableSelector =
      'a[href], button:not([disabled]), input:not([disabled]), [tabindex]:not([tabindex="-1"])';
    const getFocusable = () => Array.from(menu?.querySelectorAll(focusableSelector) ?? []);
    const focusFrame = requestAnimationFrame(() => getFocusable()[0]?.focus());

    function handleKeyDown(event) {
      if (event.key === "Escape") {
        dispatch(closeMobileMenu());
        return;
      }
      if (event.key !== "Tab") return;

      const focusable = getFocusable();
      if (focusable.length === 0) {
        event.preventDefault();
        return;
      }
      const first = focusable[0];
      const last = focusable.at(-1);
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      cancelAnimationFrame(focusFrame);
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKeyDown);
      previouslyFocused?.focus?.();
    };
  }, [dispatch, menuOpen]);

  useEffect(() => {
    if (!isAuthenticated) {
      dispatch(resetNotifications());
      return;
    }

    getUnreadNotificationCount()
      .then((response) => dispatch(setNotificationUnreadCount(response?.unreadCount ?? 0)))
      .catch(() => {
        // The notification page exposes a retryable error; the header badge stays quiet.
      });
  }, [dispatch, isAuthenticated]);

  const handleNotificationEvent = useCallback(
    (event) => {
      switch (event?.eventType) {
        case "notification.created":
        case "notification.updated": {
          const notification = normalizeNotification(event.data);
          if (notification) dispatch(upsertNotification(notification));
          break;
        }
        case "notification.read-all":
          dispatch(applyNotificationReadAll(event.data));
          break;
        case "notification.deleted":
          dispatch(
            removeNotification({
              id: event.data?.notificationId,
              unreadCount: event.data?.unreadCount,
            }),
          );
          break;
        default:
          break;
      }
    },
    [dispatch],
  );

  useRealtimeTopic(isAuthenticated ? "/user/queue/events" : null, handleNotificationEvent);
  const visibleNavItems = navItems;
  const displayName = currentUser?.displayName || currentUser?.username || t("nav.profile");
  const isDark = theme === "dark";
  const themeLabel = isDark ? t("theme.toLight") : t("theme.toDark");
  const nextLocale = locale === "vi" ? "en" : "vi";

  // Explicit choices are persisted; without one the app follows the OS/browser.
  function toggleTheme() {
    const next = isDark ? "light" : "dark";
    storeTheme(next);
    dispatch(setTheme(next));
  }

  function switchLocale() {
    storeLocale(nextLocale);
    dispatch(setLocale(nextLocale));
  }

  return (
    <header className="topbar">
      <div className="topbar__inner">
        <Link className="brand" to="/market" aria-label={t("nav.market")}>
          <img src={logoUrl} alt="" className="brand__mark" width="32" height="32" />
          <span>{t("common.appName")}</span>
        </Link>

        <nav className="top-nav" aria-label={t("nav.primary")}>
          {visibleNavItems.map((item) => (
            <NavLink
              key={item.key}
              to={item.path}
              className={({ isActive }) =>
                classNames(
                  "top-nav-link",
                  (isActive || pageGroups[item.key]?.includes(active)) && "is-active",
                )
              }
            >
              {t(item.labelKey)}
            </NavLink>
          ))}
        </nav>

        <div className="topbar__actions">
          <GlobalSearch />

          <button
            className="icon-button icon-button--text hide-sm"
            type="button"
            aria-label={t("language.switch")}
            title={t(`language.${nextLocale}`)}
            onClick={switchLocale}
          >
            {locale.toUpperCase()}
          </button>

          <button
            className="icon-button hide-sm"
            type="button"
            aria-label={themeLabel}
            title={themeLabel}
            onClick={toggleTheme}
          >
            {isDark ? <Sun size={19} /> : <Moon size={19} />}
          </button>

          {isAuthenticated ? (
            <Link
              className="icon-button hide-sm"
              to="/notifications"
              aria-label={
                unreadCount > 0
                  ? t("nav.notifications.unread", { count: unreadCount })
                  : t("nav.notifications")
              }
            >
              <Bell size={20} />
              {unreadCount > 0 ? <span className="notification-dot" /> : null}
            </Link>
          ) : null}

          {isAuthenticated ? (
            <div className="account-menu hide-sm" ref={accountMenuRef}>
              <button
                className="account-menu__trigger"
                type="button"
                aria-haspopup="menu"
                aria-expanded={accountMenuOpen}
                onClick={() => setAccountMenuOpen((current) => !current)}
              >
                <Avatar
                  name={displayName}
                  seed={currentUser?.username ?? displayName}
                  size="sm"
                  src={currentUser?.avatarUrl}
                />
                <span className="hide-xs">{displayName}</span>
                <ChevronDown size={15} />
              </button>

              {accountMenuOpen ? (
                <div className="account-menu__panel" role="menu">
                  <div className="account-menu__identity">
                    <strong>{displayName}</strong>
                    <span>{currentUser?.email || `@${currentUser?.username ?? ""}`}</span>
                  </div>
                  {accountLinks.map((item) => (
                    <Link key={item.path} to={item.path} role="menuitem">
                      <item.icon size={16} />
                      {t(item.labelKey)}
                      {item.path === "/notifications" && unreadCount > 0 ? (
                        <em>{unreadCount}</em>
                      ) : null}
                    </Link>
                  ))}
                  <button type="button" role="menuitem" onClick={signOut}>
                    <LogOut size={16} />
                    {t("common.signOut")}
                  </button>
                </div>
              ) : null}
            </div>
          ) : (
            <div className="guest-auth-actions hide-xs">
              <Link className="button button--ghost" to="/login">
                {t("common.signIn")}
              </Link>
              <Link className="button button--primary" to="/register">
                {t("common.createAccount")}
              </Link>
            </div>
          )}

          <button
            className="icon-button show-sm"
            type="button"
            aria-label={t("nav.openMenu")}
            onClick={() => dispatch(openMobileMenu())}
          >
            <Menu size={20} />
          </button>
        </div>
      </div>

      {menuOpen ? (
        <div ref={mobileMenuRef} className="mobile-menu" role="dialog" aria-modal="true" aria-label={t("nav.menu")}>
          <div className="mobile-menu__bar">
            <span>{t("nav.menu")}</span>
            <button
              className="icon-button"
              type="button"
              aria-label={t("nav.closeMenu")}
              onClick={() => dispatch(closeMobileMenu())}
            >
              <X size={20} />
            </button>
          </div>

          <GlobalSearch inline onNavigate={() => dispatch(closeMobileMenu())} />

          <nav className="mobile-menu__links" aria-label={t("nav.mobile")}>
            {visibleNavItems.map((item) => (
              <NavLink key={item.key} to={item.path} onClick={() => dispatch(closeMobileMenu())}>
                {t(item.labelKey)}
              </NavLink>
            ))}
            {isAuthenticated ? (
              <>
                <NavLink to="/watchlist" onClick={() => dispatch(closeMobileMenu())}>
                  {t("nav.watchlist")}
                </NavLink>
                <NavLink to="/notifications" onClick={() => dispatch(closeMobileMenu())}>
                  {t("nav.notifications")}
                  {unreadCount > 0 ? ` (${unreadCount})` : ""}
                </NavLink>
              </>
            ) : (
              <>
                <NavLink to="/login" onClick={() => dispatch(closeMobileMenu())}>
                  {t("common.signIn")}
                </NavLink>
                <NavLink to="/register" onClick={() => dispatch(closeMobileMenu())}>
                  {t("common.createAccount")}
                </NavLink>
              </>
            )}

            <button className="mobile-menu__theme" type="button" onClick={switchLocale}>
              {t(`language.${nextLocale}`)}
            </button>

            <button className="mobile-menu__theme" type="button" onClick={toggleTheme}>
              {isDark ? <Sun size={17} /> : <Moon size={17} />}
              {isDark ? t("theme.light") : t("theme.dark")}
            </button>

            {isAuthenticated ? (
              <button className="mobile-menu__signout" type="button" onClick={signOut}>
                {t("common.signOut")}
              </button>
            ) : null}
          </nav>
        </div>
      ) : null}
    </header>
  );
}
