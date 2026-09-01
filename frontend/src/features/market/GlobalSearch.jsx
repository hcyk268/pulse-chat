import { Search, X } from "lucide-react";
import { useEffect, useId, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getMarketOverview } from "../../api/marketApi";
import AssetAvatar from "../../components/shared/AssetAvatar";
import { classNames } from "../../components/shared/utils";
import { mockAssets } from "../../data/traderHubData";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useOutsideDismiss } from "../../hooks/useOutsideDismiss";
import { formatCurrency, formatPercent, marketTrend } from "../../utils/formatters";

const RESULT_LIMIT = 6;

// Cached for the browser session so reopening the panel is instant.
let cachedCoins = null;

function toSearchable(coin) {
  return {
    symbol: coin.symbol?.toUpperCase() ?? "",
    name: coin.name ?? "",
    imageUrl: coin.imageUrl,
    color: coin.color,
    rank: coin.marketCapRank ?? coin.rank,
    price: coin.currentPriceUsd ?? coin.price,
    change24h: coin.priceChangePercentage24h ?? coin.change24h,
  };
}

/**
 * `inline` renders the field permanently expanded, which is how the mobile menu
 * exposes search: the topbar icon is hidden below 900px.
 */
export default function GlobalSearch({ inline = false, onNavigate }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const listboxId = useId();
  const containerRef = useRef(null);
  const inputRef = useRef(null);
  const [expanded, setExpanded] = useState(false);
  const [query, setQuery] = useState("");
  const [coins, setCoins] = useState(() => cachedCoins ?? []);
  const [activeIndex, setActiveIndex] = useState(0);
  const debouncedQuery = useDebouncedValue(query, 200);
  const open = inline || expanded;

  useOutsideDismiss(containerRef, () => setExpanded(false), !inline && expanded);

  useEffect(() => {
    if (!open) return undefined;

    if (!inline) inputRef.current?.focus();
    if (cachedCoins) return undefined;

    let active = true;
    getMarketOverview()
      .then((overview) => {
        if (!active) return;
        cachedCoins = (overview?.coins ?? []).map(toSearchable);
        setCoins(cachedCoins);
      })
      .catch(() => {
        if (!active) return;
        // The market API is optional here: the panel still works on mock assets.
        setCoins(mockAssets.map(toSearchable));
      });

    return () => {
      active = false;
    };
  }, [inline, open]);

  const results = useMemo(() => {
    const normalizedQuery = debouncedQuery.trim().toLowerCase();
    if (!normalizedQuery) return coins.slice(0, RESULT_LIMIT);

    return coins
      .filter(
        (coin) =>
          coin.symbol.toLowerCase().includes(normalizedQuery) ||
          coin.name.toLowerCase().includes(normalizedQuery),
      )
      .slice(0, RESULT_LIMIT);
  }, [coins, debouncedQuery]);

  useEffect(() => {
    setActiveIndex(results.length > 0 ? 0 : -1);
  }, [results]);

  function openCoin(symbol) {
    setExpanded(false);
    setQuery("");
    onNavigate?.();
    navigate(`/coins/${symbol.toLowerCase()}`);
  }

  function handleSubmit(event) {
    event.preventDefault();
    const selected = results[activeIndex] ?? results[0];
    if (selected) openCoin(selected.symbol);
  }

  function handleSearchKeyDown(event) {
    if (event.key === "Escape" && !inline) {
      event.preventDefault();
      setExpanded(false);
      return;
    }
    if (results.length === 0) return;

    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActiveIndex((current) => (current + 1) % results.length);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActiveIndex((current) => (current <= 0 ? results.length - 1 : current - 1));
    } else if (event.key === "Enter") {
      event.preventDefault();
      openCoin((results[activeIndex] ?? results[0]).symbol);
    }
  }

  return (
    <div
      className={classNames("global-search", inline && "global-search--inline")}
      ref={containerRef}
    >
      {!inline ? (
        <button
          className="icon-button hide-sm"
          type="button"
          aria-label={t("search.aria")}
          aria-expanded={expanded}
          onClick={() => setExpanded((current) => !current)}
        >
          {expanded ? <X size={20} /> : <Search size={20} />}
        </button>
      ) : null}

      {open ? (
        <div className="global-search__panel">
          <form className="global-search__field" onSubmit={handleSubmit}>
            <Search size={17} />
            <input
              ref={inputRef}
              role="combobox"
              aria-autocomplete="list"
              aria-controls={listboxId}
              aria-expanded={open}
              aria-activedescendant={
                results[activeIndex] ? `${listboxId}-${results[activeIndex].symbol}` : undefined
              }
              type="search"
              placeholder={t("search.placeholder")}
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              onKeyDown={handleSearchKeyDown}
              aria-label={t("search.aria")}
            />
          </form>
          <div className="global-search__results" id={listboxId} role="listbox">
            {results.map((coin, index) => (
              <button
                key={coin.symbol}
                id={`${listboxId}-${coin.symbol}`}
                role="option"
                type="button"
                aria-selected={index === activeIndex}
                className={index === activeIndex ? "is-active" : ""}
                onMouseEnter={() => setActiveIndex(index)}
                onClick={() => openCoin(coin.symbol)}
              >
                <AssetAvatar asset={coin} />
                <span>
                  <strong>{coin.name}</strong>
                  <small>{coin.symbol}</small>
                </span>
                <span className="global-search__price">
                  <strong>{formatCurrency(coin.price)}</strong>
                  <small className={classNames("change", `change--${marketTrend(coin.change24h)}`)}>
                    {formatPercent(coin.change24h)}
                  </small>
                </span>
              </button>
            ))}
            {results.length === 0 ? (
              <p className="global-search__empty">
                {coins.length === 0 ? t("search.loading") : t("search.empty")}
              </p>
            ) : null}
          </div>
        </div>
      ) : null}
    </div>
  );
}
