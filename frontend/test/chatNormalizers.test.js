import test from "node:test";
import assert from "node:assert/strict";
import {
  dedupeById,
  getMessagePreview,
  mergeContacts,
  normalizeConversation,
  normalizeMessage,
  toCurrentUser,
} from "../src/domain/chat/normalizers.js";

test("normalizes a direct conversation without losing existing messages", () => {
  const messages = [{ id: 99, content: "Existing" }];
  const conversation = normalizeConversation(
    {
      id: 1,
      type: "DIRECT",
      peer: {
        id: 2,
        username: "alex",
        displayName: "Alex",
      },
      unreadCount: 3,
      createdAt: "2026-01-01T00:00:00.000Z",
    },
    messages,
  );

  assert.equal(conversation.otherParticipantId, 2);
  assert.equal(conversation.title, "Alex");
  assert.equal(conversation.unreadCount, 3);
  assert.equal(conversation.messages, messages);
});

test("normalizes group invitation state", () => {
  const conversation = normalizeConversation({
    id: 7,
    type: "GROUP",
    name: "Core team",
    currentUserStatus: "PENDING",
    participants: [],
  });

  assert.equal(conversation.title, "Core team");
  assert.equal(conversation.otherParticipantId, null);
  assert.equal(conversation.isPendingInvitation, true);
});

test("normalizes users and messages defensively", () => {
  assert.equal(toCurrentUser({ username: "newbie" }).displayName, "newbie");
  assert.deepEqual(normalizeMessage({ id: 1, senderId: 2 }).attachments, []);
  assert.equal(getMessagePreview({ attachments: [{ fileName: "guide.pdf" }] }), "guide.pdf");
  assert.equal(getMessagePreview({ deletedAt: "2026-01-01" }), "Message deleted");
});

test("deduplicates entities and merges newer contact fields", () => {
  assert.deepEqual(dedupeById([{ id: 1, value: "old" }, { id: "1", value: "new" }]), [
    { id: "1", value: "new" },
  ]);

  assert.deepEqual(
    mergeContacts(
      [{ id: 1, displayName: "Old", username: "alex" }],
      [{ id: "1", displayName: "New" }],
    ),
    [{ id: "1", displayName: "New", username: "alex" }],
  );
});
