import { BellRing, LockKeyhole, Pencil, RefreshCcw, Save, Star, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import PageShell from "../components/layout/PageShell";
import TraderLayout from "../components/layout/TraderLayout";
import Alert from "../components/shared/Alert";
import AssetAvatar from "../components/shared/AssetAvatar";
import Modal from "../components/shared/Modal";
import MarketTable from "../components/shared/MarketTable";
import { WatchlistInsights } from "../features/market/MarketExtras";
import {
  getApiErrorMessage,
  getMarketTickers,
  getWatchlist,
  getWatchlistItem,
  updateWatchlistItem,
  removeWatchlistItemBySymbol,
} from "../api/marketApi";
import { indexTickers, pickTicker, toWatchlistAsset } from "../domain/market/assets.js";
import { useRealtimeTopic } from "../hooks/useRealtimeTopic.js";
import { useTranslation } from "../i18n/useTranslation.js";
import { formatCurrency, formatPercent } from "../utils/formatters";
import { useAppDispatch, useAppSelector } from "../store/hooks";
import { selectIsAuthenticated } from "../store/slices/authSlice.js";
import { useAuthGate } from "../features/auth/AuthGateContext.jsx";
import { setFavorites, toggleFavorite } from "../store/slices/marketSlice";

export default function WatchlistPage() {
  const { t } = useTranslation();
  const dispatch = useAppDispatch();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const { openAuth } = useAuthGate();
  const [items, setItems] = useState([]);
  const [tickersBySymbol, setTickersBySymbol] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [detailItem, setDetailItem] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [editingItem, setEditingItem] = useState(false);
  const [editSymbol, setEditSymbol] = useState("");
  const [updatingItem, setUpdatingItem] = useState(false);

  const loadWatchlist = useCallback(() => {
    if (!isAuthenticated) {
      setItems([]);
      setLoading(false);
      return;
    }

    setLoading(true);
    setError("");

    getWatchlist()
      .then((watchlist) => {
        const nextItems = watchlist ?? [];
        setItems(nextItems);
        dispatch(
          setFavorites(
            nextItems.map((item) => item.asset?.symbol?.toUpperCase()).filter(Boolean),
          ),
        );
      })
      .catch((requestError) => setError(getApiErrorMessage(requestError, t, "errors.watchlistLoad")))
      .finally(() => setLoading(false));
  }, [dispatch, isAuthenticated, t]);

  useEffect(() => {
    loadWatchlist();
  }, [loadWatchlist]);

  useEffect(() => {
    let active = true;

    getMarketTickers()
      .then((tickers) => {
        if (active) setTickersBySymbol(indexTickers(tickers));
      })
      .catch(() => {
        // Live pricing is a bonus here; the stored snapshot still renders.
      });

    return () => {
      active = false;
    };
  }, []);

  const handleSelectAsset = useCallback(
    async (asset) => {
      if (!asset?.watchlistItemId) return;
      setDetailLoading(true);
      setError("");
      try {
        const detail = await getWatchlistItem(asset.watchlistItemId);
        setDetailItem(detail);
        setEditSymbol(detail?.asset?.symbol?.toUpperCase() ?? "");
        setEditingItem(false);
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, t, "errors.watchlistDetail"));
      } finally {
        setDetailLoading(false);
      }
    },
    [t],
  );
  const handleUpdateItem = useCallback(
    async (event) => {
      event.preventDefault();
      const itemId = detailItem?.id;
      const symbol = editSymbol.trim().toUpperCase();
      if (!itemId || !symbol || updatingItem) return;

      setUpdatingItem(true);
      setError("");
      try {
        const updated = await updateWatchlistItem(itemId, { symbol });
        const nextItem = updated ?? detailItem;
        setDetailItem(nextItem);
        setItems((current) => {
          const nextItems = current.map((item) => (String(item.id) === String(itemId) ? nextItem : item));
          dispatch(
            setFavorites(
              nextItems.map((item) => item.asset?.symbol?.toUpperCase()).filter(Boolean),
            ),
          );
          return nextItems;
        });
        setEditSymbol(nextItem?.asset?.symbol?.toUpperCase() ?? symbol);
        setEditingItem(false);
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, t, "errors.watchlistUpdate"));
      } finally {
        setUpdatingItem(false);
      }
    },
    [detailItem, dispatch, editSymbol, t, updatingItem],
  );
  const handleTickerEvent = useCallback((event) => {
    if (event?.eventType !== "market.ticker.updated" || !event.data?.symbol) return;

    setTickersBySymbol((current) => ({
      ...current,
      [event.data.symbol.toUpperCase()]: event.data,
    }));
  }, []);

  useRealtimeTopic("/topic/market/tickers", handleTickerEvent);

  const assets = useMemo(
    () =>
      items
        .map((item) => toWatchlistAsset(item, pickTicker(tickersBySymbol, item.asset?.symbol)))
        .filter((asset) => asset?.symbol),
    [items, tickersBySymbol],
  );

  const handleToggleFavorite = useCallback(
    async (symbol) => {
      const normalizedSymbol = symbol?.toUpperCase();
      if (!normalizedSymbol) return;

      const previousItems = items;
      setItems((current) =>
        current.filter((item) => item.asset?.symbol?.toUpperCase() !== normalizedSymbol),
      );
      dispatch(toggleFavorite(normalizedSymbol));

      try {
        await removeWatchlistItemBySymbol(normalizedSymbol);
      } catch (requestError) {
        setItems(previousItems);
        dispatch(toggleFavorite(normalizedSymbol));
        setError(getApiErrorMessage(requestError, t, "errors.watchlistUpdate"));
      }
    },
    [dispatch, items, t],
  );

  return (
    <TraderLayout active="watchlist">
      <PageShell
        eyebrow={t("watchlist.eyebrow")}
        title={t("watchlist.title")}
        description={t("watchlist.description")}
        action={
          <div className="page-actions">
            {isAuthenticated ? (
              <button className="button button--ghost" type="button" onClick={loadWatchlist}>
                <RefreshCcw size={17} /> {t("common.refresh")}
              </button>
            ) : null}
            <span className={isAuthenticated ? "meta-chip" : "guest-mode-badge"}>
              {isAuthenticated ? <Star size={15} /> : <LockKeyhole size={15} />}
              {isAuthenticated ? t("watchlist.saved", { count: assets.length }) : t("guest.preview")}
            </span>
          </div>
        }
      >
        {error ? (
          <Alert onDismiss={() => setError("")} onRetry={loadWatchlist}>
            {error}
          </Alert>
        ) : null}

        {!isAuthenticated ? (
          <section className="panel watchlist-guest-panel">
            <div className="watchlist-guest-panel__icon"><Star size={30} /></div>
            <div>
              <span className="eyebrow">{t("guest.watchlist.eyebrow")}</span>
              <h2>{t("guest.watchlist.title")}</h2>
              <p>{t("guest.watchlist.description")}</p>
              <ul>
                <li><Star size={16} /> {t("guest.watchlist.benefitSave")}</li>
                <li><BellRing size={16} /> {t("guest.watchlist.benefitAlerts")}</li>
              </ul>
              <div className="watchlist-guest-panel__actions">
                <button
                  className="button button--primary"
                  type="button"
                  onClick={() => openAuth({
                    kind: "watchlist",
                    mode: "register",
                    title: t("guest.watchlist.authTitle"),
                    description: t("guest.watchlist.authDescription"),
                  })}
                >
                  {t("common.createAccount")}
                </button>
                <Link className="button button--ghost" to="/market">{t("watchlist.empty.action")}</Link>
              </div>
            </div>
          </section>
        ) : !loading && assets.length === 0 ? (
          <section className="panel market-empty-panel">
            <h2>{t("watchlist.empty.title")}</h2>
            <p>{t("watchlist.empty.body")}</p>
            <Link className="button button--primary" to="/market">
              {t("watchlist.empty.action")}
            </Link>
          </section>
        ) : (
          <MarketTable
            compact
            assets={assets}
            loading={loading}
            onToggleFavorite={handleToggleFavorite}
            onSelectAsset={handleSelectAsset}
          />
        )}

        {isAuthenticated ? <WatchlistInsights /> : null}
      </PageShell>

      {detailLoading || detailItem ? (
        <Modal
          title={detailItem?.asset?.name ?? t("watchlist.detail.title")}
          description={detailItem?.asset?.symbol ?? t("common.loading")}
          onClose={() => {
            if (!detailLoading) setDetailItem(null);
          }}
        >
          {detailLoading && !detailItem ? <p className="workspace-empty">{t("common.loading")}</p> : null}
          {detailItem?.asset ? (
            <div className="watchlist-detail">
              <div className="watchlist-detail__identity">
                <AssetAvatar asset={{ ...detailItem.asset, symbol: detailItem.asset.symbol?.toUpperCase() }} />
                <div>
                  <strong>{detailItem.asset.name}</strong>
                  <span>{detailItem.asset.symbol?.toUpperCase()}</span>
                </div>
                {!editingItem ? (
                  <button
                    className="icon-button"
                    type="button"
                    title={t("watchlist.edit")}
                    aria-label={t("watchlist.edit")}
                    onClick={() => setEditingItem(true)}
                  >
                    <Pencil size={15} />
                  </button>
                ) : null}
              </div>
              {editingItem ? (
                <form className="watchlist-detail__edit" onSubmit={handleUpdateItem}>
                  <label>
                    {t("watchlist.symbol")}
                    <input
                      value={editSymbol}
                      maxLength={20}
                      required
                      autoFocus
                      onChange={(event) => setEditSymbol(event.target.value)}
                    />
                  </label>
                  <div className="dialog-form__actions">
                    <button className="button button--ghost" type="button" onClick={() => setEditingItem(false)}>
                      <X size={15} /> {t("common.cancel")}
                    </button>
                    <button className="button button--primary" type="submit" disabled={!editSymbol.trim() || updatingItem}>
                      <Save size={15} /> {updatingItem ? t("common.saving") : t("common.save")}
                    </button>
                  </div>
                </form>
              ) : null}
              <dl>
                <div><dt>{t("market.table.price")}</dt><dd>{formatCurrency(detailItem.asset.currentPriceUsd)}</dd></div>
                <div><dt>{t("market.table.change")}</dt><dd>{formatPercent(detailItem.asset.priceChangePercentage24h)}</dd></div>
                <div><dt>{t("market.table.range")}</dt><dd>{formatCurrency(detailItem.asset.low24h)} - {formatCurrency(detailItem.asset.high24h)}</dd></div>
                <div><dt>{t("market.table.marketCap")}</dt><dd>{formatCurrency(detailItem.asset.marketCap, { compact: true })}</dd></div>
                <div><dt>{t("market.table.volume")}</dt><dd>{formatCurrency(detailItem.asset.totalVolume, { compact: true })}</dd></div>
              </dl>
            </div>
          ) : null}
        </Modal>
      ) : null}
    </TraderLayout>
  );
}

