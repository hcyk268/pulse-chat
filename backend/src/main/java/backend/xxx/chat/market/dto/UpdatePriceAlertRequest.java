package backend.xxx.chat.market.dto;

import java.math.BigDecimal;

import backend.xxx.chat.market.model.PriceAlertConditionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record UpdatePriceAlertRequest(
        @Size(max = 20, message = "market.price-alert.symbol.max.length")
        String symbol,

        PriceAlertConditionType conditionType,

        @DecimalMin(value = "0.0", inclusive = false, message = "market.price-alert.target-price.positive")
        BigDecimal targetPrice,

        @DecimalMin(value = "0.0", inclusive = false, message = "market.price-alert.target-percent.positive")
        BigDecimal targetPercent,

        Boolean active
) {
}
