package backend.xxx.chat.market.stream;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import backend.xxx.chat.market.model.MarketPair;
import backend.xxx.chat.market.redis.model.MarketLiveCandleHash;
import backend.xxx.chat.market.redis.model.MarketTickerLatestHash;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import static backend.xxx.chat.market.stream.BinanceMarketStreamService.BINANCE_EXCHANGE;

@Slf4j
@RequiredArgsConstructor
public class BinanceStreamHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final BinanceMarketDataCache marketDataCache;
    private final BinanceStreamSessionManager sessionManager;
    private final Map<String, MarketPair> pairsBySymbol;
    private final Runnable reconnectCallback;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonNode root = objectMapper.readTree(message.getPayload());
        JsonNode data = root.has("data") ? root.get("data") : root;
        String eventType = data.path("e").asText();

        if ("24hrTicker".equals(eventType)) {
            handleTicker(data);
            return;
        }

        if ("kline".equals(eventType)) {
            handleKline(data);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.clearSession(session);
        marketDataCache.markDisconnected(status.toString());
        log.warn("Stream closed: {}", status);
        reconnectCallback.run();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessionManager.clearSession(session);
        marketDataCache.markDisconnected(exception.getMessage());
        log.error("Transport error", exception);
        reconnectCallback.run();
    }

    private void handleTicker(JsonNode data) {
        String symbol = data.path("s").asText();
        MarketPair pair = pairsBySymbol.get(symbol);
        MarketTickerLatestHash ticker = new MarketTickerLatestHash();
        ticker.setSymbol(symbol);
        ticker.setPairId(pair == null ? null : pair.getId());
        ticker.setExchange(BINANCE_EXCHANGE);
        ticker.setPrice(decimal(data, "c"));
        ticker.setBidPrice(decimal(data, "b"));
        ticker.setAskPrice(decimal(data, "a"));
        ticker.setHigh24h(decimal(data, "h"));
        ticker.setLow24h(decimal(data, "l"));
        ticker.setVolume24h(decimal(data, "v"));
        ticker.setQuoteVolume24h(decimal(data, "q"));
        ticker.setPriceChange(decimal(data, "p"));
        ticker.setPriceChangePercent(decimal(data, "P"));
        ticker.setEventTime(instantMillis(data, "E"));
        ticker.setUpdatedAt(Instant.now());

        marketDataCache.cacheTicker(ticker);
        marketDataCache.markTickerEvent();
    }

    private void handleKline(JsonNode data) {
        String symbol = data.path("s").asText();
        JsonNode kline = data.path("k");
        String interval = kline.path("i").asText();
        MarketPair pair = pairsBySymbol.get(symbol);

        MarketLiveCandleHash candle = new MarketLiveCandleHash();
        candle.setId(symbol + ":" + interval);
        candle.setPairId(pair == null ? null : pair.getId());
        candle.setExchange(BINANCE_EXCHANGE);
        candle.setSymbol(symbol);
        candle.setIntervalName(interval);
        candle.setOpenTime(instantMillis(kline, "t"));
        candle.setCloseTime(instantMillis(kline, "T"));
        candle.setOpen(decimal(kline, "o"));
        candle.setHigh(decimal(kline, "h"));
        candle.setLow(decimal(kline, "l"));
        candle.setClose(decimal(kline, "c"));
        candle.setVolume(decimal(kline, "v"));
        candle.setQuoteVolume(decimal(kline, "q"));
        candle.setTradeCount(kline.path("n").asLong());
        candle.setClosed(kline.path("x").asBoolean());
        candle.setUpdatedAt(Instant.now());

        marketDataCache.cacheCandle(candle);
        marketDataCache.markKlineEvent();
    }

    private BigDecimal decimal(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return new BigDecimal(value.asText());
    }

    private Instant instantMillis(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return Instant.ofEpochMilli(value.asLong());
    }
}