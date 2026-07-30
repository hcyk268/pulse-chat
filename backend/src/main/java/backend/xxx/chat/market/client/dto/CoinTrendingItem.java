package backend.xxx.chat.market.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CoinTrendingItem(
        String id,
        @JsonProperty("coin_id")
        Integer coinId,
        String name,
        String symbol,
        @JsonProperty("market_cap_rank")
        Integer marketCapRank,
        String thumb,
        Integer score
) {
}
