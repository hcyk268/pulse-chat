import AdminLayout from "../../features/admin/AdminLayout";
import AdminTable from "../../features/admin/AdminTable";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useAppSelector } from "../../store/hooks";
import { selectAdminAuditLog } from "../../store/slices/adminSlice";
import { formatLongTime, formatRelativeTime } from "../../utils/formatters";

export default function AdminAuditPage() {
  const { t } = useTranslation();
  const auditLog = useAppSelector(selectAdminAuditLog);

  const columns = [
    { key: "action", label: t("admin.audit.column.action") },
    { key: "target", label: t("admin.audit.column.target") },
    { key: "actor", label: t("admin.audit.column.actor") },
    { key: "note", label: t("admin.audit.column.note") },
    { key: "when", label: t("admin.audit.column.when") },
  ];

  return (
    <AdminLayout title={t("admin.audit.title")} description={t("admin.audit.description")}>
      <AdminTable
        columns={columns}
        rowCount={auditLog.length}
        total={auditLog.length}
        emptyLabel={t("admin.audit.empty")}
      >
        {auditLog.map((entry) => (
          <tr key={entry.id}>
            <td>
              <strong>{t(`admin.action.${entry.action}`)}</strong>
            </td>
            <td>{entry.target}</td>
            <td>@{entry.actor}</td>
            <td>{entry.note || "--"}</td>
            <td title={formatLongTime(entry.createdAt)}>{formatRelativeTime(entry.createdAt)}</td>
          </tr>
        ))}
      </AdminTable>
    </AdminLayout>
  );
}
