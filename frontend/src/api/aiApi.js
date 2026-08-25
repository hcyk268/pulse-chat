import httpClient, { getApiErrorMessage, unwrap } from "./httpClient.js";

export { getApiErrorMessage };

function compactPayload(payload) {
  return Object.fromEntries(
    Object.entries(payload ?? {}).filter(([, value]) => value !== "" && value !== null && value !== undefined),
  );
}

export async function askSmartAssistant(request) {
  return unwrap(await httpClient.post("/api/v1/ai/assistant", compactPayload(request)));
}

export async function getMarketInsight(request = {}) {
  return unwrap(await httpClient.post("/api/v1/ai/market/insight", compactPayload(request)));
}

export async function summarizeConversation(conversationId, request = {}) {
  return unwrap(
    await httpClient.post(
      "/api/v1/ai/conversations/" + encodeURIComponent(conversationId) + "/summary",
      compactPayload(request),
    ),
  );
}

export async function moderateCommunityContent(request) {
  return unwrap(await httpClient.post("/api/v1/ai/community/moderation", compactPayload(request)));
}
