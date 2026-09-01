import { CheckCircle2, KeyRound, LogOut, Pencil, ShieldCheck } from "lucide-react";
import { Link } from "react-router-dom";
import { profileBadges, profileFocusSummary, profileFocusTags } from "../../data/traderHubData";
import { useTranslation } from "../../i18n/useTranslation.js";

export function ProfileStats({ stats }) {
  const { t } = useTranslation();

  return (
    <section className="profile-stat-grid">
      {stats.map((stat) => (
        <article className="panel" key={stat.key}>
          <span>{t(`profile.stat.${stat.key}`)}</span>
          <strong>{stat.value}</strong>
        </article>
      ))}
    </section>
  );
}

export function ProfileFocusCard() {
  const { t } = useTranslation();

  return (
    <article className="panel">
      <div className="panel-heading">
        <h2>{t("profile.focus.title")}</h2>
        <ShieldCheck size={20} />
      </div>
      <p>{profileFocusSummary}</p>
      <div className="tag-list">
        {profileFocusTags.map((tag) => (
          <span key={tag}>{tag}</span>
        ))}
      </div>
    </article>
  );
}

export function ProfileBadgesCard() {
  const { t } = useTranslation();

  return (
    <article className="panel">
      <div className="panel-heading">
        <h2>{t("profile.badges.title")}</h2>
        <CheckCircle2 size={20} />
      </div>
      <div className="badge-list">
        {profileBadges.map((badge) => (
          <span key={badge}>{badge}</span>
        ))}
      </div>
    </article>
  );
}

export function ProfileHeroActions({ onEdit, onSignOut }) {
  const { t } = useTranslation();

  return (
    <div className="profile-actions">
      <button className="button button--ghost" type="button" onClick={onSignOut}>
        <LogOut size={17} /> {t("common.signOut")}
      </button>
      <Link className="button button--ghost" to="/change-password">
        <KeyRound size={17} /> {t("auth.changePassword")}
      </Link>
      <button className="button button--primary" type="button" onClick={onEdit}>
        <Pencil size={17} /> {t("profile.editProfile")}
      </button>
    </div>
  );
}
