package backend.xxx.chat.market.dto;

import java.math.BigDecimal;
import java.time.Instant;

import backend.xxx.chat.market.model.PriceAlertConditionType;

public record PriceAlertResponse(
        Long id,
        MarketPairResponse pair,
        PriceAlertConditionType conditionType,
        BigDecimal targetPrice,
        BigDecimal targetPercent,
        boolean active,
        Instant triggeredAt,
        Instant lastCheckedAt,
        BigDecimal lastTriggeredPrice,
        Instant createdAt,
        Instant updatedAt
) {
}
