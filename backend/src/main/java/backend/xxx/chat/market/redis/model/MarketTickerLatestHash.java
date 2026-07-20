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
@RedisHash("market_ticker_latest")
@NoArgsConstructor
@AllArgsConstructor
public class MarketTickerLatestHash {

    @Id
    private String symbol;

    private Long pairId;
    private String exchange;
    private BigDecimal price;
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private BigDecimal high24h;
    private BigDecimal low24h;
    private BigDecimal volume24h;
    private BigDecimal quoteVolume24h;
    private BigDecimal priceChange;
    private BigDecimal priceChangePercent;
    private Instant eventTime;
    private Instant updatedAt;
}
