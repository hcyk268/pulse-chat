import httpClient, { getApiErrorMessage, unwrap } from "./httpClient.js";

export { getApiErrorMessage };

export async function getMarketOverview() {
  return unwrap(await httpClient.get("/api/v1/market"));
}

export async function getMarketTickers() {
  return unwrap(await httpClient.get("/api/v1/market/tickers"));
}

export async function getCoinDetail(symbol) {
  return unwrap(await httpClient.get(`/api/v1/market/${encodeURIComponent(symbol)}`));
}

export async function getMarketTicker(symbol) {
  return unwrap(await httpClient.get(`/api/v1/market/tickers/${encodeURIComponent(symbol)}`));
}

export async function getMarketCandles(symbol, interval) {
  return unwrap(
    await httpClient.get(`/api/v1/market/tickers/${encodeURIComponent(symbol)}/candles`, {
      params: { interval },
    }),
  );
}

export async function getWatchlist() {
  return unwrap(await httpClient.get("/api/v1/market/watchlist"));
}

export async function getWatchlistItem(itemId) {
  return unwrap(await httpClient.get(`/api/v1/market/watchlist/${encodeURIComponent(itemId)}`));
}

export async function addWatchlistItem(symbol) {
  return unwrap(await httpClient.post("/api/v1/market/watchlist", { symbol }));
}

export async function updateWatchlistItem(itemId, request) {
  return unwrap(
    await httpClient.patch(`/api/v1/market/watchlist/${encodeURIComponent(itemId)}`, request),
  );
}

export async function removeWatchlistItem(itemId) {
  return unwrap(await httpClient.delete(`/api/v1/market/watchlist/${encodeURIComponent(itemId)}`));
}

export async function removeWatchlistItemBySymbol(symbol) {
  return unwrap(
    await httpClient.delete(`/api/v1/market/watchlist/symbols/${encodeURIComponent(symbol)}`),
  );
}
export async function createPriceAlert(payload) {
  return unwrap(await httpClient.post("/api/v1/market/price-alerts", payload));
}

export async function getPriceAlerts() {
  return unwrap(await httpClient.get("/api/v1/market/price-alerts"));
}

export async function getPriceAlert(alertId) {
  return unwrap(
    await httpClient.get(`/api/v1/market/price-alerts/${encodeURIComponent(alertId)}`),
  );
}

export async function updatePriceAlert(alertId, payload) {
  return unwrap(
    await httpClient.patch(
      `/api/v1/market/price-alerts/${encodeURIComponent(alertId)}`,
      payload,
    ),
  );
}

export async function deletePriceAlert(alertId) {
  return unwrap(
    await httpClient.delete(`/api/v1/market/price-alerts/${encodeURIComponent(alertId)}`),
  );
}
