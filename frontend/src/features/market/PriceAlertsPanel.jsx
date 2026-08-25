import { Bell, BellOff, Pencil, Plus, RefreshCw, Trash2 } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  createPriceAlert,
  deletePriceAlert,
  getApiErrorMessage,
  getPriceAlert,
  getPriceAlerts,
  updatePriceAlert,
} from "../../api/marketApi.js";
import Modal from "../../components/shared/Modal.jsx";
import {
  buildPriceAlertRequest,
  createPriceAlertForm,
  getPriceAlertMode,
  PRICE_ALERT_DIRECTIONS,
  PRICE_ALERT_MODES,
  validatePriceAlertForm,
} from "../../domain/market/priceAlerts.js";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useAppSelector } from "../../store/hooks.js";
import { selectIsAuthenticated } from "../../store/slices/authSlice.js";
import { formatCurrency, formatPercent, formatRelativeTime } from "../../utils/formatters.js";
import { useAuthGate } from "../auth/AuthGateContext.jsx";

function PriceAlertDialog({ alert, assets, onClose, onSave }) {
  const { t } = useTranslation();
  const editing = Boolean(alert);
  const [form, setForm] = useState(() => createPriceAlertForm(alert));
  const [status, setStatus] = useState({ saving: false, error: "" });

  const selectableAssets = useMemo(() => {
    const bySymbol = new Map(assets.map((asset) => [asset.symbol, asset]));
    const alertSymbol = alert?.pair?.baseSymbol?.toUpperCase();

    if (alertSymbol && !bySymbol.has(alertSymbol)) {
      bySymbol.set(alertSymbol, {
        name: alertSymbol,
        symbol: alertSymbol,
        pairSymbol: alert.pair?.symbol ?? "",
        currentPriceUsd: null,
        priceChangePercentage24h: null,
      });
    }

    return [...bySymbol.values()];
  }, [alert, assets]);

  const selectedAsset = selectableAssets.find(
    (asset) => asset.symbol === form.symbol.trim().toUpperCase(),
  );
  const mode = getPriceAlertMode(form.conditionType);
  const targetIsPercent = mode === "PERCENT";

  function updateField(event) {
    const { checked, name, type, value } = event.target;
    setForm((current) => ({ ...current, [name]: type === "checkbox" ? checked : value }));
  }

  function selectMode(nextMode) {
    if (nextMode === mode) return;
    setForm((current) => ({
      ...current,
      conditionType: nextMode === "PERCENT" ? "CHANGE_PERCENT" : "ABOVE",
      targetValue: "",
    }));
  }

  function selectDirection(conditionType) {
    setForm((current) => ({ ...current, conditionType }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const supportedSymbols = selectableAssets.map((asset) => asset.symbol);
    const validationKey = validatePriceAlertForm(form, supportedSymbols);

    if (validationKey) {
      setStatus({ saving: false, error: t(validationKey) });
      return;
    }

    setStatus({ saving: true, error: "" });
    try {
      await onSave(buildPriceAlertRequest(form));
      onClose();
    } catch (requestError) {
      setStatus({
        saving: false,
        error: getApiErrorMessage(
          requestError,
          t,
          editing ? "errors.priceAlertUpdate" : "errors.priceAlertCreate",
        ),
      });
    }
  }

  return (
    <Modal
      title={t(editing ? "market.alerts.editTitle" : "market.alerts.createTitle")}
      description={t(
        editing ? "market.alerts.editDescription" : "market.alerts.createDescription",
      )}
      onClose={status.saving ? undefined : onClose}
    >
      <form className="dialog-form price-alert-form" onSubmit={handleSubmit} noValidate>
        <label>
          {t("market.alerts.symbol")}
          <input
            name="symbol"
            value={form.symbol}
            onChange={updateField}
            placeholder={t("market.alerts.symbolPlaceholder")}
            list="price-alert-assets"
            maxLength={20}
            autoComplete="off"
            required
          />
          <datalist id="price-alert-assets">
            {selectableAssets.map((asset) => (
              <option key={asset.symbol} value={asset.symbol}>
                {asset.name} ({asset.pairSymbol})
              </option>
            ))}
          </datalist>
        </label>

        {selectedAsset ? (
          <div className="price-alert-form__market" aria-live="polite">
            <div>
              <span>{selectedAsset.pairSymbol}</span>
              <small>{t("market.alerts.currentPrice")}</small>
              <strong>{formatCurrency(selectedAsset.currentPriceUsd)}</strong>
            </div>
            <div>
              <small>{t("market.alerts.change24h")}</small>
              <strong>{formatPercent(selectedAsset.priceChangePercentage24h)}</strong>
            </div>
          </div>
        ) : null}

        <div className="price-alert-form__group">
          <span className="price-alert-form__label">{t("market.alerts.mode")}</span>
          <div className="segmented price-alert-form__segmented" role="group" aria-label={t("market.alerts.mode")}>
            {PRICE_ALERT_MODES.map((item) => (
              <button
                key={item}
                className={mode === item ? "is-active" : ""}
                type="button"
                aria-pressed={mode === item}
                onClick={() => selectMode(item)}
              >
                {t(`market.alerts.mode.${item}`)}
              </button>
            ))}
          </div>
        </div>

        {!targetIsPercent ? (
          <div className="price-alert-form__group">
            <span className="price-alert-form__label">{t("market.alerts.direction")}</span>
            <div
              className="segmented price-alert-form__segmented"
              role="group"
              aria-label={t("market.alerts.direction")}
            >
              {PRICE_ALERT_DIRECTIONS.map((condition) => (
                <button
                  key={condition}
                  className={form.conditionType === condition ? "is-active" : ""}
                  type="button"
                  aria-pressed={form.conditionType === condition}
                  onClick={() => selectDirection(condition)}
                >
                  {t(`market.alerts.condition.${condition}`)}
                </button>
              ))}
            </div>
          </div>
        ) : null}

        <label>
          {t(targetIsPercent ? "market.alerts.targetPercent" : "market.alerts.targetPrice")}
          <input
            name="targetValue"
            type="number"
            min="0"
            step="any"
            inputMode="decimal"
            value={form.targetValue}
            onChange={updateField}
            placeholder={t(
              targetIsPercent
                ? "market.alerts.targetPercentPlaceholder"
                : "market.alerts.targetPricePlaceholder",
            )}
            required
          />
          <small className="price-alert-form__hint">
            {t(
              targetIsPercent
                ? "market.alerts.targetPercentHint"
                : `market.alerts.targetPriceHint.${form.conditionType}`,
              { symbol: selectedAsset?.symbol ?? form.symbol.trim().toUpperCase() },
            )}
          </small>
        </label>

        <label className="price-alert-form__active">
          <input name="active" type="checkbox" checked={form.active} onChange={updateField} />
          <span>{t("market.alerts.active")}</span>
        </label>

        {status.error ? (
          <p className="form-error" role="alert">
            {status.error}
          </p>
        ) : null}

        <div className="dialog-form__actions">
          <button
            className="button button--ghost"
            type="button"
            onClick={onClose}
            disabled={status.saving}
          >
            {t("common.cancel")}
          </button>
          <button className="button button--primary" type="submit" disabled={status.saving}>
            {status.saving ? t("common.saving") : t("common.save")}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function formatTarget(alert) {
  if (alert.conditionType === "CHANGE_PERCENT") {
    return formatPercent(alert.targetPercent, { signed: false });
  }

  return formatCurrency(alert.targetPrice);
}

function formatAlertActivity(alert, t) {
  if (alert.triggeredAt) {
    return t("market.alerts.triggered", {
      price: formatCurrency(alert.lastTriggeredPrice),
      time: formatRelativeTime(alert.triggeredAt),
    });
  }
  if (alert.lastCheckedAt) {
    return t("market.alerts.lastChecked", { time: formatRelativeTime(alert.lastCheckedAt) });
  }
  return t("market.alerts.notChecked");
}

export default function PriceAlertsPanel({ assets = [] }) {
  const { t } = useTranslation();
  const { openAuth } = useAuthGate();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [dialog, setDialog] = useState(null);
  const [pendingId, setPendingId] = useState(null);

  const availableAssets = useMemo(() => {
    const bySymbol = new Map();

    assets.forEach((asset) => {
      const symbol = asset.symbol?.toUpperCase();
      const pairSymbol = asset.pairSymbol?.toUpperCase();
      if (!symbol || !pairSymbol || bySymbol.has(symbol)) return;

      bySymbol.set(symbol, {
        name: asset.name ?? symbol,
        symbol,
        pairSymbol,
        currentPriceUsd: asset.currentPriceUsd,
        priceChangePercentage24h: asset.priceChangePercentage24h,
      });
    });

    return [...bySymbol.values()];
  }, [assets]);

  const loadAlerts = useCallback(async () => {
    if (!isAuthenticated) {
      setAlerts([]);
      return;
    }

    setLoading(true);
    setError("");
    try {
      setAlerts((await getPriceAlerts()) ?? []);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, t, "errors.priceAlertsLoad"));
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated, t]);

  useEffect(() => {
    loadAlerts();
  }, [loadAlerts]);

  function openCreateDialog() {
    if (!isAuthenticated) {
      openAuth({
        kind: "price-alert.create",
        title: t("guest.market.alertTitle"),
        description: t("guest.market.alertDescription"),
      }, () => setDialog({ mode: "create", alert: null }));
      return;
    }
    setDialog({ mode: "create", alert: null });
  }

  async function openEditDialog(alertId) {
    setPendingId(alertId);
    setError("");
    try {
      const alert = await getPriceAlert(alertId);
      setDialog({ mode: "edit", alert });
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, t, "errors.priceAlertDetail"));
    } finally {
      setPendingId(null);
    }
  }

  async function saveAlert(payload) {
    if (dialog?.mode === "edit") {
      const saved = await updatePriceAlert(dialog.alert.id, payload);
      setAlerts((current) => current.map((item) => (item.id === saved.id ? saved : item)));
      return;
    }

    const saved = await createPriceAlert(payload);
    setAlerts((current) => [saved, ...current]);
  }

  async function toggleAlert(alert) {
    setPendingId(alert.id);
    setError("");
    try {
      const saved = await updatePriceAlert(alert.id, { active: !alert.active });
      setAlerts((current) => current.map((item) => (item.id === saved.id ? saved : item)));
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, t, "errors.priceAlertUpdate"));
    } finally {
      setPendingId(null);
    }
  }

  async function removeAlert(alert) {
    if (!window.confirm(t("market.alerts.deleteConfirm", { symbol: alert.pair?.baseSymbol }))) {
      return;
    }

    setPendingId(alert.id);
    setError("");
    try {
      await deletePriceAlert(alert.id);
      setAlerts((current) => current.filter((item) => item.id !== alert.id));
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, t, "errors.priceAlertDelete"));
    } finally {
      setPendingId(null);
    }
  }

  return (
    <>
      <section className="panel price-alerts-panel">
        <div className="panel-heading price-alerts-panel__heading">
          <div>
            <h2>
              <Bell size={19} />
              {t("market.alerts.title")}
            </h2>
            <p>{t("market.alerts.description")}</p>
          </div>
          <div className="price-alerts-panel__actions">
            {isAuthenticated ? (
              <button
                className="icon-button"
                type="button"
                aria-label={t("market.alerts.refresh")}
                title={t("market.alerts.refresh")}
                onClick={loadAlerts}
                disabled={loading}
              >
                <RefreshCw size={17} />
              </button>
            ) : null}
            <button
              className="button button--primary"
              type="button"
              onClick={openCreateDialog}
              disabled={isAuthenticated && availableAssets.length === 0}
            >
              <Plus size={17} />
              {t("market.alerts.add")}
            </button>
          </div>
        </div>

        {error ? (
          <p className="form-error price-alerts-panel__error" role="alert">
            {error}
          </p>
        ) : null}

        {!isAuthenticated ? (
          <div className="price-alerts-empty">
            <BellOff size={24} />
            <p>{t("market.alerts.guest")}</p>
            <button className="button button--ghost" type="button" onClick={openCreateDialog}>
              {t("common.signIn")}
            </button>
          </div>
        ) : loading ? (
          <div className="price-alerts-empty">
            <p>{t("market.alerts.loading")}</p>
          </div>
        ) : alerts.length === 0 ? (
          <div className="price-alerts-empty">
            <BellOff size={24} />
            <p>{t("market.alerts.empty")}</p>
          </div>
        ) : (
          <div className="price-alert-list">
            {alerts.map((alert) => {
              const symbol = alert.pair?.baseSymbol ?? alert.pair?.symbol ?? "--";
              const isPending = pendingId === alert.id;

              return (
                <article className="price-alert-row" key={alert.id}>
                  <div className="price-alert-row__identity">
                    <strong>{symbol}</strong>
                    <span>{alert.pair?.quoteSymbol ? `/${alert.pair.quoteSymbol}` : ""}</span>
                  </div>
                  <div className="price-alert-row__rule">
                    <span>{t(`market.alerts.condition.${alert.conditionType}`)}</span>
                    <strong>{formatTarget(alert)}</strong>
                  </div>
                  <div className="price-alert-row__meta">
                    <div>
                      <span
                        className={`price-alert-status ${
                          alert.active ? "price-alert-status--active" : ""
                        }`}
                      >
                        {t(alert.active ? "market.alerts.enabled" : "market.alerts.paused")}
                      </span>
                      <small>{formatAlertActivity(alert, t)}</small>
                      <small>
                        {t("market.alerts.updated", {
                          time: formatRelativeTime(alert.updatedAt ?? alert.createdAt),
                        })}
                      </small>
                    </div>
                  </div>
                  <div className="price-alert-row__actions">
                    <label
                      className="price-alert-switch"
                      title={t(alert.active ? "market.alerts.pause" : "market.alerts.enable")}
                    >
                      <input
                        type="checkbox"
                        checked={alert.active}
                        onChange={() => toggleAlert(alert)}
                        disabled={isPending}
                        aria-label={t(
                          alert.active ? "market.alerts.pause" : "market.alerts.enable",
                        )}
                      />
                      <span />
                    </label>
                    <button
                      className="icon-button"
                      type="button"
                      aria-label={t("market.alerts.edit", { symbol })}
                      title={t("market.alerts.edit", { symbol })}
                      onClick={() => openEditDialog(alert.id)}
                      disabled={isPending}
                    >
                      <Pencil size={16} />
                    </button>
                    <button
                      className="icon-button price-alert-row__delete"
                      type="button"
                      aria-label={t("market.alerts.delete", { symbol })}
                      title={t("market.alerts.delete", { symbol })}
                      onClick={() => removeAlert(alert)}
                      disabled={isPending}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>

      {dialog ? (
        <PriceAlertDialog
          key={`${dialog.mode}:${dialog.alert?.id ?? "new"}`}
          alert={dialog.alert}
          assets={availableAssets}
          onClose={() => setDialog(null)}
          onSave={saveAlert}
        />
      ) : null}
    </>
  );
}
