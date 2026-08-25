export const NOTIFICATION_TYPE_KEYS = {
  PRICE_ALERT: "notifications.type.priceAlert",
  COMMUNITY: "notifications.type.community",
  MENTION: "notifications.type.mention",
  SYSTEM: "notifications.type.system",
};

export const NOTIFICATION_TARGET_KEYS = {
  MARKET_ASSET: "notifications.target.marketAsset",
  PRICE_ALERT: "notifications.target.priceAlert",
  COMMUNITY: "notifications.target.community",
  CONVERSATION: "notifications.target.conversation",
  MESSAGE: "notifications.target.message",
};

export function normalizeNotification(notification) {
  if (!notification?.id) return null;

  return {
    ...notification,
    body: notification.body ?? "",
    read: Boolean(notification.read),
    actor: notification.actor ?? null,
    targetType: notification.targetType ?? null,
    targetId: notification.targetId ?? null,
    sourceType: notification.sourceType ?? null,
    sourceId: notification.sourceId ?? null,
    readAt: notification.readAt ?? null,
    createdAt: notification.createdAt ?? null,
    updatedAt: notification.updatedAt ?? null,
  };
}

export function normalizeNotificationList(items) {
  return (items ?? []).map(normalizeNotification).filter(Boolean);
}

export function resolveNotificationAction(notification) {
  if (!notification) return null;

  if (
    notification.type === "PRICE_ALERT" ||
    notification.sourceType === "MARKET" ||
    notification.targetType === "MARKET_ASSET" ||
    notification.targetType === "PRICE_ALERT"
  ) {
    const symbol = (notification.sourceId || notification.targetId || "").toString().toLowerCase();
    if (symbol) {
      return {
        path: `/coins/${symbol}`,
        labelKey: "notifications.actions.openMarket",
        kind: "market",
      };
    }
  }

  if (
    notification.type === "MENTION" ||
    notification.targetType === "CONVERSATION" ||
    notification.targetType === "MESSAGE" ||
    notification.sourceType === "CONVERSATION"
  ) {
    const conversationId = notification.targetType === "CONVERSATION"
      ? notification.targetId
      : (notification.sourceId || notification.targetId);
    return {
      path: conversationId ? `/chat?conversationId=${conversationId}` : "/chat",
      labelKey: "notifications.actions.openConversation",
      kind: "chat",
    };
  }

  if (
    notification.type === "COMMUNITY" ||
    notification.targetType === "COMMUNITY" ||
    notification.sourceType === "COMMUNITY"
  ) {
    const slug = notification.sourceId || notification.targetId;
    return {
      path: slug ? `/community/${slug}` : "/community",
      labelKey: "notifications.actions.openCommunity",
      kind: "community",
    };
  }

  return null;
}
