import { Check, X } from "lucide-react";
import { useMemo } from "react";
import { classNames } from "../../components/shared/utils";
import AdminLayout from "../../features/admin/AdminLayout";
import AdminTable, { StatusPill } from "../../features/admin/AdminTable";
import { ADMIN_REPORT_STATUSES } from "../../data/adminMockData.js";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useAppDispatch, useAppSelector } from "../../store/hooks";
import {
  dismissReport,
  filterAdminReports,
  resolveReport,
  selectAdminReports,
  selectReportStatusFilter,
  setReportStatusFilter,
} from "../../store/slices/adminSlice";
import { formatRelativeTime } from "../../utils/formatters";

const statusFilters = ["all", ...ADMIN_REPORT_STATUSES];

export default function AdminModerationPage() {
  const { t } = useTranslation();
  const dispatch = useAppDispatch();
  const reports = useAppSelector(selectAdminReports);
  const status = useAppSelector(selectReportStatusFilter);
  const visibleReports = useMemo(() => filterAdminReports(reports, status), [reports, status]);

  const columns = [
    { key: "content", label: t("admin.moderation.column.content") },
    { key: "reason", label: t("admin.moderation.column.reason") },
    { key: "reportedUser", label: t("admin.moderation.column.reportedUser") },
    { key: "reportedBy", label: t("admin.moderation.column.reportedBy") },
    { key: "status", label: t("admin.users.column.status") },
    { key: "createdAt", label: t("admin.moderation.column.received") },
    { key: "actions", label: t("admin.column.actions") },
  ];

  return (
    <AdminLayout
      title={t("admin.moderation.title")}
      description={t("admin.moderation.description")}
    >
      <section className="utility-bar">
        <div>
          {statusFilters.map((filter) => (
            <button
              key={filter}
              className={classNames(filter === status && "is-active")}
              type="button"
              aria-pressed={filter === status}
              onClick={() => dispatch(setReportStatusFilter(filter))}
            >
              {t(`admin.moderation.filter.${filter}`)}
            </button>
          ))}
        </div>
      </section>

      <AdminTable
        columns={columns}
        rowCount={visibleReports.length}
        total={reports.length}
        emptyLabel={t("admin.moderation.empty")}
      >
        {visibleReports.map((report) => (
          <tr key={report.id}>
            <td>
              <span className="admin-cell-content">
                <strong>{report.excerpt}</strong>
                <small>
                  {t(`admin.moderation.target.${report.target}`)} · {report.context}
                </small>
              </span>
            </td>
            <td>{t(`admin.moderation.reason.${report.reason}`)}</td>
            <td>@{report.reportedUser}</td>
            <td>@{report.reportedBy}</td>
            <td>
              <StatusPill status={report.status} label={t(`admin.status.${report.status}`)} />
            </td>
            <td>{formatRelativeTime(report.createdAt)}</td>
            <td>
              <span className="admin-row-actions">
                {report.status === "open" ? (
                  <>
                    <button
                      className="button button--primary button--sm"
                      type="button"
                      onClick={() => dispatch(resolveReport(report.id))}
                    >
                      <Check size={15} /> {t("admin.moderation.resolve")}
                    </button>
                    <button
                      className="button button--ghost button--sm"
                      type="button"
                      onClick={() => dispatch(dismissReport(report.id))}
                    >
                      <X size={15} /> {t("admin.moderation.dismiss")}
                    </button>
                  </>
                ) : (
                  <small className="admin-resolved-note">
                    {t("admin.moderation.closedBy", {
                      actor: report.resolvedBy ?? "--",
                      time: formatRelativeTime(report.resolvedAt),
                    })}
                  </small>
                )}
              </span>
            </td>
          </tr>
        ))}
      </AdminTable>
    </AdminLayout>
  );
}
