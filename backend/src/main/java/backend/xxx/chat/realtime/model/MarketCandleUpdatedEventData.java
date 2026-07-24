package backend.xxx.chat.realtime.model;

import java.math.BigDecimal;
import java.time.Instant;

import backend.xxx.chat.market.redis.model.MarketLiveCandleHash;

public record MarketCandleUpdatedEventData(
        Long pairId,
        String exchange,
        String symbol,
        String intervalName,
        Instant openTime,
        Instant closeTime,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume,
        BigDecimal quoteVolume,
        Long tradeCount,
        boolean closed,
        Instant updatedAt
) {

    public static MarketCandleUpdatedEventData from(MarketLiveCandleHash candle) {
        return new MarketCandleUpdatedEventData(
                candle.getPairId(),
                candle.getExchange(),
                candle.getSymbol(),
                candle.getIntervalName(),
                candle.getOpenTime(),
                candle.getCloseTime(),
                candle.getOpen(),
                candle.getHigh(),
                candle.getLow(),
                candle.getClose(),
                candle.getVolume(),
                candle.getQuoteVolume(),
                candle.getTradeCount(),
                candle.isClosed(),
                candle.getUpdatedAt()
        );
    }
}
