import { useEffect, useState } from "react";
import TraderLayout from "../components/layout/TraderLayout";
import Alert from "../components/shared/Alert";
import Avatar from "../components/shared/Avatar";
import { useSignOut } from "../features/auth/useSignOut";
import EditProfileDialog from "../features/profile/EditProfileDialog";
import {
  ProfileBadgesCard,
  ProfileFocusCard,
  ProfileHeroActions,
  ProfileStats,
} from "../features/profile/ProfilePanels";
import { getApiErrorMessage, getMyProfile } from "../api/chatApi.js";
import { profileStats } from "../data/traderHubData";
import { useTranslation } from "../i18n/useTranslation.js";
import { useAppDispatch, useAppSelector } from "../store/hooks";
import { profileLoaded, selectCurrentUser } from "../store/slices/authSlice";
import { formatLongTime } from "../utils/formatters";

export default function ProfilePage() {
  const { t } = useTranslation();
  const dispatch = useAppDispatch();
  const signOut = useSignOut();
  const profile = useAppSelector(selectCurrentUser);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [editing, setEditing] = useState(false);

  useEffect(() => {
    let ignore = false;

    getMyProfile()
      .then((currentProfile) => {
        if (ignore || !currentProfile) return;
        dispatch(profileLoaded(currentProfile));
      })
      .catch((apiError) => {
        if (!ignore) setError(getApiErrorMessage(apiError, t, "errors.profileRefresh"));
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, [dispatch, t]);

  const displayName = profile?.displayName || profile?.username || "";
  const subtitle = profile?.bio || profile?.email || `@${profile?.username ?? "trader"}`;

  return (
    <TraderLayout active="profile">
      <main className="page-shell" id="main">
        {error ? <Alert onDismiss={() => setError("")}>{error}</Alert> : null}

        <section className="profile-hero">
          <div className="profile-cover" />
          <div className="profile-summary">
            <Avatar
              name={displayName}
              seed={profile?.username ?? displayName}
              size="xl"
              src={profile?.avatarUrl}
            />
            <div>
              <h1>{displayName}</h1>
              <p>{loading && !profile ? t("common.loading") : subtitle}</p>
              <div className="profile-summary__meta">
                <span>@{profile?.username ?? "trader"}</span>
                {profile?.createdAt ? (
                  <span>{t("profile.memberSince", { date: formatLongTime(profile.createdAt) })}</span>
                ) : null}
                {profile?.accountStatus ? <span>{profile.accountStatus}</span> : null}
              </div>
            </div>
            <ProfileHeroActions onEdit={() => setEditing(true)} onSignOut={signOut} />
          </div>
        </section>

        <ProfileStats stats={profileStats} />
        <p className="profile-sample-note">{t("profile.sampleData")}</p>

        <section className="profile-grid">
          <ProfileFocusCard />
          <ProfileBadgesCard />
        </section>
      </main>

      {editing ? (
        <EditProfileDialog
          profile={profile}
          onClose={() => setEditing(false)}
          onSaved={(updated) => dispatch(profileLoaded(updated))}
        />
      ) : null}
    </TraderLayout>
  );
}
