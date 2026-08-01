package backend.xxx.chat.market.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import backend.xxx.chat.config.properties.BinanceStreamProperties;
import backend.xxx.chat.market.client.BinanceMarketHistoryClient;
import backend.xxx.chat.market.client.dto.BinanceKlineResponse;
import backend.xxx.chat.market.model.MarketCandle;
import backend.xxx.chat.market.model.MarketPair;
import backend.xxx.chat.market.repository.MarketCandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketCandleHistoryBackfillService {

    private final BinanceMarketHistoryClient binanceMarketHistoryClient;
    private final MarketCandleRepository marketCandleRepository;
    private final BinanceStreamProperties properties;

    @Transactional
    public void backfillIfNeeded(MarketPair pair, Collection<String> intervals) {
        if (pair == null || pair.getId() == null || pair.getSymbol() == null || pair.getSymbol().isBlank()) {
            return;
        }

        List<String> normalizedIntervals = intervals == null
                ? List.of()
                : intervals.stream()
                        .filter(interval -> interval != null && !interval.isBlank())
                        .distinct()
                        .toList();
        if (normalizedIntervals.isEmpty()) {
            return;
        }

        int historyLimit = properties.getCandleHistoryLimit();
        Instant now = Instant.now();
        Map<String, MarketCandleRepository.CandleHistoryState> stateByInterval =
                marketCandleRepository.findHistoryStates(pair.getId(), normalizedIntervals)
                        .stream()
                        .collect(Collectors.toMap(
                                MarketCandleRepository.CandleHistoryState::getIntervalName,
                                Function.identity()
                        ));

        List<String> intervalsToBackfill = normalizedIntervals.stream()
                .filter(interval -> needsBackfill(
                        stateByInterval.get(interval),
                        interval,
                        historyLimit,
                        now
                ))
                .toList();
        if (intervalsToBackfill.isEmpty()) {
            return;
        }

        Map<String, List<BinanceKlineResponse>> klinesByInterval = new LinkedHashMap<>();
        for (String interval : intervalsToBackfill) {
            List<BinanceKlineResponse> klines = loadClosedKlines(pair, interval, historyLimit, now);
            if (!klines.isEmpty()) {
                klinesByInterval.put(interval, klines);
            }
        }
        if (klinesByInterval.isEmpty()) {
            return;
        }

        Set<Instant> openTimes = klinesByInterval.values()
                .stream()
                .flatMap(Collection::stream)
                .map(BinanceKlineResponse::openTime)
                .collect(Collectors.toSet());
        Map<CandleKey, MarketCandle> existingByKey = marketCandleRepository.findExistingCandles(
                        pair.getId(),
                        klinesByInterval.keySet(),
                        openTimes
                )
                .stream()
                .collect(Collectors.toMap(
                        candle -> new CandleKey(candle.getIntervalName(), candle.getOpenTime()),
                        Function.identity()
                ));

        List<MarketCandle> candlesToSave = new ArrayList<>();
        klinesByInterval.forEach((interval, klines) -> klines.forEach(kline -> {
            CandleKey key = new CandleKey(interval, kline.openTime());
            MarketCandle candle = existingByKey.getOrDefault(
                    key,
                    MarketCandle.create(pair, interval, kline.openTime())
            );
            applyKline(candle, kline);
            candlesToSave.add(candle);
        }));
        marketCandleRepository.saveAll(candlesToSave);
    }

    private boolean needsBackfill(
            MarketCandleRepository.CandleHistoryState state,
            String interval,
            int historyLimit,
            Instant now
    ) {
        return state == null
                || state.getCandleCount() < historyLimit
                || isStale(state.getLatestCloseTime(), interval, now);
    }

    private List<BinanceKlineResponse> loadClosedKlines(
            MarketPair pair,
            String interval,
            int historyLimit,
            Instant now
    ) {
        try {
            return binanceMarketHistoryClient
                    .getKlines(pair.getSymbol(), interval, Math.min(historyLimit + 1, 1000))
                    .stream()
                    .filter(kline -> isClosedKline(kline, now))
                    .toList();
        } catch (RuntimeException exception) {
            log.warn("Failed to backfill Binance candle history for {} {}", pair.getSymbol(), interval, exception);
            return List.of();
        }
    }

    private boolean isClosedKline(BinanceKlineResponse kline, Instant now) {
        return kline.closeTime() != null && kline.closeTime().isBefore(now);
    }

    private boolean isStale(Instant closeTime, String interval, Instant now) {
        Duration intervalDuration = intervalDuration(interval);
        return closeTime == null || closeTime.plus(intervalDuration).isBefore(now);
    }

    private Duration intervalDuration(String interval) {
        if (interval == null || interval.length() < 2) {
            return Duration.ofHours(1);
        }

        long amount;
        try {
            amount = Long.parseLong(interval.substring(0, interval.length() - 1));
        } catch (NumberFormatException exception) {
            return Duration.ofHours(1);
        }

        return switch (interval.charAt(interval.length() - 1)) {
            case 'm' -> Duration.ofMinutes(amount);
            case 'h' -> Duration.ofHours(amount);
            case 'd' -> Duration.ofDays(amount);
            case 'w' -> Duration.ofDays(amount * 7);
            default -> Duration.ofHours(1);
        };
    }

    private void applyKline(MarketCandle candle, BinanceKlineResponse kline) {
        candle.updateOhlcv(
                kline.closeTime(),
                kline.open(),
                kline.high(),
                kline.low(),
                kline.close(),
                kline.volume(),
                kline.quoteVolume(),
                kline.tradeCount(),
                true
        );
    }

    private record CandleKey(String interval, Instant openTime) {
    }
}
