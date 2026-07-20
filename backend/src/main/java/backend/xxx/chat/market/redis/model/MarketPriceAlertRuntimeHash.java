package backend.xxx.chat.market.redis.model;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@RedisHash("price_alert_runtime")
@NoArgsConstructor
@AllArgsConstructor
public class MarketPriceAlertRuntimeHash {

    @Id
    private Long alertId;

    private Long userId;
    private Long assetId;
    private Long pairId;
    private String conditionType;
    private BigDecimal targetPrice;
    private BigDecimal targetPercent;
    private BigDecimal lastCheckedPrice;
    private Instant lastTriggeredAt;
    private boolean active;
    private Instant updatedAt;
}
