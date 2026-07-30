package backend.xxx.chat.market.dto;

import java.math.BigDecimal;

import backend.xxx.chat.market.model.PriceAlertConditionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePriceAlertRequest(
        @NotBlank(message = "market.price-alert.symbol.blank")
        @Size(max = 20, message = "market.price-alert.symbol.max.length")
        String symbol,

        @NotNull(message = "market.price-alert.condition-type.required")
        PriceAlertConditionType conditionType,

        @DecimalMin(value = "0.0", inclusive = false, message = "market.price-alert.target-price.positive")
        BigDecimal targetPrice,

        @DecimalMin(value = "0.0", inclusive = false, message = "market.price-alert.target-percent.positive")
        BigDecimal targetPercent,

        Boolean active
) {
}
