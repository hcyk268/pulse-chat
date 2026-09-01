import test from "node:test";
import assert from "node:assert/strict";
import {
  indexTickers,
  pickTicker,
  sortByGain,
  toMarketAsset,
  toPairSymbol,
  toWatchlistAsset,
} from "../src/domain/market/assets.js";

test("prefers live ticker values over the stored snapshot", () => {
  const coin = {
    id: 7,
    symbol: "btc",
    name: "Bitcoin",
    currentPriceUsd: 60000,
    priceChangePercentage24h: 1.5,
    marketCapRank: 1,
    totalVolume: 100,
  };

  const snapshot = toMarketAsset(coin);
  assert.equal(snapshot.symbol, "BTC");
  assert.equal(snapshot.price, 60000);
  assert.equal(snapshot.isLive, false);

  const live = toMarketAsset(coin, { price: 61000, priceChangePercent: 2.5, quoteVolume24h: 250 });
  assert.equal(live.price, 61000);
  assert.equal(live.change24h, 2.5);
  assert.equal(live.volume, 250);
  assert.equal(live.isLive, true);
});

test("normalizes mock assets and missing input defensively", () => {
  const mock = toMarketAsset({ symbol: "ETH", name: "Ethereum", price: 3400, change24h: -1.2 });
  assert.equal(mock.price, 3400);
  assert.equal(mock.change24h, -1.2);
  assert.equal(toMarketAsset(null), null);
  assert.equal(toWatchlistAsset({ id: 3, asset: null }), null);
  assert.equal(toWatchlistAsset({ id: 3, asset: { symbol: "sol" } }).watchlistItemId, 3);
});

test("indexes tickers by pair symbol", () => {
  const tickers = indexTickers([{ symbol: "btcusdt" }, { symbol: null }, { symbol: "ETHUSDT" }]);

  assert.deepEqual(Object.keys(tickers), ["BTCUSDT", "ETHUSDT"]);
  assert.equal(toPairSymbol("btc"), "BTCUSDT");
  assert.equal(toPairSymbol(""), "");
  assert.ok(pickTicker(tickers, "eth"));
  assert.equal(pickTicker(tickers, "doge"), null);
});

test("ranks gainers and drops unusable changes", () => {
  const ranked = sortByGain([
    { symbol: "A", change24h: 1 },
    { symbol: "B", change24h: null },
    { symbol: "C", change24h: 9 },
    { symbol: "D", change24h: -4 },
  ]);

  assert.deepEqual(
    ranked.map((asset) => asset.symbol),
    ["C", "A", "D"],
  );
});
