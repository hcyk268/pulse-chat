package backend.xxx.chat.market.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OverviewMarketResponse(
        List<CoinMarketItemResponse> coins,
        List<TrendingCoinResponse> trending
) {

    public record CoinMarketItemResponse(
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
}