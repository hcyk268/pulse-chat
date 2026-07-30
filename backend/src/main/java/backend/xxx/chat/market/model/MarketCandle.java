package backend.xxx.chat.market.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.common.model.AbstractBaseEntity;
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
@Table(name = "market_candles")
@NoArgsConstructor
@AllArgsConstructor
public class MarketCandle extends AbstractBaseEntity<Long> {

    private static final int INTERVAL_MAX_LENGTH = 10;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pair_id", nullable = false)
    private MarketPair pair;

    @Column(name = "interval_name", nullable = false, length = INTERVAL_MAX_LENGTH)
    private String intervalName;

    @Column(name = "open_time", nullable = false)
    private Instant openTime;

    @Column(name = "close_time", nullable = false)
    private Instant closeTime;

    @Column(name = "open", nullable = false, precision = 30, scale = 12)
    private BigDecimal open;

    @Column(name = "high", nullable = false, precision = 30, scale = 12)
    private BigDecimal high;

    @Column(name = "low", nullable = false, precision = 30, scale = 12)
    private BigDecimal low;

    @Column(name = "close", nullable = false, precision = 30, scale = 12)
    private BigDecimal close;

    @Column(name = "volume", nullable = false, precision = 30, scale = 12)
    private BigDecimal volume;

    @Column(name = "quote_volume", nullable = false, precision = 30, scale = 12)
    private BigDecimal quoteVolume;

    @Column(name = "trade_count", nullable = false)
    private Long tradeCount = 0L;

    @Column(name = "is_closed", nullable = false)
    private boolean closed = true;

    public static MarketCandle create(MarketPair pair, String intervalName, Instant openTime) {
        MarketCandle candle = new MarketCandle();
        candle.pair = Objects.requireNonNull(pair, "market.candle.pair.required");
        candle.intervalName = requireInterval(intervalName);
        candle.openTime = Objects.requireNonNull(openTime, "market.candle.open-time.required");
        return candle;
    }

    public void updateOhlcv(
            Instant closeTime,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume,
            BigDecimal quoteVolume,
            long tradeCount,
            boolean closed
    ) {
        Instant nextCloseTime = Objects.requireNonNull(closeTime, "market.candle.close-time.required");
        if (openTime != null && nextCloseTime.isBefore(openTime)) {
            throw new ValidationException("market.candle.close-time.before-open-time");
        }

        BigDecimal nextOpen = requirePositive(open, "market.candle.open");
        BigDecimal nextHigh = requirePositive(high, "market.candle.high");
        BigDecimal nextLow = requirePositive(low, "market.candle.low");
        BigDecimal nextClose = requirePositive(close, "market.candle.close");
        validatePriceRange(nextOpen, nextHigh, nextLow, nextClose);

        this.closeTime = nextCloseTime;
        this.open = nextOpen;
        this.high = nextHigh;
        this.low = nextLow;
        this.close = nextClose;
        this.volume = requireNonNegative(volume, "market.candle.volume");
        this.quoteVolume = requireNonNegative(quoteVolume, "market.candle.quote-volume");
        this.tradeCount = requireNonNegative(tradeCount, "market.candle.trade-count");
        this.closed = closed;
    }

    private static String requireInterval(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("market.candle.interval.required");
        }
        String trimmedValue = value.trim();
        if (trimmedValue.length() > INTERVAL_MAX_LENGTH) {
            throw new ValidationException("market.candle.interval.max.length");
        }
        return trimmedValue;
    }

    private BigDecimal requirePositive(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + ".required");
        }
        if (value.signum() <= 0) {
            throw new ValidationException(fieldName + ".positive");
        }
        return value;
    }

    private BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + ".required");
        }
        if (value.signum() < 0) {
            throw new ValidationException(fieldName + ".non-negative");
        }
        return value;
    }

    private Long requireNonNegative(long value, String fieldName) {
        if (value < 0) {
            throw new ValidationException(fieldName + ".non-negative");
        }
        return value;
    }

    private void validatePriceRange(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {
        if (high.compareTo(low) < 0
                || high.compareTo(open) < 0
                || high.compareTo(close) < 0
                || low.compareTo(open) > 0
                || low.compareTo(close) > 0) {
            throw new ValidationException("market.candle.ohlc.range.invalid");
        }
    }
}