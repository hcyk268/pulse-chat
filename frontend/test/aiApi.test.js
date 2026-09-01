import assert from "node:assert/strict";
import test from "node:test";
import httpClient from "../src/api/httpClient.js";
import {
  askSmartAssistant,
  getMarketInsight,
  moderateCommunityContent,
  summarizeConversation,
} from "../src/api/aiApi.js";

function apiResponse(config, data) {
  return {
    config,
    data: { data },
    headers: {},
    status: 200,
    statusText: "OK",
  };
}

test("aiApi sends compact backend DTOs", async () => {
  const originalAdapter = httpClient.defaults.adapter;
  const requests = [];

  httpClient.defaults.adapter = async (config) => {
    requests.push({ method: config.method, url: config.url, data: JSON.parse(config.data ?? "{}") });
    return apiResponse(config, { ok: true });
  };

  try {
    await askSmartAssistant({ question: "What changed?", symbol: "", communitySlug: "daily" });
    await getMarketInsight({ symbol: "BTC" });
    await summarizeConversation(42, { limit: 20 });
    await moderateCommunityContent({ title: "", content: "Risky content", communitySlug: "daily" });
  } finally {
    httpClient.defaults.adapter = originalAdapter;
  }

  assert.deepEqual(requests, [
    {
      method: "post",
      url: "/api/v1/ai/assistant",
      data: { question: "What changed?", communitySlug: "daily" },
    },
    { method: "post", url: "/api/v1/ai/market/insight", data: { symbol: "BTC" } },
    { method: "post", url: "/api/v1/ai/conversations/42/summary", data: { limit: 20 } },
    {
      method: "post",
      url: "/api/v1/ai/community/moderation",
      data: { content: "Risky content", communitySlug: "daily" },
    },
  ]);
});
