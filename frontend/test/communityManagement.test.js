import test from "node:test";
import assert from "node:assert/strict";
import {
  buildChannelRequest,
  buildCommunityRequest,
  canManageChannels,
  canManageCommunity,
  createCommunityForm,
  toggleTag,
} from "../src/features/community/communityManagement.js";
import communityManagementMessages from "../src/i18n/communityManagementMessages.js";

test("community management roles mirror backend access policy", () => {
  assert.equal(canManageCommunity("OWNER"), true);
  assert.equal(canManageCommunity("ADMIN"), true);
  assert.equal(canManageCommunity("MODERATOR"), false);
  assert.equal(canManageChannels("MODERATOR"), true);
  assert.equal(canManageChannels("MEMBER"), false);
});

test("create community request trims fields and includes the initial channel", () => {
  const form = {
    ...createCommunityForm(),
    name: "  Asia Desk  ",
    description: "  Market open notes  ",
    categorySlug: " technical ",
    tagSlugs: ["btc", "macro"],
    channelName: " general ",
    channelDescription: "  Daily discussion ",
    channelType: "TEXT",
  };

  assert.deepEqual(buildCommunityRequest(form, { creating: true }), {
    name: "Asia Desk",
    description: "Market open notes",
    categorySlug: "technical",
    visibility: "PUBLIC",
    tagSlugs: ["btc", "macro"],
    channels: [
      {
        name: "general",
        description: "Daily discussion",
        type: "TEXT",
        readOnly: false,
      },
    ],
  });
});

test("edit requests omit channels and empty optional text becomes null", () => {
  const request = buildCommunityRequest({
    ...createCommunityForm(),
    name: "Desk",
    description: " ",
    categorySlug: "",
  });

  assert.equal(request.description, null);
  assert.equal(request.categorySlug, null);
  assert.equal("channels" in request, false);
});

test("channel requests and tag toggles stay deterministic", () => {
  assert.deepEqual(
    buildChannelRequest({
      name: " signals ",
      description: "",
      type: "SIGNALS",
      readOnly: true,
    }),
    {
      name: "signals",
      description: null,
      type: "SIGNALS",
      readOnly: true,
    },
  );
  assert.deepEqual(toggleTag(["btc"], "macro"), ["btc", "macro"]);
  assert.deepEqual(toggleTag(["btc", "macro"], "btc"), ["macro"]);
});

test("community management catalogues expose matching keys", () => {
  assert.deepEqual(
    Object.keys(communityManagementMessages.en).sort(),
    Object.keys(communityManagementMessages.vi).sort(),
  );
});
