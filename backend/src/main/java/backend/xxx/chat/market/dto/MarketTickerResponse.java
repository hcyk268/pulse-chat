package backend.xxx.chat.market.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import backend.xxx.chat.market.model.MarketCandle;
import backend.xxx.chat.market.redis.model.MarketTickerLatestHash;

public record MarketTickerResponse(
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
        Instant updatedAt,
        List<CandleResponse> candles,
        Map<String, List<CandleResponse>> candlesByInterval
) {

    public static MarketTickerResponse from(MarketTickerLatestHash ticker) {
        return from(ticker, List.of(), Map.of());
    }

    public static MarketTickerResponse from(
            MarketTickerLatestHash ticker,
            List<CandleResponse> candles,
            Map<String, List<CandleResponse>> candlesByInterval
    ) {
        return new MarketTickerResponse(
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
                ticker.getUpdatedAt(),
                candles,
                candlesByInterval
        );
    }

    public record CandleResponse(
            Long id,
            Long pairId,
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
            boolean closed
    ) {

        public static CandleResponse from(MarketCandle candle) {
            return new CandleResponse(
                    candle.getId(),
                    candle.getPair().getId(),
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
                    candle.isClosed()
            );
        }
    }
}