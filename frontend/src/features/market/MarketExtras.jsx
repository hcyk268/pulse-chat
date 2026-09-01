import { ArrowRight, Sparkles, Star, TrendingUp } from "lucide-react";
import { Link } from "react-router-dom";
import Avatar from "../../components/shared/Avatar";
import { coinDiscussions, coinNews, insightCards } from "../../data/traderHubData";
import { useTranslation } from "../../i18n/useTranslation.js";
import { formatRelativeTime } from "../../utils/formatters";

export function WatchlistCta({ trending = [] }) {
  const { t } = useTranslation();

  return (
    <section className="bento-two">
      <div className="cta-panel">
        <h2>{t("market.cta.title")}</h2>
        <p>{t("market.cta.body")}</p>
        <Link className="button button--light" to="/watchlist">
          {t("market.cta.action")}
        </Link>
        <Star size={160} />
      </div>
      <div className="panel trending-panel">
        <div className="panel-heading">
          <h3>{t("market.trending.title")}</h3>
          <TrendingUp size={20} />
        </div>
        {trending.slice(0, 7).map((asset, index) => (
          <Link
            key={asset.symbol}
            to={`/coins/${asset.symbol.toLowerCase()}`}
            className="search-row"
          >
            <span>{String(index + 1).padStart(2, "0")}</span>
            <strong>
              {asset.name} ({asset.symbol})
            </strong>
            <em className="trending-rank">#{asset.marketCapRank ?? "--"}</em>
          </Link>
        ))}
        {trending.length === 0 ? (
          <p className="market-empty-copy">{t("market.trending.empty")}</p>
        ) : null}
      </div>
    </section>
  );
}

export function WatchlistInsights() {
  return (
    <section className="insight-grid">
      {insightCards.map((card) => (
        <article className="insight-card" key={card.id}>
          <Sparkles size={22} />
          <h2>{card.title}</h2>
          <p>{card.body}</p>
          <Link to="/market">
            {card.action} <ArrowRight size={16} />
          </Link>
        </article>
      ))}
    </section>
  );
}

export function CoinNewsPanel() {
  const { t } = useTranslation();

  return (
    <section className="panel stack-panel">
      <div className="panel-heading">
        <h3>{t("coin.news.title")}</h3>
      </div>
      {coinNews.map((item) => (
        <Link className="news-mini" key={item.id} to="/community/macro-crypto-desk">
          <span>{item.tag}</span>
          <p>{item.title}</p>
          <small className="trending-rank">{formatRelativeTime(item.createdAt)}</small>
        </Link>
      ))}
    </section>
  );
}

export function CoinDiscussion({ symbol }) {
  const { t } = useTranslation();

  return (
    <section className="discussion-block">
      <h2>{t("coin.discussion.title")}</h2>
      {coinDiscussions.map((post) => (
        <article className="panel" key={post.id}>
          <div className="post-card__header">
            <Avatar name={post.author} seed={post.handle ?? post.author} />
            <div>
              <strong>{post.author}</strong>
              <span>
                {post.handle} · {formatRelativeTime(post.createdAt)}
              </span>
            </div>
          </div>
          <p>{post.body}</p>
        </article>
      ))}
      <Link className="button button--ghost" to="/community/daily-strategy">
        {t("coin.discussion.action", { symbol })} <ArrowRight size={16} />
      </Link>
    </section>
  );
}
