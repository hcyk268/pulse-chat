import { Activity, ArrowDown, BarChart3, Bot, LineChart, PieChart, Radio, UserPlus } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import PageShell from "../components/layout/PageShell";
import TraderLayout from "../components/layout/TraderLayout";
import MarketTable from "../components/shared/MarketTable";
import Alert from "../components/shared/Alert";
import MetricCard from "../components/shared/MetricCard";
import { WatchlistCta } from "../features/market/MarketExtras";
import PriceAlertsPanel from "../features/market/PriceAlertsPanel.jsx";
import UtilityBar from "../features/market/UtilityBar";
import { realtimeStatusKey } from "../features/market/realtimeStatus.js";
import {
  addWatchlistItem,
  getApiErrorMessage,
  getMarketOverview,
  getMarketTickers,
  getWatchlist,
  removeWatchlistItemBySymbol,
} from "../api/marketApi";
import { indexTickers, pickTicker, toMarketAsset } from "../domain/market/assets.js";
import { useRealtimeTopic } from "../hooks/useRealtimeTopic.js";
import { useTranslation } from "../i18n/useTranslation.js";
import { useAppDispatch, useAppSelector } from "../store/hooks";
import { selectIsAuthenticated } from "../store/slices/authSlice";
import {
  selectFavoriteSymbols,
  selectMarketFilter,
  setFavorites,
  toggleFavorite,
} from "../store/slices/marketSlice";
import { formatCurrency, formatRelativeTime } from "../utils/formatters";
import { useAuthGate } from "../features/auth/AuthGateContext.jsx";

export default function MarketPage() {
  const dispatch = useAppDispatch();
  const { openAuth } = useAuthGate();
  const { t } = useTranslation();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const activeFilter = useAppSelector(selectMarketFilter);
  const favorites = useAppSelector(selectFavoriteSymbols);
  const [overview, setOverview] = useState({ coins: [], trending: [] });
  const [tickersBySymbol, setTickersBySymbol] = useState({});
  const [syncedAt, setSyncedAt] = useState(null);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);

    Promise.allSettled([getMarketOverview(), getMarketTickers()])
      .then(([overviewResult, tickerResult]) => {
        if (!active) return;
        if (overviewResult.status === "rejected") throw overviewResult.reason;

        setOverview(overviewResult.value ?? { coins: [], trending: [] });
        setSyncedAt(new Date().toISOString());
        if (tickerResult.status === "fulfilled") {
          setTickersBySymbol(indexTickers(tickerResult.value));
        }
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
  }, [t]);

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
        // The watchlist is optional context on this page.
      });

    return () => {
      active = false;
    };
  }, [dispatch, isAuthenticated]);

  const handleTickerEvent = useCallback((event) => {
    if (event?.eventType !== "market.ticker.updated" || !event.data?.symbol) return;

    setTickersBySymbol((current) => ({
      ...current,
      [event.data.symbol.toUpperCase()]: event.data,
    }));
  }, []);

  const realtimeStatus = useRealtimeTopic("/topic/market/tickers", handleTickerEvent);

  const handleToggleFavorite = useCallback(
    async (symbol) => {
      const normalizedSymbol = symbol?.toUpperCase();
      if (!normalizedSymbol) return;

      const persistFavorite = async (wasFavorite) => {
        dispatch(toggleFavorite(normalizedSymbol));
        try {
          if (wasFavorite) await removeWatchlistItemBySymbol(normalizedSymbol);
          else await addWatchlistItem(normalizedSymbol);
        } catch (requestError) {
          dispatch(toggleFavorite(normalizedSymbol));
          setError(getApiErrorMessage(requestError, t, "errors.watchlistUpdate"));
        }
      };

      if (!isAuthenticated) {
        openAuth({
          kind: "watchlist.add",
          title: t("guest.market.favoriteTitle", { symbol: normalizedSymbol }),
          description: t("guest.market.favoriteDescription", { symbol: normalizedSymbol }),
          payload: { symbol: normalizedSymbol },
        }, () => persistFavorite(false));
        return;
      }

      await persistFavorite(favorites.includes(normalizedSymbol));
    },
    [dispatch, favorites, isAuthenticated, openAuth, t],
  );

  const trendingSymbols = useMemo(
    () => new Set(overview.trending.map((coin) => coin.symbol?.toUpperCase())),
    [overview.trending],
  );

  const assets = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return overview.coins
      .map((coin) => toMarketAsset(coin, pickTicker(tickersBySymbol, coin.symbol, coin.pairSymbol)))
      .filter((asset) => asset?.symbol)
      .filter((asset) => {
        const matchesQuery =
          !normalizedQuery ||
          asset.name?.toLowerCase().includes(normalizedQuery) ||
          asset.symbol?.toLowerCase().includes(normalizedQuery);
        if (!matchesQuery) return false;
        if (activeFilter === "top100") return Number(asset.rank) <= 100;
        if (activeFilter === "favorites") return favorites.includes(asset.symbol);
        if (activeFilter === "trending") return trendingSymbols.has(asset.symbol);
        return true;
      });
  }, [activeFilter, favorites, overview.coins, query, tickersBySymbol, trendingSymbols]);

  const stats = useMemo(() => {
    const trackedMarketCap = overview.coins.reduce(
      (sum, coin) => sum + Number(coin.marketCap || 0),
      0,
    );
    const volume24h = overview.coins.reduce((sum, coin) => sum + Number(coin.totalVolume || 0), 0);
    const bitcoin = overview.coins.find((coin) => coin.symbol?.toLowerCase() === "btc");
    const btcShare =
      trackedMarketCap > 0 ? (Number(bitcoin?.marketCap || 0) / trackedMarketCap) * 100 : 0;
    const overviewPairSymbols = overview.coins
      .map((coin) => coin.pairSymbol?.toUpperCase())
      .filter(Boolean);
    const livePairs = overviewPairSymbols.filter((pairSymbol) => tickersBySymbol[pairSymbol]).length;
    const isLive = realtimeStatus === "connected";

    // No progress value where there is nothing real to measure against.
    return [
      {
        label: t("market.metric.marketCap"),
        value: formatCurrency(trackedMarketCap, { compact: true }),
        change: t("market.metric.marketCapChange", { count: overview.coins.length }),
        trend: "flat",
        icon: LineChart,
      },
      {
        label: t("market.metric.volume"),
        value: formatCurrency(volume24h, { compact: true }),
        change: t("market.metric.volumeChange", { time: formatRelativeTime(syncedAt) }),
        trend: "flat",
        icon: BarChart3,
      },
      {
        label: t("market.metric.btcShare"),
        value: `${btcShare.toFixed(2)}%`,
        change: t("market.metric.btcShareChange"),
        trend: "flat",
        icon: PieChart,
        progress: btcShare,
        progressLabel: t("market.metric.btcShareCaption"),
      },
      {
        label: t("market.metric.streaming"),
        value: String(livePairs),
        change: t(realtimeStatusKey(realtimeStatus)),
        trend: isLive ? "up" : "flat",
        icon: Activity,
        progress: overview.coins.length
          ? Math.min(100, (livePairs / overview.coins.length) * 100)
          : 0,
        progressLabel: t("market.metric.streamingCaption", {
          live: livePairs,
          total: overview.coins.length,
        }),
      },
    ];
  }, [overview.coins, realtimeStatus, syncedAt, t, tickersBySymbol]);

  return (
    <TraderLayout active="market">
      <PageShell
        eyebrow={t("market.eyebrow")}
        title={t("market.title")}
        description={t("market.description")}
        action={
          <span className={`realtime-status realtime-status--${realtimeStatus}`}>
            <Activity size={16} />
            {t(realtimeStatusKey(realtimeStatus))}
          </span>
        }
      >
        {error ? <Alert onDismiss={() => setError("")}>{error}</Alert> : null}

        {!isAuthenticated ? (
          <section className="guest-market-hero">
            <div className="guest-market-hero__copy">
              <span className="guest-market-hero__eyebrow"><Radio size={15} /> {t("guest.market.eyebrow")}</span>
              <h2>{t("guest.market.title")}</h2>
              <p>{t("guest.market.description")}</p>
              <div className="guest-market-hero__actions">
                <a className="button button--ghost" href="#market-assets">
                  {t("guest.market.explore")} <ArrowDown size={16} />
                </a>
                <button
                  className="button button--primary"
                  type="button"
                  onClick={() => openAuth({
                    kind: "market.signup",
                    mode: "register",
                    title: t("guest.market.authTitle"),
                    description: t("guest.market.authDescription"),
                  })}
                >
                  <UserPlus size={17} /> {t("common.createAccount")}
                </button>
              </div>
            </div>
            <div className="guest-market-hero__signals" aria-label={t("guest.market.signals")}> 
              <div><Activity size={18} /><span>{t("guest.market.liveAssets")}</span><strong>{overview.coins.length}</strong></div>
              <div><BarChart3 size={18} /><span>{t("market.metric.volume")}</span><strong>{stats[1]?.value ?? "--"}</strong></div>
              <div><Bot size={18} /><span>{t("guest.market.aiDemos")}</span><strong>3</strong></div>
            </div>
          </section>
        ) : null}

        <section className="metric-grid metric-grid--four">
          {stats.map((stat) => (
            <MetricCard key={stat.label} stat={stat} />
          ))}
        </section>

        <div id="market-assets" className="market-assets-anchor">
          <UtilityBar query={query} onQueryChange={setQuery} resultCount={assets.length} />
        </div>

        <MarketTable
          assets={assets}
          emptyContent={
            !isAuthenticated && activeFilter === "favorites" ? (
              <div className="market-favorites-empty">
                <p>{t("market.favorites.guestEmpty")}</p>
                <button
                  className="button button--primary"
                  type="button"
                  onClick={() => openAuth({
                    kind: "watchlist.view",
                    title: t("guest.watchlist.authTitle"),
                    description: t("guest.watchlist.authDescription"),
                  })}
                >
                  {t("common.signIn")}
                </button>
              </div>
            ) : null
          }
          loading={loading}
          onToggleFavorite={handleToggleFavorite}
          resetToken={`${activeFilter}:${query}`}
        />

        <PriceAlertsPanel assets={overview.coins} />

        <WatchlistCta trending={overview.trending} />
      </PageShell>
    </TraderLayout>
  );
}

