import { useCallback, useEffect, useMemo, useState } from "react";
import {
  deleteNotification,
  getApiErrorMessage,
  getNotification,
  getNotifications,
  getUnreadNotificationCount,
  markAllNotificationsRead as markAllNotificationsReadRequest,
  markNotificationRead as markNotificationReadRequest,
  markNotificationUnread as markNotificationUnreadRequest,
} from "../api/notificationApi.js";
import PageShell from "../components/layout/PageShell.jsx";
import TraderLayout from "../components/layout/TraderLayout.jsx";
import Alert from "../components/shared/Alert.jsx";
import { classNames } from "../components/shared/utils.js";
import NotificationCard from "../features/notifications/NotificationCard.jsx";
import NotificationDetailDialog from "../features/notifications/NotificationDetailDialog.jsx";
import {
  normalizeNotification,
  normalizeNotificationList,
} from "../domain/notifications/notifications.js";
import { useTranslation } from "../i18n/useTranslation.js";
import { useAppDispatch, useAppSelector } from "../store/hooks.js";
import {
  applyNotificationReadAll,
  NOTIFICATION_FILTERS,
  countNotifications,
  notificationMatchesFilter,
  removeNotification,
  selectNotificationFilter,
  selectNotificationHasMore,
  selectNotificationItems,
  selectNotificationNextBeforeId,
  selectUnreadNotificationCount,
  setNotificationFilter,
  setNotificationPage,
  upsertNotification,
} from "../store/slices/notificationsSlice.js";

const PAGE_LIMIT = 20;

export default function NotificationPage() {
  const { t } = useTranslation();
  const dispatch = useAppDispatch();
  const activeFilter = useAppSelector(selectNotificationFilter);
  const items = useAppSelector(selectNotificationItems);
  const unreadCount = useAppSelector(selectUnreadNotificationCount);
  const hasMore = useAppSelector(selectNotificationHasMore);
  const nextBeforeId = useAppSelector(selectNotificationNextBeforeId);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [busyId, setBusyId] = useState(null);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState("");

  const visibleItems = useMemo(
    () => items.filter((item) => notificationMatchesFilter(item, activeFilter)),
    [activeFilter, items],
  );

  const loadInitial = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const [page, count] = await Promise.all([
        getNotifications({ limit: PAGE_LIMIT }),
        getUnreadNotificationCount(),
      ]);

      dispatch(
        setNotificationPage({
          items: normalizeNotificationList(page?.items),
          nextBeforeId: page?.nextBeforeId,
          hasMore: page?.hasMore,
          unreadCount: count?.unreadCount ?? page?.unreadCount,
        }),
      );
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, t, "errors.notificationsLoad"));
    } finally {
      setLoading(false);
    }
  }, [dispatch, t]);

  useEffect(() => {
    loadInitial();
  }, [loadInitial]);

  async function loadMore() {
    if (!hasMore || !nextBeforeId || loadingMore) return;

    setLoadingMore(true);
    setError("");
    try {
      const page = await getNotifications({ limit: PAGE_LIMIT, beforeId: nextBeforeId });
      dispatch(
        setNotificationPage({
          append: true,
          items: normalizeNotificationList(page?.items),
          nextBeforeId: page?.nextBeforeId,
          hasMore: page?.hasMore,
          unreadCount: page?.unreadCount,
        }),
      );
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, t, "errors.notificationsLoadMore"));
    } finally {
      setLoadingMore(false);
    }
  }

  async function openDetail(notificationId) {
    setBusyId(notificationId);
    setError("");
    try {
      let detail = normalizeNotification(await getNotification(notificationId));
      if (!detail) return;

      if (!detail.read) {
        detail = normalizeNotification(await markNotificationReadRequest(notificationId));
      }

      dispatch(upsertNotification(detail));
      setSelected(detail);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, t, "errors.notificationDetail"));
    } finally {
      setBusyId(null);
    }
  }

  async function toggleRead(item) {
    setBusyId(item.id);
    setError("");
    try {
      const response = item.read
        ? await markNotificationUnreadRequest(item.id)
        : await markNotificationReadRequest(item.id);
      const saved = normalizeNotification(response);
      dispatch(upsertNotification(saved));
      setSelected((current) => (current?.id === saved.id ? saved : current));
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, t, "errors.notificationUpdate"));
    } finally {
      setBusyId(null);
    }
  }

  async function markAllRead() {
    setBusyId("all");
    setError("");
    try {
      const response = await markAllNotificationsReadRequest();
      dispatch(applyNotificationReadAll(response));
      setSelected((current) =>
        current ? { ...current, read: true, readAt: current.readAt ?? response?.readAt } : current,
      );
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, t, "errors.notificationsReadAll"));
    } finally {
      setBusyId(null);
    }
  }

  async function remove(item) {
    if (!window.confirm(t("notifications.deleteConfirm", { title: item.title }))) return;

    setBusyId(item.id);
    setError("");
    try {
      await deleteNotification(item.id);
      dispatch(removeNotification(item.id));
      setSelected((current) => (current?.id === item.id ? null : current));
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, t, "errors.notificationDelete"));
    } finally {
      setBusyId(null);
    }
  }

  return (
    <TraderLayout active="notifications">
      <PageShell
        eyebrow={t("notifications.eyebrow")}
        title={t("notifications.title")}
        description={t("notifications.description")}
        action={
          <span className="meta-chip">
            {unreadCount > 0
              ? t("notifications.unread", { count: unreadCount })
              : t("notifications.allCaughtUp")}
          </span>
        }
      >
        {error ? (
          <Alert onRetry={loadInitial} onDismiss={() => setError("")}>
            {error}
          </Alert>
        ) : null}

        <section className="notification-layout">
          <aside className="notification-filter">
            {NOTIFICATION_FILTERS.map((filter) => (
              <button
                key={filter}
                className={classNames(filter === activeFilter && "is-active")}
                type="button"
                aria-pressed={filter === activeFilter}
                onClick={() => dispatch(setNotificationFilter(filter))}
              >
                {t(`notifications.filter.${filter}`)}
                <em>
                  {filter === "unread" ? unreadCount : countNotifications(items, filter)}
                </em>
              </button>
            ))}
          </aside>

          <div className="notification-list">
            <div className="panel-heading">
              <h2>{t("notifications.recent")}</h2>
              <button
                className="button button--ghost"
                type="button"
                disabled={unreadCount === 0 || busyId === "all"}
                onClick={markAllRead}
              >
                {t("notifications.markAllRead")}
              </button>
            </div>

            {loading ? (
              <div className="market-page-state">{t("notifications.loading")}</div>
            ) : (
              visibleItems.map((item) => (
                <NotificationCard
                  key={item.id}
                  item={item}
                  busy={busyId === item.id}
                  onOpen={openDetail}
                  onToggleRead={toggleRead}
                  onDelete={remove}
                />
              ))
            )}

            {!loading && visibleItems.length === 0 ? (
              <div className="market-page-state">{t("notifications.empty")}</div>
            ) : null}

            {!loading && hasMore ? (
              <button
                className="button button--ghost notification-load-more"
                type="button"
                onClick={loadMore}
                disabled={loadingMore}
              >
                {t(loadingMore ? "notifications.loadingMore" : "notifications.loadMore")}
              </button>
            ) : null}
          </div>
        </section>

        {selected ? (
          <NotificationDetailDialog
            item={selected}
            busy={busyId === selected.id}
            onClose={() => setSelected(null)}
            onToggleRead={toggleRead}
            onDelete={remove}
          />
        ) : null}
      </PageShell>
    </TraderLayout>
  );
}
