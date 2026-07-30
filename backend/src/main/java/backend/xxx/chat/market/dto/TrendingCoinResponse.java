package backend.xxx.chat.market.dto;

import java.time.Instant;

public record TrendingCoinResponse(
        Long id,
        Long assetId,
        String coingeckoId,
        String symbol,
        String name,
        String thumbUrl,
        Integer marketCapRank,
        Integer score,
        Instant snapshotAt
) {
}
