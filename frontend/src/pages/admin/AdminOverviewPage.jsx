import { Activity, MessageSquare, ShieldAlert, Users } from "lucide-react";
import { useMemo } from "react";
import MetricCard from "../../components/shared/MetricCard";
import AdminLayout from "../../features/admin/AdminLayout";
import { adminKpis, adminSignupSeries } from "../../data/adminMockData.js";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useAppSelector } from "../../store/hooks";
import { selectAdminAuditLog } from "../../store/slices/adminSlice";
import { formatCompactNumber, formatPercent, formatRelativeTime } from "../../utils/formatters";

const kpiIcons = {
  users: Users,
  activeToday: Activity,
  messages24h: MessageSquare,
  openReports: ShieldAlert,
};

function SignupChart({ series, caption }) {
  const peak = Math.max(...series.map((point) => point.value), 1);

  return (
    <div className="admin-bars" role="img" aria-label={caption}>
      {series.map((point) => (
        <div className="admin-bars__column" key={point.label}>
          <span style={{ height: `${Math.round((point.value / peak) * 100)}%` }} />
          <small>{point.label}</small>
          <em>{point.value}</em>
        </div>
      ))}
    </div>
  );
}

export default function AdminOverviewPage() {
  const { t } = useTranslation();
  const auditLog = useAppSelector(selectAdminAuditLog);

  const stats = useMemo(
    () =>
      adminKpis.map((kpi) => ({
        label: t(`admin.kpi.${kpi.key}`),
        value: formatCompactNumber(kpi.value),
        change: formatPercent(kpi.deltaPercent),
        trend: kpi.deltaPercent > 0 ? "up" : kpi.deltaPercent < 0 ? "down" : "flat",
        icon: kpiIcons[kpi.key] ?? Activity,
      })),
    [t],
  );

  return (
    <AdminLayout title={t("admin.overview.title")} description={t("admin.overview.description")}>
      <section className="metric-grid metric-grid--four">
        {stats.map((stat) => (
          <MetricCard key={stat.label} stat={stat} />
        ))}
      </section>

      <div className="admin-split">
        <section className="panel">
          <div className="panel-heading">
            <h2>{t("admin.overview.signups")}</h2>
          </div>
          <SignupChart series={adminSignupSeries} caption={t("admin.overview.signups")} />
        </section>

        <section className="panel stack-panel">
          <div className="panel-heading">
            <h2>{t("admin.overview.recentActions")}</h2>
          </div>
          {auditLog.slice(0, 6).map((entry) => (
            <div className="admin-activity-row" key={entry.id}>
              <div>
                <strong>{t(`admin.action.${entry.action}`)}</strong>
                <small>
                  {entry.target} · @{entry.actor}
                </small>
              </div>
              <time dateTime={entry.createdAt}>{formatRelativeTime(entry.createdAt)}</time>
            </div>
          ))}
        </section>
      </div>
    </AdminLayout>
  );
}
