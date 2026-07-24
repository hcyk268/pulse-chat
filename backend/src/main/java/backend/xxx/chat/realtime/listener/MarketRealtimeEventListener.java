package backend.xxx.chat.realtime.listener;

import java.util.Locale;

import backend.xxx.chat.market.redis.model.MarketLiveCandleHash;
import backend.xxx.chat.market.redis.model.MarketTickerLatestHash;
import backend.xxx.chat.realtime.event.CandleUpdatedDomainEvent;
import backend.xxx.chat.realtime.event.TickerUpdatedDomainEvent;
import backend.xxx.chat.realtime.model.MarketCandleUpdatedEventData;
import backend.xxx.chat.realtime.model.MarketTickerUpdatedEventData;
import backend.xxx.chat.realtime.model.RealtimeEventType;
import backend.xxx.chat.realtime.service.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketRealtimeEventListener {

    private static final String MARKET_TICKERS_TOPIC = "/topic/market/tickers";
    private static final String MARKET_TICKER_TOPIC_PREFIX = "/topic/market/tickers/";
    private static final String MARKET_CANDLE_TOPIC_PREFIX = "/topic/market/candles/";

    private final RealtimeEventPublisher realtimeEventPublisher;

    @EventListener
    public void onTickerUpdated(TickerUpdatedDomainEvent event) {
        MarketTickerLatestHash ticker = event.ticker();
        if (ticker == null || isBlank(ticker.getSymbol())) {
            return;
        }

        MarketTickerUpdatedEventData data = MarketTickerUpdatedEventData.from(ticker);
        String symbol = normalizeSymbol(ticker.getSymbol());

        realtimeEventPublisher.sendToTopic(
                MARKET_TICKERS_TOPIC,
                RealtimeEventType.MARKET_TICKER_UPDATED,
                data
        );
        realtimeEventPublisher.sendToTopic(
                MARKET_TICKER_TOPIC_PREFIX + symbol,
                RealtimeEventType.MARKET_TICKER_UPDATED,
                data
        );
    }

    @EventListener
    public void onCandleUpdated(CandleUpdatedDomainEvent event) {
        MarketLiveCandleHash candle = event.candle();
        if (candle == null || isBlank(candle.getSymbol()) || isBlank(candle.getIntervalName())) {
            return;
        }

        MarketCandleUpdatedEventData data = MarketCandleUpdatedEventData.from(candle);
        String symbol = normalizeSymbol(candle.getSymbol());
        String interval = candle.getIntervalName().toLowerCase(Locale.ROOT);

        realtimeEventPublisher.sendToTopic(
                MARKET_CANDLE_TOPIC_PREFIX + symbol + "/" + interval,
                RealtimeEventType.MARKET_CANDLE_UPDATED,
                data
        );
    }

    private String normalizeSymbol(String symbol) {
        return symbol.toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
