package backend.xxx.chat.market.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CoinDetailResponse(
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
        BigDecimal circulatingSupply,
        BigDecimal totalSupply,
        BigDecimal maxSupply,
        Instant lastSyncedAt,
        MarketPairResponse binancePair
) {

    public record MarketPairResponse(
            Long id,
            String exchange,
            String baseSymbol,
            String quoteSymbol,
            String symbol,
            Instant lastSyncedAt
    ) {
    }
}