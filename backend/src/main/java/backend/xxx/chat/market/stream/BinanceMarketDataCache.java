package backend.xxx.chat.market.stream;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import backend.xxx.chat.config.properties.BinanceStreamProperties;
import backend.xxx.chat.market.redis.model.MarketLiveCandleHash;
import backend.xxx.chat.market.redis.model.MarketStreamStateHash;
import backend.xxx.chat.market.redis.model.MarketTickerLatestHash;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BinanceMarketDataCache {

    private static final String TICKER_KEY_PREFIX = "ticker:binance:";
    private static final String CANDLE_KEY_PREFIX = "kline:binance:";
    private static final String TICKER_SYMBOLS_KEY = "tickers:binance:symbols";
    private static final String STREAM_STATE_KEY = "stream:binance:state";

    private final RedisTemplate<String, Object> redisTemplate;
    private final BinanceStreamProperties properties;

    public void cacheTicker(MarketTickerLatestHash ticker) {
        redisTemplate.opsForValue().set(
                tickerKey(ticker.getSymbol()),
                ticker,
                properties.getTickerCacheTtl()
        );
    }

    public Optional<MarketTickerLatestHash> getTicker(String symbol) {
        Object cached = redisTemplate.opsForValue().get(tickerKey(symbol));
        if (cached instanceof MarketTickerLatestHash ticker) {
            return Optional.of(ticker);
        }

        return Optional.empty();
    }

    public List<MarketTickerLatestHash> getTickers(Collection<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return List.of();
        }

        List<String> keys = symbols.stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .map(this::tickerKey)
                .toList();
        if (keys.isEmpty()) {
            return List.of();
        }

        List<Object> cachedValues = redisTemplate.opsForValue().multiGet(keys);
        if (cachedValues == null || cachedValues.isEmpty()) {
            return List.of();
        }

        return cachedValues.stream()
                .filter(MarketTickerLatestHash.class::isInstance)
                .map(MarketTickerLatestHash.class::cast)
                .toList();
    }

    public Optional<List<String>> getTickerSymbols() {
        Object cached = redisTemplate.opsForValue().get(TICKER_SYMBOLS_KEY);
        if (cached instanceof List<?> values) {
            return Optional.of(values.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList());
        }

        return Optional.empty();
    }

    public void cacheTickerSymbols(List<String> symbols) {
        redisTemplate.opsForValue().set(
                TICKER_SYMBOLS_KEY,
                symbols == null ? List.of() : symbols,
                properties.getTickerSymbolsCacheTtl()
        );
    }
    public void cacheCandle(MarketLiveCandleHash candle) {
        redisTemplate.opsForValue().set(
                CANDLE_KEY_PREFIX + candle.getSymbol() + ":" + candle.getIntervalName(),
                candle,
                properties.getCandleCacheTtl()
        );
    }

    public void markConnected(int activeSymbolCount) {
        MarketStreamStateHash state = new MarketStreamStateHash();
        state.setExchange("BINANCE");
        state.setConnected(true);
        state.setLastHeartbeatAt(Instant.now());
        state.setActiveSymbolCount(activeSymbolCount);
        state.setUpdatedAt(Instant.now());
        redisTemplate.opsForValue().set(STREAM_STATE_KEY, state);
    }

    public void markTickerEvent() {
        MarketStreamStateHash state = currentState();
        state.setLastTickerEventAt(Instant.now());
        state.setLastHeartbeatAt(Instant.now());
        state.setUpdatedAt(Instant.now());
        redisTemplate.opsForValue().set(STREAM_STATE_KEY, state);
    }

    public void markKlineEvent() {
        MarketStreamStateHash state = currentState();
        state.setLastKlineEventAt(Instant.now());
        state.setLastHeartbeatAt(Instant.now());
        state.setUpdatedAt(Instant.now());
        redisTemplate.opsForValue().set(STREAM_STATE_KEY, state);
    }

    public void markDisconnected(String errorMessage) {
        MarketStreamStateHash state = currentState();
        state.setConnected(false);
        state.setLastErrorMessage(errorMessage);
        state.setUpdatedAt(Instant.now());
        redisTemplate.opsForValue().set(STREAM_STATE_KEY, state);
    }

    private MarketStreamStateHash currentState() {
        Object cached = redisTemplate.opsForValue().get(STREAM_STATE_KEY);
        if (cached instanceof MarketStreamStateHash state) {
            return state;
        }

        MarketStreamStateHash state = new MarketStreamStateHash();
        state.setExchange("BINANCE");
        return state;
    }

    private String tickerKey(String symbol) {
        return TICKER_KEY_PREFIX + symbol.toUpperCase(Locale.ROOT);
    }
}
