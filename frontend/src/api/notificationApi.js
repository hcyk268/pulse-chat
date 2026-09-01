import httpClient, { getApiErrorMessage, unwrap } from "./httpClient.js";

export { getApiErrorMessage };

const NOTIFICATION_PATH = "/api/v1/notifications";

export async function getNotifications({ limit = 20, beforeId } = {}) {
  return unwrap(
    await httpClient.get(NOTIFICATION_PATH, {
      params: {
        limit,
        ...(beforeId ? { beforeId } : {}),
      },
    }),
  );
}

export async function getUnreadNotificationCount() {
  return unwrap(await httpClient.get(`${NOTIFICATION_PATH}/unread-count`));
}

export async function getNotification(notificationId) {
  return unwrap(
    await httpClient.get(`${NOTIFICATION_PATH}/${encodeURIComponent(notificationId)}`),
  );
}

export async function markNotificationRead(notificationId) {
  return unwrap(
    await httpClient.patch(
      `${NOTIFICATION_PATH}/${encodeURIComponent(notificationId)}/read`,
    ),
  );
}

export async function markNotificationUnread(notificationId) {
  return unwrap(
    await httpClient.patch(
      `${NOTIFICATION_PATH}/${encodeURIComponent(notificationId)}/unread`,
    ),
  );
}

export async function markAllNotificationsRead() {
  return unwrap(await httpClient.patch(`${NOTIFICATION_PATH}/read-all`));
}

export async function deleteNotification(notificationId) {
  return unwrap(
    await httpClient.delete(`${NOTIFICATION_PATH}/${encodeURIComponent(notificationId)}`),
  );
}
