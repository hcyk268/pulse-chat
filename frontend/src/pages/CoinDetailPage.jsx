import { Activity, ArrowLeft, BarChart3, LineChart, LogIn, PieChart, RefreshCw, Sparkles, Star, TrendingUp } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import TraderLayout from "../components/layout/TraderLayout";
import { getMarketInsight } from "../api/aiApi.js";
import CandlestickChart from "../components/market/CandlestickChart";
import Alert from "../components/shared/Alert";
import AssetAvatar from "../components/shared/AssetAvatar";
import MetricCard from "../components/shared/MetricCard";
import { classNames } from "../components/shared/utils";
import { CoinDiscussion, CoinNewsPanel } from "../features/market/MarketExtras";
import { realtimeStatusKey } from "../features/market/realtimeStatus.js";
import {
  addWatchlistItem,
  getMarketCandles,
  getApiErrorMessage,
  getCoinDetail,
  getMarketTicker,
  getWatchlist,
  removeWatchlistItemBySymbol,
} from "../api/marketApi";
import { useRealtimeTopic } from "../hooks/useRealtimeTopic.js";
import { useTranslation } from "../i18n/useTranslation.js";
import { useAppDispatch, useAppSelector } from "../store/hooks";
import { selectIsAuthenticated } from "../store/slices/authSlice";
import { selectFavoriteSymbols, setFavorites, toggleFavorite } from "../store/slices/marketSlice";
import {
  formatCompactNumber,
  formatCurrency,
  formatPercent,
  formatRelativeTime,
  marketTrend,
} from "../utils/formatters";

const intervals = [
  { label: "4H", value: "4h" },
  { label: "1D", value: "1d" },
  { label: "1W", value: "1w" },
];

const emptyCandles = { "4h": [], "1d": [], "1w": [] };

function rangePositionPercent({ price, low24h, high24h }) {
  const low = Number(low24h);
  const high = Number(high24h);
  const current = Number(price);

  if (![low, high, current].every(Number.isFinite) || high <= low) return null;

  return Math.min(100, Math.max(0, ((current - low) / (high - low)) * 100));
}

function upsertCandle(candles, candle) {
  const index = candles.findIndex((item) => item.openTime === candle.openTime);
  if (index === -1) return [...candles, candle].slice(-300);

  return candles.map((item, itemIndex) => (itemIndex === index ? candle : item));
}

function mergeCandleHistory(current, incoming) {
  const byOpenTime = new Map(
    [...current, ...incoming]
      .filter((candle) => candle?.openTime)
      .map((candle) => [candle.openTime, candle]),
  );

  return [...byOpenTime.values()]
    .sort((left, right) => new Date(left.openTime) - new Date(right.openTime))
    .slice(-300);
}

export default function CoinDetailPage() {
  const { symbol = "btc" } = useParams();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const favorites = useAppSelector(selectFavoriteSymbols);
  const [coin, setCoin] = useState(null);
  const [ticker, setTicker] = useState(null);
  const [activeInterval, setActiveInterval] = useState("4h");
  const [candlesByInterval, setCandlesByInterval] = useState(emptyCandles);
  const [loadedCandleIntervals, setLoadedCandleIntervals] = useState([]);
  const [loadingCandles, setLoadingCandles] = useState(false);
  const [candleError, setCandleError] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [aiInsight, setAiInsight] = useState(null);
  const [aiInsightError, setAiInsightError] = useState("");
  const [loadingAiInsight, setLoadingAiInsight] = useState(false);
  const [aiInsightRequest, setAiInsightRequest] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    setCoin(null);
    setCandlesByInterval(emptyCandles);
    setLoadedCandleIntervals([]);
    setCandleError("");

    getCoinDetail(symbol)
      .then((coinData) => {
        if (active) setCoin(coinData);
      })
      .catch((requestError) => {
        if (active) setError(getApiErrorMessage(requestError, t, "errors.marketData"));
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [symbol, t]);

  useEffect(() => {
    let active = true;
    setTicker(null);

    getMarketTicker(symbol)
      .then((tickerData) => {
        if (!active) return;
        setTicker(tickerData);

        const nextCandlesByInterval = tickerData?.candlesByInterval ?? {};
        const nextLoadedIntervals = Object.entries(nextCandlesByInterval)
          .filter(([, candles]) => Array.isArray(candles) && candles.length > 0)
          .map(([interval]) => interval);

        if (nextLoadedIntervals.length > 0) {
          setCandlesByInterval((current) => ({
            ...current,
            ...Object.fromEntries(
              nextLoadedIntervals.map((interval) => [
                interval,
                mergeCandleHistory(current[interval] ?? [], nextCandlesByInterval[interval]),
              ]),
            ),
          }));
          setLoadedCandleIntervals((current) => [
            ...new Set([...current, ...nextLoadedIntervals]),
          ]);
        }
      })
      .catch(() => {
        // Coin detail already contains a price snapshot, so ticker failure must
        // not block the page while the realtime stream reconnects.
      });

    return () => {
      active = false;
    };
  }, [symbol]);

  useEffect(() => {
    if (!isAuthenticated) return undefined;

    let active = true;

    getWatchlist()
      .then((items) => {
        if (!active) return;
        dispatch(
          setFavorites(
            (items ?? []).map((item) => item.asset?.symbol?.toUpperCase()).filter(Boolean),
          ),
        );
      })
      .catch(() => {
        // Watchlist state is optional on this page.
      });

    return () => {
      active = false;
    };
  }, [dispatch, isAuthenticated]);

  const pairSymbol = coin?.binancePair?.symbol?.toUpperCase() ?? null;

  useEffect(() => {
    const coinMatchesRoute = coin?.symbol?.toLowerCase() === symbol.toLowerCase();

    if (!pairSymbol || !coinMatchesRoute || loadedCandleIntervals.includes(activeInterval)) {
      setLoadingCandles(false);
      setCandleError("");
      return undefined;
    }

    let active = true;
    setLoadingCandles(true);
    setCandleError("");

    getMarketCandles(pairSymbol, activeInterval)
      .then((candles) => {
        if (!active) return;
        setCandlesByInterval((current) => ({
          ...current,
          [activeInterval]: mergeCandleHistory(current[activeInterval] ?? [], candles ?? []),
        }));
        setLoadedCandleIntervals((current) =>
          current.includes(activeInterval) ? current : [...current, activeInterval],
        );
      })
      .catch((requestError) => {
        if (active) {
          setCandleError(getApiErrorMessage(requestError, t, "errors.marketData"));
        }
      })
      .finally(() => {
        if (active) setLoadingCandles(false);
      });

    return () => {
      active = false;
    };
  }, [activeInterval, coin?.symbol, loadedCandleIntervals, pairSymbol, symbol, t]);

  const handleTickerEvent = useCallback((event) => {
    const nextTicker = event?.data;
    if (event?.eventType !== "market.ticker.updated" || !nextTicker?.symbol) return;
    if (pairSymbol && nextTicker.symbol.toUpperCase() !== pairSymbol) return;

    setTicker(nextTicker);
  }, [pairSymbol]);

  const handleCandleEvent = useCallback((event) => {
    const candle = event?.data;
    if (event?.eventType !== "market.candle.updated" || !candle?.intervalName) return;
    if (pairSymbol && candle.symbol?.toUpperCase() !== pairSymbol) return;

    setCandlesByInterval((current) => ({
      ...current,
      [candle.intervalName]: upsertCandle(current[candle.intervalName] ?? [], candle),
    }));
  }, [pairSymbol]);

  const tickerStatus = useRealtimeTopic(
    pairSymbol ? `/topic/market/tickers/${pairSymbol}` : null,
    handleTickerEvent,
  );
  useRealtimeTopic(
    pairSymbol ? `/topic/market/candles/${pairSymbol}/${activeInterval}` : null,
    handleCandleEvent,
  );

  const normalizedCoin = useMemo(() => {
    if (!coin) return null;

    return {
      ...coin,
      symbol: coin.symbol?.toUpperCase(),
      price: ticker?.price ?? coin.currentPriceUsd,
      change24h: ticker?.priceChangePercent ?? coin.priceChangePercentage24h,
      high24h: ticker?.high24h ?? coin.high24h,
      low24h: ticker?.low24h ?? coin.low24h,
      volume24h: ticker?.quoteVolume24h ?? coin.totalVolume,
    };
  }, [coin, ticker]);

  const normalizedSymbol = normalizedCoin?.symbol;

  useEffect(() => {
    if (!isAuthenticated || !normalizedSymbol) {
      setAiInsight(null);
      setAiInsightError("");
      setLoadingAiInsight(false);
      return undefined;
    }

    let active = true;
    setAiInsight(null);
    setAiInsightError("");
    setLoadingAiInsight(true);

    getMarketInsight({ symbol: normalizedSymbol })
      .then((result) => {
        if (active) setAiInsight(result);
      })
      .catch((requestError) => {
        if (active) setAiInsightError(getApiErrorMessage(requestError, t, "errors.ai"));
      })
      .finally(() => {
        if (active) setLoadingAiInsight(false);
      });

    return () => {
      active = false;
    };
  }, [aiInsightRequest, isAuthenticated, normalizedSymbol, t]);

  const isFavorite = Boolean(normalizedSymbol && favorites.includes(normalizedSymbol));

  const handleToggleFavorite = useCallback(async () => {
    if (!normalizedSymbol) return;

    if (!isAuthenticated) {
      navigate("/login", { state: { from: `/coins/${normalizedSymbol.toLowerCase()}` } });
      return;
    }

    dispatch(toggleFavorite(normalizedSymbol));

    try {
      if (isFavorite) {
        await removeWatchlistItemBySymbol(normalizedSymbol);
      } else {
        await addWatchlistItem(normalizedSymbol);
      }
    } catch (requestError) {
      dispatch(toggleFavorite(normalizedSymbol));
      setError(getApiErrorMessage(requestError, t, "errors.watchlistUpdate"));
    }
  }, [dispatch, isAuthenticated, isFavorite, navigate, normalizedSymbol, t]);

  if (loading) {
    return (
      <TraderLayout active="coin">
        <main className="page-shell" id="main">
          <div className="market-page-state">{t("common.loading")}</div>
        </main>
      </TraderLayout>
    );
  }

  if (!normalizedCoin) {
    return (
      <TraderLayout active="coin">
        <main className="page-shell" id="main">
          <Alert>{error || t("errors.coinData", { symbol: symbol.toUpperCase() })}</Alert>
          <Link className="button button--ghost" to="/market">
            <ArrowLeft size={16} /> {t("coin.backToMarket")}
          </Link>
        </main>
      </TraderLayout>
    );
  }

  const trend = marketTrend(normalizedCoin.change24h);
  const currentCandles = candlesByInterval[activeInterval] ?? [];
  const intervalLabel = intervals.find((entry) => entry.value === activeInterval)?.label ?? "";
  // Where the live price sits inside the 24h range, shared by the high/low cards.
  const rangePosition = rangePositionPercent(normalizedCoin);
  const rangeLabel =
    rangePosition === null
      ? null
      : t("coin.metric.rangeCaption", { percent: rangePosition.toFixed(0) });
  const rank = normalizedCoin.marketCapRank ?? null;
  const metricStats = [
    {
      label: t("coin.metric.marketCap"),
      value: formatCurrency(normalizedCoin.marketCap, { compact: true }),
      change: rank ? t("coin.metric.rank", { rank }) : t("common.unranked"),
      trend: "flat",
      icon: PieChart,
    },
    {
      label: t("coin.metric.volume"),
      value: formatCurrency(normalizedCoin.volume24h, { compact: true }),
      change: t("coin.metric.volumeChange"),
      trend: "flat",
      icon: BarChart3,
    },
    {
      label: t("coin.metric.high"),
      value: formatCurrency(normalizedCoin.high24h),
      change: formatPercent(normalizedCoin.change24h),
      trend,
      icon: TrendingUp,
      progress: rangePosition ?? undefined,
      progressLabel: rangeLabel,
    },
    {
      label: t("coin.metric.low"),
      value: formatCurrency(normalizedCoin.low24h),
      change: formatPercent(normalizedCoin.change24h),
      trend,
      icon: LineChart,
      progress: rangePosition ?? undefined,
      progressLabel: rangeLabel,
    },
  ];

  return (
    <TraderLayout active="coin">
      <main className="page-shell coin-page" id="main">
        {error ? <Alert onDismiss={() => setError("")}>{error}</Alert> : null}

        <section className="coin-hero">
          <div className="coin-hero__identity">
            <AssetAvatar asset={normalizedCoin} size="lg" />
            <div>
              <Link className="eyebrow" to="/market">
                <ArrowLeft size={13} /> {t("coin.backToMarket")}
              </Link>
              <h1>
                {normalizedCoin.name} <small>{normalizedCoin.symbol}</small>
              </h1>
              <p>
                {t("coin.meta", {
                  rank: rank ? `#${rank}` : t("common.unranked"),
                  pair: pairSymbol ?? t("coin.noPair"),
                  time: formatRelativeTime(ticker?.updatedAt ?? normalizedCoin.lastSyncedAt),
                })}
              </p>
            </div>
          </div>
          <div className="coin-hero__price">
            <span
              className={`realtime-status realtime-status--${pairSymbol ? tickerStatus : "idle"}`}
              title={pairSymbol ? undefined : t("market.status.noLivePair")}
            >
              <Activity size={15} />
              {pairSymbol ? t(realtimeStatusKey(tickerStatus)) : t("market.status.notStreamed")}
            </span>
            <strong className={ticker ? "live-price" : ""}>
              {formatCurrency(normalizedCoin.price)}
            </strong>
            <span className={classNames("change", `change--${trend}`)}>
              {formatPercent(normalizedCoin.change24h)}
            </span>
            <button
              className={classNames("button", isFavorite ? "button--ghost" : "button--primary")}
              type="button"
              aria-pressed={isFavorite}
              onClick={handleToggleFavorite}
            >
              <Star size={17} /> {isFavorite ? t("coin.inWatchlist") : t("coin.addToWatchlist")}
            </button>
          </div>
        </section>

        <section className="metric-grid metric-grid--four">
          {metricStats.map((stat) => (
            <MetricCard key={stat.label} stat={stat} />
          ))}
        </section>

        <section className="coin-layout">
          <div className="chart-panel">
            <div className="panel">
              <div className="panel-heading">
                <div>
                  <h2>{t("coin.chart.title", { symbol: normalizedCoin.symbol })}</h2>
                  <span className="chart-subtitle">
                    {pairSymbol
                      ? t("coin.chart.subtitle", { interval: intervalLabel })
                      : t("coin.chart.noPairSubtitle")}
                  </span>
                </div>
                <div className="segmented" aria-label={t("coin.chart.interval")}>
                  {intervals.map((interval) => (
                    <button
                      key={interval.value}
                      className={classNames(activeInterval === interval.value && "is-active")}
                      type="button"
                      aria-pressed={activeInterval === interval.value}
                      disabled={!pairSymbol}
                      onClick={() => setActiveInterval(interval.value)}
                    >
                      {interval.label}
                    </button>
                  ))}
                </div>
              </div>
              <CandlestickChart
                key={activeInterval}
                candles={currentCandles}
                emptyMessage={
                  pairSymbol ? candleError || undefined : t("coin.chart.noPairBody")
                }
                loading={Boolean(pairSymbol && loadingCandles)}
                symbol={normalizedCoin.symbol}
              />
            </div>

            <CoinDiscussion symbol={normalizedCoin.symbol} />
          </div>

          <aside className="coin-sidebar">
            <section className="panel stack-panel coin-ai-insight">
              <div className="panel-heading">
                <div>
                  <h3><Sparkles size={17} /> {t("coin.ai.title")}</h3>
                  <span>{t("coin.ai.subtitle", { symbol: normalizedCoin.symbol })}</span>
                </div>
                {isAuthenticated ? (
                  <button
                    className="icon-button"
                    type="button"
                    aria-label={t("coin.ai.refresh")}
                    title={t("coin.ai.refresh")}
                    disabled={loadingAiInsight}
                    onClick={() => setAiInsightRequest((current) => current + 1)}
                  >
                    <RefreshCw size={16} />
                  </button>
                ) : null}
              </div>
              {!isAuthenticated ? (
                <div className="coin-ai-insight__empty">
                  <p>{t("coin.ai.signInDescription")}</p>
                  <Link
                    className="button button--primary"
                    to="/login"
                    state={{ from: `/coins/${normalizedCoin.symbol.toLowerCase()}` }}
                  >
                    <LogIn size={16} /> {t("common.signIn")}
                  </Link>
                </div>
              ) : null}
              {loadingAiInsight ? (
                <div className="coin-ai-insight__loading" role="status">
                  <Sparkles size={18} /> {t("ai.pending.market")}
                </div>
              ) : null}
              {aiInsightError ? <Alert>{aiInsightError}</Alert> : null}
              {aiInsight ? (
                <div className="coin-ai-insight__result">
                  <p>{aiInsight.insight}</p>
                  {aiInsight.keyPoints?.length ? (
                    <div>
                      <strong>{t("ai.market.keyPoints")}</strong>
                      <ul>{aiInsight.keyPoints.map((item, index) => <li key={String(index)}>{item}</li>)}</ul>
                    </div>
                  ) : null}
                  {aiInsight.riskNotes?.length ? (
                    <div className="coin-ai-insight__risks">
                      <strong>{t("ai.market.riskNotes")}</strong>
                      <ul>{aiInsight.riskNotes.map((item, index) => <li key={String(index)}>{item}</li>)}</ul>
                    </div>
                  ) : null}
                </div>
              ) : null}
            </section>

            <section className="panel stack-panel">
              <div className="panel-heading">
                <h3>{t("coin.live.title")}</h3>
              </div>
              <div className="info-row">
                <span>{t("coin.live.bid")}</span>
                <strong>{formatCurrency(ticker?.bidPrice)}</strong>
              </div>
              <div className="info-row">
                <span>{t("coin.live.ask")}</span>
                <strong>{formatCurrency(ticker?.askPrice)}</strong>
              </div>
              <div className="info-row">
                <span>{t("coin.live.volume", { symbol: normalizedCoin.symbol })}</span>
                <strong>{formatCompactNumber(ticker?.volume24h)}</strong>
              </div>
              <div className="info-row">
                <span>{t("coin.live.priceChange")}</span>
                <strong>{formatCurrency(ticker?.priceChange)}</strong>
              </div>
            </section>

            <section className="panel stack-panel panel--subtle">
              <div className="panel-heading">
                <h3>{t("coin.supply.title")}</h3>
              </div>
              <div className="info-row">
                <span>{t("coin.supply.circulating")}</span>
                <strong>{formatCompactNumber(normalizedCoin.circulatingSupply)}</strong>
              </div>
              <div className="info-row">
                <span>{t("coin.supply.total")}</span>
                <strong>{formatCompactNumber(normalizedCoin.totalSupply)}</strong>
              </div>
              <div className="info-row">
                <span>{t("coin.supply.max")}</span>
                <strong>{formatCompactNumber(normalizedCoin.maxSupply)}</strong>
              </div>
              <div className="info-row">
                <span>{t("coin.supply.exchange")}</span>
                <strong>{normalizedCoin.binancePair?.exchange ?? "--"}</strong>
              </div>
            </section>

            <CoinNewsPanel />
          </aside>
        </section>
      </main>
    </TraderLayout>
  );
}

