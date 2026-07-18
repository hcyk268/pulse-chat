package backend.xxx.chat.market.model;

import java.math.BigDecimal;
import java.time.Instant;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import backend.xxx.chat.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "price_alerts")
@NoArgsConstructor
@AllArgsConstructor
public class PriceAlert extends AbstractBaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private MarketAsset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pair_id")
    private MarketPair pair;

    @Column(name = "condition_type", nullable = false, length = 20)
    private String conditionType;

    @Column(name = "target_price", precision = 30, scale = 12)
    private BigDecimal targetPrice;

    @Column(name = "target_percent", precision = 20, scale = 8)
    private BigDecimal targetPercent;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "triggered_at")
    private Instant triggeredAt;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "last_triggered_price", precision = 30, scale = 12)
    private BigDecimal lastTriggeredPrice;
}
