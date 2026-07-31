package backend.xxx.chat.market.service;

import java.math.BigDecimal;

import backend.xxx.chat.market.model.PriceAlertConditionType;
import backend.xxx.chat.market.redis.model.MarketTickerLatestHash;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PriceAlertRuleTest {

    @Test
    void matchesPriceThresholds() {
        MarketTickerLatestHash ticker = ticker("100", "2.5");

        assertThat(rule(PriceAlertConditionType.ABOVE, "100", null).matches(ticker)).isTrue();
        assertThat(rule(PriceAlertConditionType.BELOW, "100", null).matches(ticker)).isTrue();
        assertThat(rule(PriceAlertConditionType.ABOVE, "101", null).matches(ticker)).isFalse();
        assertThat(rule(PriceAlertConditionType.BELOW, "99", null).matches(ticker)).isFalse();
    }

    @Test
    void matchesPercentageChange() {
        MarketTickerLatestHash ticker = ticker("100", "-5.25");

        assertThat(rule(PriceAlertConditionType.CHANGE_PERCENT, null, "5").matches(ticker)).isTrue();
        assertThat(rule(PriceAlertConditionType.CHANGE_PERCENT, null, "6").matches(ticker)).isFalse();
    }

    @Test
    void rejectsMissingPrice() {
        MarketTickerLatestHash ticker = ticker(null, "10");

        assertThat(rule(PriceAlertConditionType.CHANGE_PERCENT, null, "5").matches(ticker)).isFalse();
    }

    private PriceAlertRule rule(
            PriceAlertConditionType type,
            String targetPrice,
            String targetPercent
    ) {
        return new PriceAlertRule(
                1L,
                10L,
                type,
                decimal(targetPrice),
                decimal(targetPercent)
        );
    }

    private MarketTickerLatestHash ticker(String price, String changePercent) {
        MarketTickerLatestHash ticker = new MarketTickerLatestHash();
        ticker.setPairId(10L);
        ticker.setPrice(decimal(price));
        ticker.setPriceChangePercent(decimal(changePercent));
        return ticker;
    }

    private BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
