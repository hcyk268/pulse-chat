import test from "node:test";
import assert from "node:assert/strict";
import {
  normalizeNotification,
  normalizeNotificationList,
  resolveNotificationAction,
} from "../src/domain/notifications/notifications.js";

test("normalizes the notification DTO without losing target and source metadata", () => {
  const notification = normalizeNotification({
    id: 42,
    type: "MENTION",
    title: "Mentioned",
    body: null,
    actor: { id: 7, username: "alex" },
    targetType: "MESSAGE",
    targetId: 99,
    sourceType: "COMMUNITY",
    sourceId: 3,
    read: false,
    createdAt: "2026-07-31T00:00:00Z",
  });

  assert.equal(notification.body, "");
  assert.equal(notification.read, false);
  assert.equal(notification.actor.username, "alex");
  assert.equal(notification.targetType, "MESSAGE");
  assert.equal(notification.targetId, 99);
  assert.equal(notification.sourceType, "COMMUNITY");
  assert.equal(notification.sourceId, 3);
});

test("drops unusable list entries defensively", () => {
  assert.deepEqual(
    normalizeNotificationList([{ id: 1, type: "SYSTEM", read: true }, null, {}]).map(
      (item) => item.id,
    ),
    [1],
  );
});

test("resolves actionable navigation destinations for price alerts, mentions, and communities", () => {
  const priceAlertAction = resolveNotificationAction({
    id: 1,
    type: "PRICE_ALERT",
    sourceType: "MARKET",
    sourceId: "BTC",
  });
  assert.equal(priceAlertAction?.path, "/coins/btc");
  assert.equal(priceAlertAction?.labelKey, "notifications.actions.openMarket");

  const mentionAction = resolveNotificationAction({
    id: 2,
    type: "MENTION",
    targetType: "CONVERSATION",
    targetId: 101,
  });
  assert.equal(mentionAction?.path, "/chat?conversationId=101");
  assert.equal(mentionAction?.labelKey, "notifications.actions.openConversation");

  const communityAction = resolveNotificationAction({
    id: 3,
    type: "COMMUNITY",
    targetType: "COMMUNITY",
    targetId: "daily-strategy",
  });
  assert.equal(communityAction?.path, "/community/daily-strategy");
  assert.equal(communityAction?.labelKey, "notifications.actions.openCommunity");

  assert.equal(resolveNotificationAction(null), null);
});
