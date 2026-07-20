package backend.xxx.chat.market.client.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CoinMarketResponse(
        String id,
        String symbol,
        String name,
        String image,
        @JsonProperty("current_price")
        BigDecimal currentPrice,
        @JsonProperty("market_cap")
        BigDecimal marketCap,
        @JsonProperty("market_cap_rank")
        Integer marketCapRank,
        @JsonProperty("total_volume")
        BigDecimal totalVolume,
        @JsonProperty("high_24h")
        BigDecimal high24h,
        @JsonProperty("low_24h")
        BigDecimal low24h,
        @JsonProperty("price_change_percentage_24h")
        BigDecimal priceChangePercentage24h,
        @JsonProperty("circulating_supply")
        BigDecimal circulatingSupply,
        @JsonProperty("total_supply")
        BigDecimal totalSupply,
        @JsonProperty("max_supply")
        BigDecimal maxSupply
) {
}
