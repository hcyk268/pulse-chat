package backend.xxx.chat.market.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WatchlistAssetResponse(
        Long id,
        String coingeckoId,
        String symbol,
        String name,
        String imageUrl,
        Integer marketCapRank,
        BigDecimal currentPriceUsd,
        BigDecimal priceChangePercentage24h,
        BigDecimal high24h,
        BigDecimal low24h,
        BigDecimal marketCap,
        BigDecimal totalVolume,
        Instant lastSyncedAt
) {
}
