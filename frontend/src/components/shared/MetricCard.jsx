import { Minus, PieChart, TrendingDown, TrendingUp } from "lucide-react";
import { classNames } from "./utils";

const trendIcons = {
  up: TrendingUp,
  down: TrendingDown,
  flat: Minus,
};

export default function MetricCard({ stat }) {
  const Icon = stat.icon ?? PieChart;
  const TrendIcon = trendIcons[stat.trend] ?? Minus;
  // The bar is only drawn when the number it represents is real.
  const progress = Number(stat.progress);
  const hasProgress = Number.isFinite(progress);

  return (
    <article className="metric-card">
      <div className="metric-card__top">
        <span>{stat.label}</span>
        <Icon size={20} />
      </div>
      <div className="metric-card__value">
        <strong>{stat.value}</strong>
        <span className={classNames("change", `change--${stat.trend ?? "flat"}`)}>
          <TrendIcon size={15} />
          {stat.change}
        </span>
      </div>
      {hasProgress ? (
        <div>
          <div className="progress-track">
            <span style={{ width: `${Math.min(100, Math.max(0, progress))}%` }} />
          </div>
          {stat.progressLabel ? (
            <small className="progress-caption">{stat.progressLabel}</small>
          ) : null}
        </div>
      ) : null}
    </article>
  );
}
