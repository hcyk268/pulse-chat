import test from "node:test";
import assert from "node:assert/strict";
import {
  applyIncomingMessageToList,
  applyMessageResponseToList,
  applyMessageStatusToList,
  applyOutgoingMessageToList,
  applyPresenceToList,
  applyReadReceiptToList,
  mergeConversationList,
  mergeMessageIntoList,
  prependMessageHistory,
  resetConversationUnread,
} from "../src/domain/chat/conversationState.js";

function createConversation(overrides = {}) {
  return {
    id: 1,
    messages: [],
    unreadCount: 0,
    createdAt: "2026-01-01T00:00:00.000Z",
    ...overrides,
  };
}

test("merges conversations while preserving loaded messages and recency order", () => {
  const older = createConversation({
    id: 1,
    messages: [{ id: 10 }],
    lastMessageAt: "2026-01-01T00:00:00.000Z",
  });
  const newer = createConversation({
    id: 2,
    lastMessageAt: "2026-01-02T00:00:00.000Z",
  });

  const result = mergeConversationList(
    [older],
    [newer, { ...older, messages: [] }],
  );

  assert.deepEqual(result.map((item) => item.id), [2, 1]);
  assert.deepEqual(result[1].messages, [{ id: 10 }]);
});

test("applies incoming messages and increments unread only for other users", () => {
  const incoming = {
    id: 11,
    conversationId: 1,
    senderId: 2,
    content: "Hello",
    status: "SENT",
    createdAt: "2026-01-02T00:00:00.000Z",
  };

  const result = applyIncomingMessageToList(
    [createConversation()],
    incoming,
    1,
  );

  assert.equal(result.applied, true);
  assert.equal(result.conversations[0].unreadCount, 1);
  assert.equal(result.conversations[0].lastMessage.contentPreview, "Hello");

  const pending = applyIncomingMessageToList([], incoming, 1);
  assert.equal(pending.applied, false);
  assert.deepEqual(pending.conversations, []);
});

test("updates edited message content inside replies", () => {
  const original = { id: 10, conversationId: 1, content: "Old" };
  const reply = {
    id: 11,
    conversationId: 1,
    content: "Reply",
    replyTo: { id: 10, content: "Old" },
  };
  const edited = { ...original, content: "New", editedAt: "2026-01-02" };

  const [conversation] = applyMessageResponseToList(
    [createConversation({ messages: [original, reply], lastMessage: reply })],
    edited,
  );

  assert.equal(conversation.messages[0].content, "New");
  assert.equal(conversation.messages[1].replyTo.content, "New");
});

test("replaces the optimistic message instead of duplicating it", () => {
  const optimistic = { id: null, clientMessageId: "abc", conversationId: 1, pending: true };
  const confirmed = {
    id: 21,
    clientMessageId: "abc",
    conversationId: 1,
    content: "Sent",
    createdAt: "2026-01-02T00:00:00.000Z",
  };

  const merged = mergeMessageIntoList([optimistic], confirmed);
  assert.deepEqual(merged, [confirmed]);

  const [conversation] = applyOutgoingMessageToList(
    [createConversation({ messages: [optimistic] })],
    confirmed,
  );
  assert.equal(conversation.messages.length, 1);
  assert.equal(conversation.messages[0].id, 21);
  assert.equal(conversation.lastMessage.contentPreview, "Sent");

  // The realtime echo of the same message must not add a second bubble.
  const echoed = applyIncomingMessageToList([conversation], confirmed, 9).conversations[0];
  assert.equal(echoed.messages.length, 1);
});

test("prepends an older page without dropping unsent drafts", () => {
  const loaded = [
    { id: 5, content: "Loaded" },
    { id: null, clientMessageId: "p1", pending: true },
    { id: null, clientMessageId: "p2", pending: true },
  ];
  const older = [
    { id: 3, content: "Older" },
    { id: 5, content: "Loaded" },
  ];

  const merged = prependMessageHistory(loaded, older);

  assert.deepEqual(
    merged.map((message) => message.id ?? message.clientMessageId),
    [3, 5, "p1", "p2"],
  );
});

test("clears unread state and applies presence to participants", () => {
  const conversation = createConversation({
    unreadCount: 4,
    otherParticipant: { id: 2, presence: { isOnline: false, lastActiveAt: null } },
    participants: [{ id: 2 }, { id: 3 }],
  });

  assert.equal(resetConversationUnread([conversation], 1)[0].unreadCount, 0);

  const [withPresence] = applyPresenceToList([conversation], {
    userId: 2,
    isOnline: true,
    lastActiveAt: "2026-01-02T00:00:00.000Z",
  });

  assert.equal(withPresence.otherParticipant.presence.isOnline, true);
  assert.equal(withPresence.participants[0].presence.isOnline, true);
  assert.equal(withPresence.participants[1].presence, undefined);
  assert.equal(applyPresenceToList([conversation], { userId: null })[0].unreadCount, 4);
});

test("applies delivered and read status cutoffs", () => {
  const messages = [
    { id: 10, senderId: 1, status: "SENT" },
    { id: 11, senderId: 1, status: "SENT" },
    { id: 12, senderId: 2, status: "SENT" },
  ];
  const delivered = applyMessageStatusToList(
    [createConversation({ messages })],
    { messageId: 11, status: "DELIVERED", deliveredAt: "2026-01-02" },
    1,
  );

  assert.deepEqual(
    delivered[0].messages.map((message) => message.status),
    ["DELIVERED", "DELIVERED", "SENT"],
  );

  const read = applyReadReceiptToList(delivered, {
    conversationId: 1,
    readerId: 2,
    lastReadMessageId: 11,
    readAt: "2026-01-03",
    currentUserId: 1,
  });

  assert.deepEqual(
    read[0].messages.map((message) => message.status),
    ["READ", "READ", "SENT"],
  );
});
