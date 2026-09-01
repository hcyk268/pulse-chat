export const PRICE_ALERT_CONDITIONS = ["ABOVE", "BELOW", "CHANGE_PERCENT"];
export const PRICE_ALERT_MODES = ["PRICE", "PERCENT"];
export const PRICE_ALERT_DIRECTIONS = ["ABOVE", "BELOW"];

export function getPriceAlertMode(conditionType) {
  return conditionType === "CHANGE_PERCENT" ? "PERCENT" : "PRICE";
}

export function createPriceAlertForm(alert) {
  const conditionType = PRICE_ALERT_CONDITIONS.includes(alert?.conditionType)
    ? alert.conditionType
    : PRICE_ALERT_DIRECTIONS[0];

  return {
    symbol: alert?.pair?.baseSymbol ?? "",
    conditionType,
    targetValue:
      conditionType === "CHANGE_PERCENT"
        ? String(alert?.targetPercent ?? "")
        : String(alert?.targetPrice ?? ""),
    active: alert?.active ?? true,
  };
}

export function validatePriceAlertForm(form, supportedSymbols = []) {
  const symbol = form.symbol.trim().toUpperCase();
  if (!symbol) return "market.alerts.validation.symbol";
  if (supportedSymbols.length > 0 && !supportedSymbols.includes(symbol)) {
    return "market.alerts.validation.unsupportedSymbol";
  }

  const targetValue = Number(form.targetValue);
  if (!Number.isFinite(targetValue) || targetValue <= 0) {
    return form.conditionType === "CHANGE_PERCENT"
      ? "market.alerts.validation.percent"
      : "market.alerts.validation.price";
  }

  return "";
}

export function buildPriceAlertRequest(form) {
  const request = {
    symbol: form.symbol.trim().toUpperCase(),
    conditionType: form.conditionType,
    active: Boolean(form.active),
  };
  const targetValue = Number(form.targetValue);

  if (form.conditionType === "CHANGE_PERCENT") {
    request.targetPercent = targetValue;
  } else {
    request.targetPrice = targetValue;
  }

  return request;
}
