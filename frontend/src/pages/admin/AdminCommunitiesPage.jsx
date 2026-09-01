import { Archive, RotateCcw, Search, Star } from "lucide-react";
import { useMemo } from "react";
import { classNames } from "../../components/shared/utils";
import AdminLayout from "../../features/admin/AdminLayout";
import AdminTable, { StatusPill } from "../../features/admin/AdminTable";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useAppDispatch, useAppSelector } from "../../store/hooks";
import {
  archiveCommunity,
  filterAdminCommunities,
  restoreCommunity,
  selectAdminCommunities,
  selectAdminCommunityQuery,
  setCommunityQuery,
  toggleCommunityFeatured,
} from "../../store/slices/adminSlice";
import { formatCompactNumber, formatCount, formatShortTime } from "../../utils/formatters";

export default function AdminCommunitiesPage() {
  const { t } = useTranslation();
  const dispatch = useAppDispatch();
  const communities = useAppSelector(selectAdminCommunities);
  const query = useAppSelector(selectAdminCommunityQuery);
  const visibleCommunities = useMemo(
    () => filterAdminCommunities(communities, query),
    [communities, query],
  );

  const columns = [
    { key: "name", label: t("admin.communities.column.group") },
    { key: "owner", label: t("admin.communities.column.owner") },
    { key: "members", label: t("admin.communities.column.members"), numeric: true },
    { key: "messages", label: t("admin.communities.column.messages"), numeric: true },
    { key: "status", label: t("admin.users.column.status") },
    { key: "created", label: t("admin.communities.column.created") },
    { key: "actions", label: t("admin.column.actions") },
  ];

  return (
    <AdminLayout
      title={t("admin.communities.title")}
      description={t("admin.communities.description")}
    >
      <section className="utility-bar">
        <label>
          <Search size={19} />
          <input
            type="search"
            placeholder={t("admin.communities.searchPlaceholder")}
            value={query}
            onChange={(event) => dispatch(setCommunityQuery(event.target.value))}
            aria-label={t("admin.communities.searchPlaceholder")}
          />
        </label>
      </section>

      <AdminTable
        columns={columns}
        rowCount={visibleCommunities.length}
        total={communities.length}
        emptyLabel={t("admin.communities.empty")}
      >
        {visibleCommunities.map((community) => (
          <tr key={community.id}>
            <td>
              <span className="admin-cell-content">
                <strong>{community.name}</strong>
                {community.featured ? (
                  <small className="admin-featured">{t("admin.communities.featured")}</small>
                ) : null}
              </span>
            </td>
            <td>@{community.owner}</td>
            <td className="admin-numeric">{formatCompactNumber(community.memberCount)}</td>
            <td className="admin-numeric">{formatCount(community.messages7d)}</td>
            <td>
              <StatusPill status={community.status} label={t(`admin.status.${community.status}`)} />
            </td>
            <td>{formatShortTime(community.createdAt)}</td>
            <td>
              <span className="admin-row-actions">
                <button
                  className={classNames(
                    "button button--sm",
                    community.featured ? "button--primary" : "button--ghost",
                  )}
                  type="button"
                  aria-pressed={community.featured}
                  onClick={() => dispatch(toggleCommunityFeatured(community.id))}
                >
                  <Star size={15} />
                  {community.featured ? t("admin.communities.unfeature") : t("admin.communities.feature")}
                </button>
                {community.status === "archived" ? (
                  <button
                    className="button button--ghost button--sm"
                    type="button"
                    onClick={() => dispatch(restoreCommunity(community.id))}
                  >
                    <RotateCcw size={15} /> {t("admin.communities.restore")}
                  </button>
                ) : (
                  <button
                    className="button button--ghost button--sm"
                    type="button"
                    onClick={() => dispatch(archiveCommunity(community.id))}
                  >
                    <Archive size={15} /> {t("admin.communities.archive")}
                  </button>
                )}
              </span>
            </td>
          </tr>
        ))}
      </AdminTable>
    </AdminLayout>
  );
}
