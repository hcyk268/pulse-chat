/**
 * Shapes the different market payloads (CoinGecko snapshot, Binance ticker,
 * watchlist entry, local mock asset) into the single asset object the UI renders.
 */
export function toMarketAsset(coin, ticker = null) {
  if (!coin) return null;

  const symbol = coin.symbol?.toUpperCase() ?? "";
  const pairSymbol = normalizePairSymbol(coin.pairSymbol ?? coin.binancePair?.symbol ?? toPairSymbol(symbol));

  return {
    id: coin.id ?? symbol,
    symbol,
    pairSymbol,
    name: coin.name ?? symbol,
    imageUrl: coin.imageUrl ?? null,
    color: coin.color,
    rank: coin.marketCapRank ?? coin.rank ?? null,
    price: ticker?.price ?? coin.currentPriceUsd ?? coin.price ?? null,
    change24h:
      ticker?.priceChangePercent ?? coin.priceChangePercentage24h ?? coin.change24h ?? null,
    high24h: ticker?.high24h ?? coin.high24h ?? null,
    low24h: ticker?.low24h ?? coin.low24h ?? null,
    marketCap: coin.marketCap ?? null,
    volume: ticker?.quoteVolume24h ?? coin.totalVolume ?? coin.volume ?? null,
    isLive: Boolean(ticker),
  };
}

export function toWatchlistAsset(item, ticker = null) {
  const asset = toMarketAsset(item?.asset, ticker);
  if (!asset) return null;

  return { ...asset, watchlistItemId: item.id };
}

/** Binance pairs are quoted in USDT; overview payloads prefer the server pairSymbol when present. */
export function toPairSymbol(symbol) {
  return symbol ? `${symbol.toUpperCase()}USDT` : "";
}

export function normalizePairSymbol(symbol) {
  return symbol ? symbol.toUpperCase() : "";
}

export function indexTickers(tickers) {
  return Object.fromEntries(
    (tickers ?? [])
      .filter((ticker) => ticker?.symbol)
      .map((ticker) => [normalizePairSymbol(ticker.symbol), ticker]),
  );
}

export function pickTicker(tickersBySymbol, symbol, pairSymbol = null) {
  const normalizedPairSymbol = normalizePairSymbol(pairSymbol);
  if (normalizedPairSymbol && tickersBySymbol[normalizedPairSymbol]) {
    return tickersBySymbol[normalizedPairSymbol];
  }

  return tickersBySymbol[toPairSymbol(symbol)] ?? null;
}

export function sortByGain(assets) {
  return assets
    .filter((asset) => {
      const change = asset?.change24h;
      if (change === null || change === undefined || change === "") return false;

      return Number.isFinite(Number(change));
    })
    .sort((left, right) => Number(right.change24h) - Number(left.change24h));
}
