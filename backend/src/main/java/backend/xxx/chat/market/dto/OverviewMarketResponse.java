package backend.xxx.chat.market.dto;

import java.util.List;

public record OverviewMarketResponse(
        List<CoinMarketItemResponse> coins,
        List<TrendingCoinResponse> trending
) {
}
