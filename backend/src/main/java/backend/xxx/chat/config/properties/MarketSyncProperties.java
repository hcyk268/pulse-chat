package backend.xxx.chat.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.market.sync")
public class MarketSyncProperties {

    private long marketsDelayMs = 1_800_000;

    private long trendingDelayMs = 3_600_000;

    private long initialDelayMs = 10_000;

    private int schedulerPoolSize = 1;

    public void setMarketsDelayMs(long marketsDelayMs) {
        if (marketsDelayMs <= 0) {
            throw new IllegalArgumentException("app.market.sync.markets-delay-ms must be positive");
        }
        this.marketsDelayMs = marketsDelayMs;
    }

    public void setTrendingDelayMs(long trendingDelayMs) {
        if (trendingDelayMs <= 0) {
            throw new IllegalArgumentException("app.market.sync.trending-delay-ms must be positive");
        }
        this.trendingDelayMs = trendingDelayMs;
    }

    public void setInitialDelayMs(long initialDelayMs) {
        if (initialDelayMs < 0) {
            throw new IllegalArgumentException("app.market.sync.initial-delay-ms must not be negative");
        }
        this.initialDelayMs = initialDelayMs;
    }

    public void setSchedulerPoolSize(int schedulerPoolSize) {
        if (schedulerPoolSize <= 0) {
            throw new IllegalArgumentException("app.market.sync.scheduler-pool-size must be positive");
        }
        this.schedulerPoolSize = schedulerPoolSize;
    }
}
