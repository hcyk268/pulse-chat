package backend.xxx.chat.market.stream;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import backend.xxx.chat.config.properties.BinanceStreamProperties;
import backend.xxx.chat.market.model.MarketPair;
import backend.xxx.chat.market.repository.MarketPairRepository;
import backend.xxx.chat.market.service.MarketCandlePersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Slf4j
@Component
public class BinanceMarketStreamService {

    public static final String BINANCE_EXCHANGE = "BINANCE";
    public static final String TICKER_STREAM = "ticker";

    private final BinanceStreamProperties properties;
    private final MarketPairRepository marketPairRepository;
    private final BinanceMarketDataCache marketDataCache;
    private final MarketCandlePersistenceService marketCandlePersistenceService;
    private final BinanceStreamSessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TaskScheduler marketStreamTaskScheduler;
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);

    public BinanceMarketStreamService(
            BinanceStreamProperties properties,
            MarketPairRepository marketPairRepository,
            BinanceMarketDataCache marketDataCache,
            MarketCandlePersistenceService marketCandlePersistenceService,
            BinanceStreamSessionManager sessionManager,
            ObjectMapper objectMapper,
            ApplicationEventPublisher applicationEventPublisher,
            @Qualifier("marketStreamTaskScheduler") TaskScheduler marketStreamTaskScheduler
    ) {
        this.properties = properties;
        this.marketPairRepository = marketPairRepository;
        this.marketDataCache = marketDataCache;
        this.marketCandlePersistenceService = marketCandlePersistenceService;
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.marketStreamTaskScheduler = marketStreamTaskScheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connectWhenApplicationIsReady() {
        connectIfNeeded();
    }

    @Scheduled(
            fixedDelayString = "${app.market.stream.reconnect-delay-ms:60000}",
            initialDelayString = "${app.market.stream.initial-delay-ms:15000}",
            scheduler = "marketStreamTaskScheduler"
    )
    public void reconnectIfNeeded() {
        connectIfNeeded();
    }

    @PreDestroy
    public void close() throws IOException {
        sessionManager.close();
    }

    private void connectIfNeeded() {
        if (sessionManager.isConnected() || !connecting.compareAndSet(false, true)) {
            return;
        }

        try {
            List<MarketPair> pairs = loadStreamPairs();
            if (pairs.isEmpty()) {
                connecting.set(false);
                marketDataCache.markDisconnected("No active Binance market pairs to stream");
                log.debug("No active market pairs exist");
                return;
            }

            Map<String, MarketPair> pairsBySymbol = pairs.stream()
                    .collect(Collectors.toMap(MarketPair::getSymbol, Function.identity()));
            List<String> streams = buildStreamNames(pairs);
            URI streamUri = buildStreamUri();
            WebSocketClient client = new StandardWebSocketClient();
            BinanceStreamHandler handler = new BinanceStreamHandler(
                    objectMapper,
                    marketDataCache,
                    marketCandlePersistenceService,
                    sessionManager,
                    pairsBySymbol,
                    streams,
                    this::scheduleReconnectNow,
                    applicationEventPublisher
            );
            CompletableFuture<WebSocketSession> future = client.execute(handler, streamUri.toString());
            future.whenComplete((connectedSession, throwable) -> {
                connecting.set(false);
                if (throwable != null) {
                    marketDataCache.markDisconnected(throwable.getMessage());
                    log.error("Failed to connect Binance", throwable);
                    scheduleReconnectNow();
                    return;
                }

                sessionManager.setSession(connectedSession);
                marketDataCache.markConnected(pairs.size());
                log.info("Connected Binance market {} coin with {} stream", pairs.size(), streams.size());
            });
        } catch (RuntimeException exception) {
            connecting.set(false);
            marketDataCache.markDisconnected(exception.getMessage());
            log.warn("Failed to prepare Binance market stream", exception);
            scheduleReconnectNow();
        }
    }

    private void scheduleReconnectNow() {
        if (!reconnectScheduled.compareAndSet(false, true)) {
            return;
        }

        marketStreamTaskScheduler.schedule(() -> {
            try {
                connectIfNeeded();
            } finally {
                reconnectScheduled.set(false);
            }
        }, Instant.now().plusSeconds(2));
    }

    private List<MarketPair> loadStreamPairs() {
        return marketPairRepository.findActivePairsWithAsset(BINANCE_EXCHANGE)
                .stream()
                .filter(pair -> pair.getSymbol() != null && !pair.getSymbol().isBlank())
                .limit(properties.getMaxPairs())
                .toList();
    }

    private List<String> buildStreamNames(List<MarketPair> pairs) {
        return pairs.stream()
                .flatMap(pair -> {
                    String symbol = pair.getSymbol().toLowerCase(Locale.ROOT);
                    Stream<String> ticker = Stream.of(symbol + "@" + TICKER_STREAM);
                    Stream<String> candles = properties.getCandleIntervals().stream()
                            .map(interval -> symbol + "@kline_" + interval);
                    return Stream.concat(ticker, candles);
                })
                .toList();
    }

    private URI buildStreamUri() {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/stream")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - "/stream".length()) + "/ws";
        }
        return URI.create(baseUrl);
    }
}
