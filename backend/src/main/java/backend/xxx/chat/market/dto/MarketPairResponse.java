package backend.xxx.chat.market.dto;

import java.time.Instant;

public record MarketPairResponse(
        Long id,
        String exchange,
        String baseSymbol,
        String quoteSymbol,
        String symbol,
        Instant lastSyncedAt
) {
}
