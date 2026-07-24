package backend.xxx.chat.config.properties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.market.stream")
public class BinanceStreamProperties {

    private boolean enabled = true;

    private String baseUrl = "wss://stream.binance.com:9443/ws";

    private long initialDelayMs = 15_000;

    private long reconnectDelayMs = 60_000;

    private int maxPairs = 200;

    private List<String> candleIntervals = new ArrayList<>(List.of("4h", "1d", "1w"));

    private Duration tickerCacheTtl = Duration.ofMinutes(10);

    private Duration tickerSymbolsCacheTtl = Duration.ofMinutes(5);

    private Duration candleCacheTtl = Duration.ofDays(8);

    public void setInitialDelayMs(long initialDelayMs) {
        if (initialDelayMs < 0) {
            throw new IllegalArgumentException("app.market.stream.initial-delay-ms must not be negative");
        }
        this.initialDelayMs = initialDelayMs;
    }

    public void setReconnectDelayMs(long reconnectDelayMs) {
        if (reconnectDelayMs <= 0) {
            throw new IllegalArgumentException("app.market.stream.reconnect-delay-ms must be positive");
        }
        this.reconnectDelayMs = reconnectDelayMs;
    }

    public void setMaxPairs(int maxPairs) {
        if (maxPairs <= 0) {
            throw new IllegalArgumentException("app.market.stream.max-pairs must be positive");
        }
        this.maxPairs = maxPairs;
    }
    public void setTickerSymbolsCacheTtl(Duration tickerSymbolsCacheTtl) {
        if (tickerSymbolsCacheTtl == null || tickerSymbolsCacheTtl.isZero() || tickerSymbolsCacheTtl.isNegative()) {
            throw new IllegalArgumentException("app.market.stream.ticker-symbols-cache-ttl must be positive");
        }
        this.tickerSymbolsCacheTtl = tickerSymbolsCacheTtl;
    }
}