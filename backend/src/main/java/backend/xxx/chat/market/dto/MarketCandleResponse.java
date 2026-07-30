package backend.xxx.chat.market.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketCandleResponse(
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
}
