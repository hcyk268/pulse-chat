package backend.xxx.chat.market.client.dto;

import java.util.List;

public record CoinTrendingResponse(
        List<CoinTrendingEntry> coins
) {

    public List<CoinTrendingEntry> coins() {
        return coins == null ? List.of() : coins;
    }
}
