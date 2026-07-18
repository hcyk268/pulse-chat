package backend.xxx.chat.market.model;

import java.math.BigDecimal;
import java.time.Instant;

import backend.xxx.chat.common.model.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pair_id", nullable = false)
    private MarketPair pair;

    @Column(name = "interval_name", nullable = false, length = 10)
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
}
