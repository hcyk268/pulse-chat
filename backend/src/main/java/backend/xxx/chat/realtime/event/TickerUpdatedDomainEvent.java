package backend.xxx.chat.realtime.event;

import backend.xxx.chat.market.redis.model.MarketTickerLatestHash;

public record TickerUpdatedDomainEvent(
        MarketTickerLatestHash ticker
) {
}
