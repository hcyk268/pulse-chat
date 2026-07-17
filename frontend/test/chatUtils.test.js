import test from "node:test";
import assert from "node:assert/strict";
import {
  filterConversations,
  getPinnedPreview,
  isSameId,
} from "../src/utils/chat.js";
import {
  hasNoHtmlAngleBrackets,
  isOptionalHttpUrl,
  isValidUsername,
} from "../src/utils/validators.js";

test("compares identifiers without coercing null values", () => {
  assert.equal(isSameId(12, "12"), true);
  assert.equal(isSameId(null, null), false);
});

test("filters conversations across participant and preview fields", () => {
  const conversations = [
    {
      id: 1,
      otherParticipant: { displayName: "Alex Nguyen", username: "alex" },
      lastMessage: { contentPreview: "Roadmap" },
    },
    {
      id: 2,
      otherParticipant: { displayName: "Sam", email: "sam@example.com" },
      lastMessage: { contentPreview: "Hello" },
    },
  ];

  assert.deepEqual(filterConversations(conversations, "ROAD"), [conversations[0]]);
  assert.deepEqual(filterConversations(conversations, "sam@example"), [conversations[1]]);
  assert.equal(filterConversations(conversations, "   "), conversations);
});

test("returns safe pinned previews", () => {
  assert.equal(getPinnedPreview(null), "Pinned message");
  assert.equal(getPinnedPreview({ deletedAt: "2026-01-01" }), "Message deleted");
  assert.equal(getPinnedPreview({ content: "Important" }), "Important");
});

test("validates user-facing fields", () => {
  assert.equal(isValidUsername("newbie.dev-1"), true);
  assert.equal(isValidUsername("newbie dev"), false);
  assert.equal(hasNoHtmlAngleBrackets("<script>"), false);
  assert.equal(isOptionalHttpUrl("https://example.com/avatar.png"), true);
  assert.equal(isOptionalHttpUrl("javascript:alert(1)"), false);
});
