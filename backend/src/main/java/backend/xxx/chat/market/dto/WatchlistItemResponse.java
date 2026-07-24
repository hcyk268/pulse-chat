package backend.xxx.chat.market.dto;

import java.math.BigDecimal;
import java.time.Instant;

import backend.xxx.chat.market.model.MarketAsset;
import backend.xxx.chat.market.model.UserWatchlistItem;

public record WatchlistItemResponse(
        Long id,
        AssetResponse asset,
        Instant createdAt,
        Instant updatedAt
) {

    public static WatchlistItemResponse from(UserWatchlistItem item) {
        return new WatchlistItemResponse(
                item.getId(),
                AssetResponse.from(item.getAsset()),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    public record AssetResponse(
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

        public static AssetResponse from(MarketAsset asset) {
            return new AssetResponse(
                    asset.getId(),
                    asset.getCoingeckoId(),
                    asset.getSymbol(),
                    asset.getName(),
                    asset.getImageUrl(),
                    asset.getMarketCapRank(),
                    asset.getCurrentPriceUsd(),
                    asset.getPriceChangePercentage24h(),
                    asset.getHigh24h(),
                    asset.getLow24h(),
                    asset.getMarketCap(),
                    asset.getTotalVolume(),
                    asset.getLastSyncedAt()
            );
        }
    }
}