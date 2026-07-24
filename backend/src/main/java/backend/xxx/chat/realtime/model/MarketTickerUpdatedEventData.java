package backend.xxx.chat.realtime.model;

import java.math.BigDecimal;
import java.time.Instant;

import backend.xxx.chat.market.redis.model.MarketTickerLatestHash;

public record MarketTickerUpdatedEventData(
        Long pairId,
        String exchange,
        String symbol,
        BigDecimal price,
        BigDecimal bidPrice,
        BigDecimal askPrice,
        BigDecimal high24h,
        BigDecimal low24h,
        BigDecimal volume24h,
        BigDecimal quoteVolume24h,
        BigDecimal priceChange,
        BigDecimal priceChangePercent,
        Instant eventTime,
        Instant updatedAt
) {

    public static MarketTickerUpdatedEventData from(MarketTickerLatestHash ticker) {
        return new MarketTickerUpdatedEventData(
                ticker.getPairId(),
                ticker.getExchange(),
                ticker.getSymbol(),
                ticker.getPrice(),
                ticker.getBidPrice(),
                ticker.getAskPrice(),
                ticker.getHigh24h(),
                ticker.getLow24h(),
                ticker.getVolume24h(),
                ticker.getQuoteVolume24h(),
                ticker.getPriceChange(),
                ticker.getPriceChangePercent(),
                ticker.getEventTime(),
                ticker.getUpdatedAt()
        );
    }
}
