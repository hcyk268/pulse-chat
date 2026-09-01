import { Check } from "lucide-react";
import { Link } from "react-router-dom";
import { classNames, identityHue, initials } from "../../components/shared/utils";
import { useTranslation } from "../../i18n/useTranslation.js";
import { formatCompactNumber } from "../../utils/formatters";

export default function CommunityCard({ community, joined = false, onToggleJoin }) {
  const { t } = useTranslation();
  const isOwner = community.membershipRole === "OWNER";
  const coverStyle = community.coverUrl
    ? { backgroundImage: `url("${community.coverUrl}")` }
    : { "--cover-h": identityHue(community.id) };

  return (
    <article className="community-card">
      <div
        className={classNames(
          "community-card__cover",
          community.coverUrl && "community-card__cover--image",
        )}
        style={coverStyle}
      >
        <strong>{initials(community.name)}</strong>
      </div>
      <div className="community-card__body">
        <h2>
          <Link to={`/community/${community.slug}`}>{community.name}</Link>
        </h2>
        <p>{community.description}</p>
        <div className="tag-list">
          {community.tags.map((tag) => (
            <span key={tag}>{tag}</span>
          ))}
        </div>
        <div className="community-card__meta">
          <span>{t("community.members", { count: formatCompactNumber(community.memberCount) })}</span>
          <span>{t("common.onlineCount", { count: formatCompactNumber(community.onlineCount) })}</span>
        </div>
        <div className="community-card__actions">
          <button
            className={classNames("button", joined ? "button--ghost" : "button--primary")}
            type="button"
            disabled={isOwner}
            onClick={() => onToggleJoin?.(community.id)}
          >
            {joined ? <Check size={16} /> : null}
            {isOwner
              ? t("community.owner")
              : joined
                ? t("community.joined")
                : t("community.join")}
          </button>
          <Link className="button button--ghost" to={`/community/${community.slug}`}>
            {t("common.open")}
          </Link>
        </div>
      </div>
    </article>
  );
}
