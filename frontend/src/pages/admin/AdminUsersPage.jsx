import { Ban, RotateCcw, Search } from "lucide-react";
import { useMemo } from "react";
import Avatar from "../../components/shared/Avatar";
import { classNames } from "../../components/shared/utils";
import AdminLayout from "../../features/admin/AdminLayout";
import AdminTable, { StatusPill } from "../../features/admin/AdminTable";
import { ADMIN_USER_STATUSES } from "../../data/adminMockData.js";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useAppDispatch, useAppSelector } from "../../store/hooks";
import {
  filterAdminUsers,
  reactivateUser,
  selectAdminUsers,
  selectUserQuery,
  selectUserStatusFilter,
  setUserQuery,
  setUserStatusFilter,
  suspendUser,
} from "../../store/slices/adminSlice";
import { formatCount, formatRelativeTime, formatShortTime } from "../../utils/formatters";

const statusFilters = ["all", ...ADMIN_USER_STATUSES];

export default function AdminUsersPage() {
  const { t } = useTranslation();
  const dispatch = useAppDispatch();
  const users = useAppSelector(selectAdminUsers);
  const query = useAppSelector(selectUserQuery);
  const status = useAppSelector(selectUserStatusFilter);
  const visibleUsers = useMemo(
    () => filterAdminUsers(users, { query, status }),
    [query, status, users],
  );

  const columns = [
    { key: "user", label: t("admin.users.column.user") },
    { key: "status", label: t("admin.users.column.status") },
    { key: "role", label: t("admin.users.column.role") },
    { key: "messages", label: t("admin.users.column.messages"), numeric: true },
    { key: "reports", label: t("admin.users.column.reports"), numeric: true },
    { key: "joined", label: t("admin.users.column.joined") },
    { key: "lastActive", label: t("admin.users.column.lastActive") },
    { key: "actions", label: t("admin.column.actions") },
  ];

  return (
    <AdminLayout title={t("admin.users.title")} description={t("admin.users.description")}>
      <section className="utility-bar">
        <label>
          <Search size={19} />
          <input
            type="search"
            placeholder={t("admin.users.searchPlaceholder")}
            value={query}
            onChange={(event) => dispatch(setUserQuery(event.target.value))}
            aria-label={t("admin.users.searchPlaceholder")}
          />
        </label>
        <div>
          {statusFilters.map((filter) => (
            <button
              key={filter}
              className={classNames(filter === status && "is-active")}
              type="button"
              aria-pressed={filter === status}
              onClick={() => dispatch(setUserStatusFilter(filter))}
            >
              {t(`admin.users.filter.${filter}`)}
            </button>
          ))}
        </div>
      </section>

      <AdminTable
        columns={columns}
        rowCount={visibleUsers.length}
        total={users.length}
        emptyLabel={t("admin.users.empty")}
      >
        {visibleUsers.map((user) => (
          <tr key={user.id}>
            <td>
              <span className="admin-cell-user">
                <Avatar name={user.displayName} seed={user.username} />
                <span>
                  <strong>{user.displayName}</strong>
                  <small>@{user.username}</small>
                </span>
              </span>
            </td>
            <td>
              <StatusPill status={user.status} label={t(`admin.status.${user.status}`)} />
            </td>
            <td>{t(`admin.role.${user.role}`)}</td>
            <td className="admin-numeric">{formatCount(user.messageCount)}</td>
            <td className="admin-numeric">{formatCount(user.reportCount)}</td>
            <td>{formatShortTime(user.createdAt)}</td>
            <td>{user.lastActiveAt ? formatRelativeTime(user.lastActiveAt) : "--"}</td>
            <td>
              <span className="admin-row-actions">
                {user.status === "suspended" ? (
                  <button
                    className="button button--ghost button--sm"
                    type="button"
                    onClick={() => dispatch(reactivateUser(user.id))}
                  >
                    <RotateCcw size={15} /> {t("admin.users.reactivate")}
                  </button>
                ) : (
                  <button
                    className="button button--ghost button--sm"
                    type="button"
                    onClick={() => dispatch(suspendUser(user.id))}
                  >
                    <Ban size={15} /> {t("admin.users.suspend")}
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
