package backend.xxx.chat.market.model;

import java.math.BigDecimal;

import backend.xxx.chat.market.redis.model.MarketTickerLatestHash;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class PriceAlertRule {

    private Long alertId;
    private Long pairId;
    private PriceAlertConditionType conditionType;
    private BigDecimal targetPrice;
    private BigDecimal targetPercent;

    public static PriceAlertRule createFrom(PriceAlert alert) {
        return new PriceAlertRule(
                alert.getId(),
                alert.getPair().getId(),
                alert.getConditionType(),
                alert.getTargetPrice(),
                alert.getTargetPercent()
        );
    }

    public boolean matches(MarketTickerLatestHash ticker) {
        if (ticker == null || ticker.getPrice() == null || ticker.getPrice().signum() <= 0) {
            return false;
        }

        return switch (conditionType) {
            case ABOVE -> targetPrice != null && ticker.getPrice().compareTo(targetPrice) >= 0;
            case BELOW -> targetPrice != null && ticker.getPrice().compareTo(targetPrice) <= 0;
            case CHANGE_PERCENT -> ticker.getPriceChangePercent() != null
                    && targetPercent != null
                    && ticker.getPriceChangePercent().abs().compareTo(targetPercent) >= 0;
        };
    }
}
