import assert from "node:assert/strict";
import test from "node:test";
import httpClient from "../src/api/httpClient.js";
import { updateWatchlistItem } from "../src/api/marketApi.js";

test("updateWatchlistItem sends the DTO directly instead of nesting symbol", async () => {
  const originalAdapter = httpClient.defaults.adapter;
  let requestConfig;

  httpClient.defaults.adapter = async (config) => {
    requestConfig = config;
    return {
      config,
      data: { data: { id: 7, symbol: "ETH" } },
      headers: {},
      status: 200,
      statusText: "OK",
    };
  };

  try {
    const result = await updateWatchlistItem(7, { symbol: "ETH" });

    assert.equal(requestConfig.method, "patch");
    assert.equal(requestConfig.url, "/api/v1/market/watchlist/7");
    assert.deepEqual(JSON.parse(requestConfig.data), { symbol: "ETH" });
    assert.deepEqual(result, { id: 7, symbol: "ETH" });
  } finally {
    httpClient.defaults.adapter = originalAdapter;
  }
});
