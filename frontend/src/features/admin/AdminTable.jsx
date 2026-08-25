import { classNames } from "../../components/shared/utils";
import { useTranslation } from "../../i18n/useTranslation.js";

/**
 * Table chrome only: the scroll container, header row, empty state and the
 * "showing n of m" footer. Each page renders its own rows because the cells and
 * row actions differ per section.
 */
export default function AdminTable({ columns, children, rowCount, total, emptyLabel }) {
  const { t } = useTranslation();

  return (
    <section className="panel admin-table-panel">
      <div className="table-scroll">
        <table className="admin-table">
          <thead>
            <tr>
              {columns.map((column) => (
                <th key={column.key} className={classNames(column.numeric && "admin-numeric")}>
                  {column.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>{children}</tbody>
        </table>

        {rowCount === 0 ? <div className="market-table-empty">{emptyLabel}</div> : null}
      </div>

      {rowCount > 0 ? (
        <div className="table-footer">
          <span>{t("admin.table.showing", { count: rowCount, total })}</span>
        </div>
      ) : null}
    </section>
  );
}

export function StatusPill({ status, label }) {
  return <span className={`status-pill status-pill--${status}`}>{label}</span>;
}
