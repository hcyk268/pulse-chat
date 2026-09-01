import {
  Compass,
  MessageCircleMore,
  Plus,
  Radio,
  RefreshCw,
  Search,
  ShieldCheck,
  UsersRound,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  createCommunity,
  getApiErrorMessage,
  getCommunities,
  getCommunityCategories,
  getCommunityTags,
  joinCommunity,
  leaveCommunity,
  normalizeCommunityDetail,
  normalizeCommunitySummary,
} from "../api/communityApi";
import PageShell from "../components/layout/PageShell";
import TraderLayout from "../components/layout/TraderLayout";
import Alert from "../components/shared/Alert";
import { classNames } from "../components/shared/utils";
import { communityCategories } from "../data/traderHubData";
import { useAuthGate } from "../features/auth/AuthGateContext.jsx";
import CommunityCard from "../features/community/CommunityCard";
import CommunityFormDialog from "../features/community/CommunityFormDialog";
import { useDebouncedValue } from "../hooks/useDebouncedValue.js";
import { useTranslation } from "../i18n/useTranslation.js";
import { useAppDispatch, useAppSelector } from "../store/hooks";
import { selectIsAuthenticated } from "../store/slices/authSlice";
import {
  filterCommunities,
  selectCommunityCategory,
  selectCommunityQuery,
  selectJoinedCommunityIds,
  setCommunityCategory,
  setCommunityQuery,
  setJoinedCommunityIds,
  upsertJoinedCommunity,
} from "../store/slices/communitySlice";

const fallbackCategories = communityCategories.map((slug) => ({ slug, name: slug }));

function categoryLabel(category, t) {
  const slug = category?.slug ?? category;
  const key = `community.category.${slug}`;
  const translated = t(key);
  return translated === key ? category?.name ?? slug : translated;
}

function replaceCommunity(items, community) {
  if (!community?.id) return items;
  return items.map((item) => (String(item.id) === String(community.id) ? community : item));
}

function CommunitySkeleton() {
  return (
    <section className="community-skeleton-grid" aria-hidden="true">
      {[0, 1, 2].map((item) => (
        <article key={item}>
          <div />
          <span />
          <p />
          <p />
          <footer />
        </article>
      ))}
    </section>
  );
}

export default function CommunityPage() {
  const { t } = useTranslation();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { openAuth } = useAuthGate();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const activeCategory = useAppSelector(selectCommunityCategory);
  const query = useAppSelector(selectCommunityQuery);
  const joinedIds = useAppSelector(selectJoinedCommunityIds);
  const debouncedQuery = useDebouncedValue(query, 250);
  const [categoryOptions, setCategoryOptions] = useState(fallbackCategories);
  const [tagOptions, setTagOptions] = useState([]);
  const [communities, setCommunities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [creating, setCreating] = useState(false);
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    let active = true;
    Promise.allSettled([getCommunityCategories(), getCommunityTags()]).then(
      ([categoriesResult, tagsResult]) => {
        if (!active) return;
        if (categoriesResult.status === "fulfilled" && categoriesResult.value?.length) {
          setCategoryOptions([
            fallbackCategories[0],
            ...categoriesResult.value.map((category) => ({
              id: category.id,
              slug: category.slug,
              name: category.name,
              description: category.description,
            })),
          ]);
        }
        if (tagsResult.status === "fulfilled") setTagOptions(tagsResult.value ?? []);
      },
    );
    return () => {
      active = false;
    };
  }, [retryKey]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    getCommunities({ category: activeCategory, query: debouncedQuery, limit: 50 })
      .then((items) => {
        if (!active) return;
        const normalized = (items ?? []).map(normalizeCommunitySummary).filter(Boolean);
        setCommunities(normalized);
        dispatch(
          setJoinedCommunityIds(
            normalized.filter((community) => community.isMember).map((community) => community.id),
          ),
        );
      })
      .catch((requestError) => {
        if (active) setError(getApiErrorMessage(requestError, t, "errors.communityData"));
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [activeCategory, debouncedQuery, dispatch, retryKey, t]);

  const handleToggleJoin = useCallback(
    async (communityId) => {
      const community = communities.find((item) => String(item.id) === String(communityId));
      if (!community) return;

      if (!isAuthenticated) {
        openAuth({
          kind: "community.join",
          title: t("guest.community.joinTitle", { community: community.name }),
          description: t("guest.community.joinDescription", { community: community.name }),
          payload: { communityId },
        }, async () => {
          try {
            const joined = normalizeCommunityDetail(await joinCommunity(communityId));
            if (!joined?.community) return;
            setCommunities((current) => replaceCommunity(current, joined.community));
            dispatch(upsertJoinedCommunity({ communityId, joined: true }));
          } catch (requestError) {
            setError(getApiErrorMessage(requestError, t, "errors.communityJoin"));
          }
        });
        return;
      }

      const wasJoined = Boolean(community.isMember);
      const nextJoined = !wasJoined;
      const optimistic = {
        ...community,
        isMember: nextJoined,
        memberCount: Math.max(0, (community.memberCount ?? 0) + (nextJoined ? 1 : -1)),
      };

      setCommunities((current) => replaceCommunity(current, optimistic));
      dispatch(upsertJoinedCommunity({ communityId, joined: nextJoined }));
      setError("");

      try {
        if (nextJoined) {
          const detail = normalizeCommunityDetail(await joinCommunity(communityId));
          if (detail?.community) {
            setCommunities((current) => replaceCommunity(current, detail.community));
            dispatch(upsertJoinedCommunity({ communityId, joined: detail.community.isMember }));
          }
        } else {
          await leaveCommunity(communityId);
        }
      } catch (requestError) {
        setCommunities((current) => replaceCommunity(current, community));
        dispatch(upsertJoinedCommunity({ communityId, joined: wasJoined }));
        setError(getApiErrorMessage(requestError, t, "errors.communityJoin"));
      }
    },
    [communities, dispatch, isAuthenticated, openAuth, t],
  );

  function handleCreateClick() {
    if (!isAuthenticated) {
      openAuth({
        kind: "community.create",
        mode: "register",
        title: t("guest.community.createTitle"),
        description: t("guest.community.createDescription"),
      }, () => setCreating(true));
      return;
    }
    setCreating(true);
  }

  function handleCreated(response) {
    const created = normalizeCommunityDetail(response);
    if (created?.community?.slug) navigate(`/community/${created.community.slug}`);
  }

  const visibleCommunities = useMemo(
    () => filterCommunities(communities, { category: activeCategory, query }),
    [activeCategory, communities, query],
  );
  const joinedCount = communities.filter(
    (community) => community.isMember || joinedIds.some((id) => String(id) === String(community.id)),
  ).length;
  const showRecovery = !loading && Boolean(error) && visibleCommunities.length === 0;
  const showEmpty = !loading && !error && visibleCommunities.length === 0;

  return (
    <TraderLayout active="community">
      <PageShell
        eyebrow={t("community.eyebrow")}
        title={t("community.title")}
        description={t("community.description")}
        action={
          <div className="page-heading__actions">
            <span className={classNames("meta-chip", !isAuthenticated && "community-public-chip")}>
              {!isAuthenticated ? <Radio size={14} /> : null}
              {isAuthenticated
                ? t("community.joinedSummary", { joined: joinedCount, total: communities.length })
                : t("guest.community.publicAccess")}
            </span>
            <button className="button button--primary" type="button" onClick={handleCreateClick}>
              <Plus size={17} />
              {t("community.create")}
            </button>
          </div>
        }
      >
        {!isAuthenticated ? (
          <section className="community-guest-intro">
            <div className="community-guest-intro__copy">
              <span className="community-guest-intro__icon"><MessageCircleMore size={22} /></span>
              <div>
                <h2>{t("guest.community.introTitle")}</h2>
                <p>{t("guest.community.introBody")}</p>
              </div>
            </div>
            <div className="community-guest-intro__features">
              <span><ShieldCheck size={16} /> {t("guest.community.featureRead")}</span>
              <span><UsersRound size={16} /> {t("guest.community.featurePeople")}</span>
              <span><Radio size={16} /> {t("guest.community.featureLive")}</span>
            </div>
          </section>
        ) : null}

        {error && isAuthenticated ? <Alert onDismiss={() => setError("")}>{error}</Alert> : null}

        <section className="utility-bar community-discovery-bar">
          <label>
            <Search size={19} />
            <input
              type="search"
              placeholder={t("community.searchPlaceholder")}
              value={query}
              onChange={(event) => dispatch(setCommunityQuery(event.target.value))}
            />
          </label>
          <div className="category-strip">
            {categoryOptions.map((category) => (
              <button
                key={category.slug}
                className={classNames(category.slug === activeCategory && "is-active")}
                type="button"
                onClick={() => dispatch(setCommunityCategory(category.slug))}
              >
                {categoryLabel(category, t)}
              </button>
            ))}
          </div>
        </section>

        {loading ? <CommunitySkeleton /> : null}

        {!loading && !showRecovery ? (
          <section className="community-grid community-grid--discovery">
            {visibleCommunities.map((community) => (
              <CommunityCard
                key={community.id}
                community={community}
                joined={community.isMember}
                onToggleJoin={handleToggleJoin}
              />
            ))}
          </section>
        ) : null}

        {showRecovery ? (
          <section className="community-empty-state community-empty-state--recovery">
            <span><RefreshCw size={24} /></span>
            <div>
              <h2>{t("guest.community.reconnectTitle")}</h2>
              <p>{t("guest.community.reconnectBody")}</p>
            </div>
            <div>
              <button className="button button--primary" type="button" onClick={() => setRetryKey((current) => current + 1)}>
                <RefreshCw size={16} /> {t("common.retry")}
              </button>
              <Link className="button button--ghost" to="/market">{t("guest.community.marketAction")}</Link>
            </div>
          </section>
        ) : null}

        {showEmpty ? (
          <section className="community-empty-state">
            <span><Compass size={24} /></span>
            <div>
              <h2>{t("guest.community.emptyTitle")}</h2>
              <p>{t("guest.community.emptyBody")}</p>
            </div>
            <button className="button button--ghost" type="button" onClick={() => {
              dispatch(setCommunityCategory("all"));
              dispatch(setCommunityQuery(""));
            }}>
              {t("guest.community.clearFilters")}
            </button>
          </section>
        ) : null}
      </PageShell>

      {creating ? (
        <CommunityFormDialog
          mode="create"
          categories={categoryOptions}
          tags={tagOptions}
          onClose={() => setCreating(false)}
          onSaved={handleCreated}
          onSubmit={createCommunity}
        />
      ) : null}
    </TraderLayout>
  );
}