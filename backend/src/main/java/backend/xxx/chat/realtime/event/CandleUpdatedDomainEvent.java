package backend.xxx.chat.realtime.event;

import backend.xxx.chat.market.redis.model.MarketLiveCandleHash;

public record CandleUpdatedDomainEvent(
        MarketLiveCandleHash candle
) {
}
