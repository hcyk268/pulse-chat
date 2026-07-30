package backend.xxx.chat.market.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
        List<MarketCandleResponse> candles,
        Map<String, List<MarketCandleResponse>> candlesByInterval
) {
}
