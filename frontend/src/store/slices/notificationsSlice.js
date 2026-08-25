import { createSlice } from "@reduxjs/toolkit";

/** Stable ids: the visible label comes from `notifications.filter.{id}`. */
export const NOTIFICATION_FILTERS = ["all", "unread", "alerts", "mentions", "community"];

const filterPredicates = {
  all: () => true,
  unread: (item) => !item.read,
  alerts: (item) => item.type === "PRICE_ALERT",
  mentions: (item) => item.type === "MENTION",
  community: (item) => item.type === "COMMUNITY",
};

export function notificationMatchesFilter(item, filter) {
  return (filterPredicates[filter] ?? filterPredicates.all)(item);
}

export function countNotifications(items, filter) {
  return items.filter((item) => notificationMatchesFilter(item, filter)).length;
}

function mergeUniqueNotifications(current, incoming) {
  const byId = new Map(current.map((item) => [String(item.id), item]));

  incoming.forEach((item) => {
    const key = String(item.id);
    byId.set(key, { ...byId.get(key), ...item });
  });

  return [...byId.values()].sort((left, right) => Number(right.id) - Number(left.id));
}

const initialState = {
  activeFilter: NOTIFICATION_FILTERS[0],
  items: [],
  unreadCount: 0,
  nextBeforeId: null,
  hasMore: false,
  loaded: false,
};

const notificationsSlice = createSlice({
  name: "notifications",
  initialState,
  reducers: {
    setNotificationFilter(state, action) {
      state.activeFilter = NOTIFICATION_FILTERS.includes(action.payload)
        ? action.payload
        : NOTIFICATION_FILTERS[0];
    },
    setNotificationPage(state, action) {
      const { append = false, hasMore, items, nextBeforeId, unreadCount } = action.payload;
      state.items = append ? mergeUniqueNotifications(state.items, items) : items;
      state.nextBeforeId = nextBeforeId ?? null;
      state.hasMore = Boolean(hasMore);
      state.unreadCount = Math.max(0, Number(unreadCount) || 0);
      state.loaded = true;
    },
    setNotificationUnreadCount(state, action) {
      state.unreadCount = Math.max(0, Number(action.payload) || 0);
    },
    upsertNotification(state, action) {
      const notification = action.payload;
      if (!notification?.id) return;

      const index = state.items.findIndex((item) => String(item.id) === String(notification.id));
      const previous = index >= 0 ? state.items[index] : null;

      if (previous) {
        state.unreadCount += Number(previous.read) - Number(notification.read);
        state.items[index] = { ...previous, ...notification };
      } else {
        state.items.unshift(notification);
        if (!notification.read) state.unreadCount += 1;
      }

      state.unreadCount = Math.max(0, state.unreadCount);
    },
    removeNotification(state, action) {
      const payload = action.payload;
      const notificationId = payload?.id ?? payload;
      const existing = state.items.find(
        (item) => String(item.id) === String(notificationId),
      );

      state.items = state.items.filter(
        (item) => String(item.id) !== String(notificationId),
      );

      if (payload?.unreadCount !== undefined) {
        state.unreadCount = Math.max(0, Number(payload.unreadCount) || 0);
      } else if (existing && !existing.read) {
        state.unreadCount = Math.max(0, state.unreadCount - 1);
      }
    },
    applyNotificationReadAll(state, action) {
      const readAt = action.payload?.readAt ?? new Date().toISOString();
      state.items = state.items.map((item) => ({
        ...item,
        read: true,
        readAt: item.readAt ?? readAt,
      }));
      state.unreadCount = Math.max(0, Number(action.payload?.unreadCount) || 0);
    },
    // Local reducers remain useful for deterministic UI tests and optimistic fallbacks.
    markNotificationRead(state, action) {
      const item = state.items.find((entry) => String(entry.id) === String(action.payload));
      if (item && !item.read) {
        item.read = true;
        item.readAt = new Date().toISOString();
        state.unreadCount = Math.max(0, state.unreadCount - 1);
      }
    },
    toggleNotificationRead(state, action) {
      const item = state.items.find((entry) => String(entry.id) === String(action.payload));
      if (!item) return;

      item.read = !item.read;
      item.readAt = item.read ? new Date().toISOString() : null;
      state.unreadCount = Math.max(0, state.unreadCount + (item.read ? -1 : 1));
    },
    markAllNotificationsRead(state) {
      const readAt = new Date().toISOString();
      state.items.forEach((item) => {
        item.read = true;
        item.readAt = item.readAt ?? readAt;
      });
      state.unreadCount = 0;
    },
    resetNotifications() {
      return initialState;
    },
  },
});

export const {
  applyNotificationReadAll,
  markAllNotificationsRead,
  markNotificationRead,
  removeNotification,
  resetNotifications,
  setNotificationFilter,
  setNotificationPage,
  setNotificationUnreadCount,
  toggleNotificationRead,
  upsertNotification,
} = notificationsSlice.actions;

export const selectNotificationFilter = (state) => state.notifications.activeFilter;
export const selectNotificationItems = (state) => state.notifications.items;
export const selectUnreadNotificationCount = (state) => state.notifications.unreadCount;
export const selectNotificationHasMore = (state) => state.notifications.hasMore;
export const selectNotificationNextBeforeId = (state) => state.notifications.nextBeforeId;
export const selectNotificationsLoaded = (state) => state.notifications.loaded;
export default notificationsSlice.reducer;
