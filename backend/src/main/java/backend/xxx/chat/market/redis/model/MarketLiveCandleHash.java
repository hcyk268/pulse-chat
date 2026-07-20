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
@RedisHash("market_live_candles")
@NoArgsConstructor
@AllArgsConstructor
public class MarketLiveCandleHash {

    @Id
    private String id;

    private Long pairId;
    private String exchange;
    private String symbol;
    private String intervalName;
    private Instant openTime;
    private Instant closeTime;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    private BigDecimal quoteVolume;
    private Long tradeCount;
    private boolean closed;
    private Instant updatedAt;
}
