package backend.xxx.chat.market.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CoinTrendingResponse(
        List<CoinTrendingEntry> coins
) {

    public List<CoinTrendingEntry> coins() {
        return coins == null ? List.of() : coins;
    }

    public record CoinTrendingEntry(
            CoinTrendingItem item
    ) {
    }

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
}
