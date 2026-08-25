import { ChevronDown, ChevronLeft, ChevronRight, ChevronUp, Info, Star } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import AssetAvatar from "./AssetAvatar";
import { classNames } from "./utils";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useAppSelector } from "../../store/hooks";
import { selectFavoriteSymbols } from "../../store/slices/marketSlice";
import { formatCurrency, formatPercent, marketTrend } from "../../utils/formatters";

const PAGE_SIZE = 20;

const sortAccessors = {
  rank: (asset) => Number(asset.rank ?? Number.MAX_SAFE_INTEGER),
  name: (asset) => (asset.name ?? "").toLowerCase(),
  price: (asset) => Number(asset.price ?? 0),
  change24h: (asset) => Number(asset.change24h ?? 0),
  marketCap: (asset) => Number(asset.marketCap ?? 0),
  volume: (asset) => Number(asset.volume ?? 0),
};

function sortAssets(assets, { key, direction }) {
  const accessor = sortAccessors[key];
  if (!accessor) return assets;

  const factor = direction === "asc" ? 1 : -1;

  return [...assets].sort((left, right) => {
    const leftValue = accessor(left);
    const rightValue = accessor(right);

    if (leftValue < rightValue) return -1 * factor;
    if (leftValue > rightValue) return factor;
    return 0;
  });
}

function PriceRange({ asset }) {
  const low = Number(asset.low24h);
  const high = Number(asset.high24h);
  const price = Number(asset.price);

  if (!Number.isFinite(low) || !Number.isFinite(high) || !Number.isFinite(price)) {
    return <span className="price-range price-range--empty">--</span>;
  }

  const position =
    high > low ? Math.min(100, Math.max(0, ((price - low) / (high - low)) * 100)) : 50;

  return (
    <div className="price-range" title={`${formatCurrency(low)} - ${formatCurrency(high)}`}>
      <span style={{ left: `${position}%` }} />
    </div>
  );
}

function SortableHeader({ label, sortKey, sort, onSort }) {
  const isActive = sort.key === sortKey;

  return (
    <th aria-sort={isActive ? (sort.direction === "asc" ? "ascending" : "descending") : "none"}>
      <button
        className={classNames("market-table__sort", isActive && "is-active")}
        type="button"
        onClick={() => onSort(sortKey)}
      >
        {label}
        {isActive ? (
          sort.direction === "asc" ? (
            <ChevronUp size={14} />
          ) : (
            <ChevronDown size={14} />
          )
        ) : null}
      </button>
    </th>
  );
}

export default function MarketTable({
  assets = [],
  compact = false,
  emptyContent = null,
  loading = false,
  onToggleFavorite,
  onSelectAsset,
  resetToken = "",
}) {
  const { t } = useTranslation();
  const favorites = useAppSelector(selectFavoriteSymbols);
  const [page, setPage] = useState(1);
  const [sort, setSort] = useState({ key: "rank", direction: "asc" });

  const sortedAssets = useMemo(() => sortAssets(assets, sort), [assets, sort]);
  const pageCount = Math.max(1, Math.ceil(sortedAssets.length / PAGE_SIZE));
  const visibleAssets = useMemo(
    () => sortedAssets.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE),
    [page, sortedAssets],
  );

  useEffect(() => setPage(1), [resetToken]);
  useEffect(() => setPage((current) => Math.min(current, pageCount)), [pageCount]);

  function handleSort(key) {
    setSort((current) =>
      current.key === key
        ? { key, direction: current.direction === "asc" ? "desc" : "asc" }
        : { key, direction: key === "rank" || key === "name" ? "asc" : "desc" },
    );
    setPage(1);
  }

  return (
    <section className="panel market-table-panel">
      <div className="table-scroll">
        <table className="market-table">
          <thead>
            <tr>
              <SortableHeader label={t("market.table.rank")} sortKey="rank" sort={sort} onSort={handleSort} />
              <SortableHeader label={t("market.table.name")} sortKey="name" sort={sort} onSort={handleSort} />
              <SortableHeader label={t("market.table.price")} sortKey="price" sort={sort} onSort={handleSort} />
              <SortableHeader label={t("market.table.change")} sortKey="change24h" sort={sort} onSort={handleSort} />
              {!compact ? (
                <SortableHeader
                  label={t("market.table.marketCap")}
                  sortKey="marketCap"
                  sort={sort}
                  onSort={handleSort}
                />
              ) : null}
              {!compact ? (
                <SortableHeader label={t("market.table.volume")} sortKey="volume" sort={sort} onSort={handleSort} />
              ) : null}
              <th>{t("market.table.range")}</th>
              {onSelectAsset ? <th>{t("market.table.details")}</th> : null}
            </tr>
          </thead>
          <tbody>
            {visibleAssets.map((asset) => {
              const isFavorite = favorites.includes(asset.symbol);

              return (
                <tr key={asset.id ?? asset.symbol}>
                  <td>{asset.rank ?? "--"}</td>
                  <td>
                    <Link className="asset-cell" to={`/coins/${asset.symbol.toLowerCase()}`}>
                      <AssetAvatar asset={asset} />
                      <span>
                        <strong>{asset.name}</strong>
                        <small>{asset.symbol}</small>
                      </span>
                    </Link>
                    <button
                      className={classNames("star-button", isFavorite && "is-active")}
                      type="button"
                      aria-label={
                        isFavorite
                          ? t("market.star.remove", { symbol: asset.symbol })
                          : t("market.star.add", { symbol: asset.symbol })
                      }
                      aria-pressed={isFavorite}
                      onClick={() => onToggleFavorite?.(asset.symbol)}
                    >
                      <Star size={17} />
                    </button>
                  </td>
                  <td className={asset.isLive ? "live-price" : ""}>
                    {formatCurrency(asset.price)}
                  </td>
                  <td>
                    <span className={classNames("change", `change--${marketTrend(asset.change24h)}`)}>
                      {formatPercent(asset.change24h)}
                    </span>
                  </td>
                  {!compact ? <td>{formatCurrency(asset.marketCap, { compact: true })}</td> : null}
                  {!compact ? <td>{formatCurrency(asset.volume, { compact: true })}</td> : null}
                  <td>
                    <PriceRange asset={asset} />
                  </td>
                   {onSelectAsset ? (
                     <td>
                       <button
                         className="icon-button"
                         type="button"
                         aria-label={t("market.table.details")}
                         title={t("market.table.details")}
                         onClick={() => onSelectAsset(asset)}
                       >
                         <Info size={16} />
                       </button>
                     </td>
                   ) : null}
                </tr>
              );
            })}
          </tbody>
        </table>

        {loading ? <div className="market-table-empty">{t("market.table.loading")}</div> : null}
        {!loading && sortedAssets.length === 0 ? (
          <div className="market-table-empty">{emptyContent ?? t("market.table.empty")}</div>
        ) : null}
      </div>

      <div className="table-footer">
        {sortedAssets.length > 0 ? (
          <span>
            {t("market.table.showing", {
              from: (page - 1) * PAGE_SIZE + 1,
              to: Math.min(page * PAGE_SIZE, sortedAssets.length),
              total: sortedAssets.length,
            })}
          </span>
        ) : (
          <span />
        )}
        <div className="pagination">
          <button
            type="button"
            aria-label={t("market.pagination.previous")}
            disabled={page === 1}
            onClick={() => setPage((value) => value - 1)}
          >
            <ChevronLeft size={18} />
          </button>
          <span className="pagination__status" aria-live="polite">
            {t("market.pagination.status", { page, total: pageCount })}
          </span>
          <button
            type="button"
            aria-label={t("market.pagination.next")}
            disabled={page >= pageCount}
            onClick={() => setPage((value) => value + 1)}
          >
            <ChevronRight size={18} />
          </button>
        </div>
      </div>
    </section>
  );
}
