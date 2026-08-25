import { useEffect, useMemo, useRef } from "react";
import { CandlestickSeries, ColorType, createChart } from "lightweight-charts";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useAppSelector } from "../../store/hooks";
import { selectTheme } from "../../store/slices/uiSlice";

function toChartCandle(candle) {
  return {
    time: Math.floor(new Date(candle.openTime).getTime() / 1000),
    open: Number(candle.open),
    high: Number(candle.high),
    low: Number(candle.low),
    close: Number(candle.close),
  };
}

/** Price labels should not show eight decimals for a five-figure asset. */
function resolvePrecision(referencePrice) {
  const price = Math.abs(Number(referencePrice) || 0);
  if (price >= 1000) return 2;
  if (price >= 1) return 4;
  return 8;
}

/**
 * The chart takes its colours from the same CSS tokens as the tables, so "up"
 * and "down" cannot drift between the two, and it follows the theme for free.
 */
function readChartTheme() {
  const styles = getComputedStyle(document.documentElement);
  const token = (name, fallback) => styles.getPropertyValue(name).trim() || fallback;

  return {
    background: token("--surface", "#ffffff"),
    text: token("--muted", "#64748b"),
    grid: token("--border", "#e2e8f0"),
    crosshair: token("--border-strong", "#94a3b8"),
    up: token("--success-soft", "#16a34a"),
    down: token("--danger-soft", "#dc2626"),
  };
}

function buildChartOptions(colors) {
  return {
    layout: {
      background: { type: ColorType.Solid, color: colors.background },
      textColor: colors.text,
      attributionLogo: false,
    },
    grid: {
      vertLines: { color: colors.grid },
      horzLines: { color: colors.grid },
    },
    rightPriceScale: { borderColor: colors.grid },
    timeScale: { borderColor: colors.grid, timeVisible: true, secondsVisible: false },
    crosshair: {
      vertLine: { color: colors.crosshair },
      horzLine: { color: colors.crosshair },
    },
  };
}

function buildSeriesOptions(colors) {
  return {
    upColor: colors.up,
    downColor: colors.down,
    borderVisible: false,
    wickUpColor: colors.up,
    wickDownColor: colors.down,
  };
}

export default function CandlestickChart({ candles = [], emptyMessage, loading = false, symbol }) {
  const { t } = useTranslation();
  const theme = useAppSelector(selectTheme);
  const containerRef = useRef(null);
  const chartRef = useRef(null);
  const seriesRef = useRef(null);
  const lastTimeRef = useRef(null);
  const countRef = useRef(0);

  const normalizedCandles = useMemo(
    () =>
      candles
        .map(toChartCandle)
        .filter((candle) => Number.isFinite(candle.time) && Number.isFinite(candle.close))
        .sort((left, right) => left.time - right.time),
    [candles],
  );

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return undefined;

    const colors = readChartTheme();
    const chart = createChart(container, { autoSize: true, ...buildChartOptions(colors) });

    chartRef.current = chart;
    seriesRef.current = chart.addSeries(CandlestickSeries, buildSeriesOptions(colors));

    return () => {
      lastTimeRef.current = null;
      countRef.current = 0;
      seriesRef.current = null;
      chartRef.current = null;
      chart.remove();
    };
  }, []);

  useEffect(() => {
    if (!chartRef.current || !seriesRef.current) return;

    const colors = readChartTheme();
    chartRef.current.applyOptions(buildChartOptions(colors));
    seriesRef.current.applyOptions(buildSeriesOptions(colors));
  }, [theme]);

  useEffect(() => {
    const series = seriesRef.current;
    if (!series || normalizedCandles.length === 0) return;

    const latest = normalizedCandles.at(-1);
    const precision = resolvePrecision(latest.close);
    series.applyOptions({
      priceFormat: { type: "price", precision, minMove: 10 ** -precision },
    });

    // Streaming updates only move the newest bar; anything else is a full reload.
    const isStreamingUpdate =
      lastTimeRef.current !== null &&
      latest.time >= lastTimeRef.current &&
      normalizedCandles.length - countRef.current <= 1;

    if (isStreamingUpdate) {
      series.update(latest);
    } else {
      series.setData(normalizedCandles);
    }

    lastTimeRef.current = latest.time;
    countRef.current = normalizedCandles.length;
  }, [normalizedCandles]);

  return (
    <div className="market-chart-shell">
      <div
        ref={containerRef}
        className="market-chart"
        role="img"
        aria-label={t("coin.chart.aria", { symbol })}
        aria-busy={loading}
      />
      {normalizedCandles.length === 0 ? (
        <div className="market-chart__empty">
          <strong>{loading ? t("common.loading") : t("coin.chart.waitingTitle")}</strong>
          <span>{emptyMessage ?? t("coin.chart.waitingBody")}</span>
        </div>
      ) : null}
      <a
        className="chart-attribution"
        href="https://www.tradingview.com/"
        target="_blank"
        rel="noreferrer"
      >
        {t("coin.chart.attribution")}
      </a>
    </div>
  );
}
