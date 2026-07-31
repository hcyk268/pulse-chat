package backend.xxx.chat.market.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.common.model.AbstractBaseEntity;
import backend.xxx.chat.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 20)
    private PriceAlertConditionType conditionType;

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

    public static PriceAlert create(
            User user,
            MarketAsset asset,
            MarketPair pair,
            PriceAlertConditionType conditionType,
            BigDecimal targetPrice,
            BigDecimal targetPercent,
            boolean active
    ) {
        PriceAlert alert = new PriceAlert();
        alert.user = Objects.requireNonNull(user, "price-alert.user.required");
        alert.changeMarket(asset, pair);
        alert.changeRule(conditionType, targetPrice, targetPercent);
        alert.active = active;
        return alert;
    }

    public void changeMarket(MarketAsset asset, MarketPair pair) {
        this.asset = Objects.requireNonNull(asset, "price-alert.asset.required");
        this.pair = Objects.requireNonNull(pair, "price-alert.pair.required");
        resetTriggerState();
    }

    public void patchRule(
            PriceAlertConditionType requestedConditionType,
            BigDecimal requestedTargetPrice,
            BigDecimal requestedTargetPercent
    ) {
        PriceAlertConditionType nextConditionType = requestedConditionType == null
                ? conditionType
                : requestedConditionType;

        if (nextConditionType == PriceAlertConditionType.CHANGE_PERCENT) {
            BigDecimal nextTargetPercent = requestedTargetPercent != null
                    ? requestedTargetPercent
                    : currentTargetPercent();
            changeRule(nextConditionType, requestedTargetPrice, nextTargetPercent);
            return;
        }

        BigDecimal nextTargetPrice = requestedTargetPrice != null
                ? requestedTargetPrice
                : currentTargetPrice();
        changeRule(nextConditionType, nextTargetPrice, requestedTargetPercent);
    }

    public void changeRule(
            PriceAlertConditionType conditionType,
            BigDecimal targetPrice,
            BigDecimal targetPercent
    ) {
        if (conditionType == null) {
            throw new ValidationException("market.price-alert.condition-type.required");
        }

        if (conditionType == PriceAlertConditionType.CHANGE_PERCENT) {
            rejectPresent(targetPrice, "market.price-alert.target-price.not.allowed");
            requirePositive(
                    targetPercent,
                    "market.price-alert.target-percent.required",
                    "market.price-alert.target-percent.positive"
            );
            this.conditionType = conditionType;
            this.targetPrice = null;
            this.targetPercent = targetPercent;
            resetTriggerState();
            return;
        }

        rejectPresent(targetPercent, "market.price-alert.target-percent.not.allowed");
        requirePositive(
                targetPrice,
                "market.price-alert.target-price.required",
                "market.price-alert.target-price.positive"
        );
        this.conditionType = conditionType;
        this.targetPrice = targetPrice;
        this.targetPercent = null;
        resetTriggerState();
    }

    public void changeActive(boolean active) {
        if (active && !this.active) {
            resetTriggerState();
        }
        this.active = active;
    }

    public boolean markTriggered(BigDecimal price, Instant triggeredAt) {
        if (!active) {
            return false;
        }
        if (price == null || price.signum() <= 0) {
            throw new ValidationException("market.price-alert.current-price.positive");
        }
        Instant checkedAt = Objects.requireNonNull(triggeredAt, "price-alert.triggered-at.required");
        this.lastCheckedAt = checkedAt;
        this.triggeredAt = checkedAt;
        this.lastTriggeredPrice = price;
        this.active = false;
        return true;
    }

    private BigDecimal currentTargetPrice() {
        if (conditionType == PriceAlertConditionType.CHANGE_PERCENT) {
            return null;
        }
        return targetPrice;
    }

    private BigDecimal currentTargetPercent() {
        if (conditionType != PriceAlertConditionType.CHANGE_PERCENT) {
            return null;
        }
        return targetPercent;
    }

    private void resetTriggerState() {
        this.triggeredAt = null;
        this.lastCheckedAt = null;
        this.lastTriggeredPrice = null;
    }

    private void requirePositive(BigDecimal value, String requiredMessage, String positiveMessage) {
        if (value == null) {
            throw new ValidationException(requiredMessage);
        }
        if (value.signum() <= 0) {
            throw new ValidationException(positiveMessage);
        }
    }

    private void rejectPresent(BigDecimal value, String message) {
        if (value != null) {
            throw new ValidationException(message);
        }
    }
}
