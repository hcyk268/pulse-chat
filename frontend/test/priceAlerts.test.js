import test from "node:test";
import assert from "node:assert/strict";
import {
  buildPriceAlertRequest,
  createPriceAlertForm,
  getPriceAlertMode,
  validatePriceAlertForm,
} from "../src/domain/market/priceAlerts.js";

test("builds a price alert without the percent-only field", () => {
  assert.deepEqual(
    buildPriceAlertRequest({
      symbol: " btc ",
      conditionType: "ABOVE",
      targetValue: "65000.25",
      active: true,
    }),
    {
      symbol: "BTC",
      conditionType: "ABOVE",
      targetPrice: 65000.25,
      active: true,
    },
  );
});

test("builds a percent alert without the price-only field", () => {
  assert.deepEqual(
    buildPriceAlertRequest({
      symbol: "eth",
      conditionType: "CHANGE_PERCENT",
      targetValue: "5",
      active: false,
    }),
    {
      symbol: "ETH",
      conditionType: "CHANGE_PERCENT",
      targetPercent: 5,
      active: false,
    },
  );
});

test("hydrates edit forms and validates required positive values", () => {
  const form = createPriceAlertForm({
    pair: { baseSymbol: "SOL" },
    conditionType: "BELOW",
    targetPrice: 120,
    active: false,
  });

  assert.deepEqual(form, {
    symbol: "SOL",
    conditionType: "BELOW",
    targetValue: "120",
    active: false,
  });
  assert.equal(validatePriceAlertForm(form), "");
  assert.equal(validatePriceAlertForm({ ...form, symbol: " " }), "market.alerts.validation.symbol");
  assert.equal(
    validatePriceAlertForm({ ...form, targetValue: "0" }),
    "market.alerts.validation.price",
  );
});
test("maps endpoint conditions to the two UI modes", () => {
  assert.equal(getPriceAlertMode("ABOVE"), "PRICE");
  assert.equal(getPriceAlertMode("BELOW"), "PRICE");
  assert.equal(getPriceAlertMode("CHANGE_PERCENT"), "PERCENT");
});

test("rejects symbols missing from the supported market response", () => {
  const form = {
    symbol: "UNKNOWN",
    conditionType: "ABOVE",
    targetValue: "100",
    active: true,
  };

  assert.equal(
    validatePriceAlertForm(form, ["BTC", "ETH"]),
    "market.alerts.validation.unsupportedSymbol",
  );
});
