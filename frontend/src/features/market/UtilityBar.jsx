import { Search } from "lucide-react";
import { classNames } from "../../components/shared/utils";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useAppDispatch, useAppSelector } from "../../store/hooks";
import {
  MARKET_FILTERS,
  selectMarketFilter,
  setActiveMarketFilter,
} from "../../store/slices/marketSlice";

export default function UtilityBar({ query, onQueryChange, resultCount }) {
  const { t } = useTranslation();
  const dispatch = useAppDispatch();
  const activeFilter = useAppSelector(selectMarketFilter);

  return (
    <section className="utility-bar">
      <label>
        <Search size={19} />
        <input
          type="search"
          placeholder={t("market.searchPlaceholder")}
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
          aria-label={t("market.searchAria")}
        />
        {query ? <small>{t("market.results", { count: resultCount })}</small> : null}
      </label>
      <div>
        {MARKET_FILTERS.map((filter) => (
          <button
            key={filter}
            className={classNames(filter === activeFilter && "is-active")}
            type="button"
            aria-pressed={filter === activeFilter}
            onClick={() => dispatch(setActiveMarketFilter(filter))}
          >
            {t(`market.filter.${filter}`)}
          </button>
        ))}
      </div>
    </section>
  );
}
