package backend.xxx.chat.market.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

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

        intervals.stream()
                .filter(interval -> interval != null && !interval.isBlank())
                .distinct()
                .forEach(interval -> backfillIntervalIfNeeded(pair, interval));
    }

    private void backfillIntervalIfNeeded(MarketPair pair, String interval) {
        int historyLimit = properties.getCandleHistoryLimit();
        Instant now = Instant.now();
        long existingCount = marketCandleRepository.countByPairIdAndIntervalName(pair.getId(), interval);
        boolean stale = marketCandleRepository.findFirstByPairIdAndIntervalNameOrderByOpenTimeDesc(pair.getId(), interval)
                .map(candle -> isStale(candle, interval, now))
                .orElse(true);
        if (existingCount >= historyLimit && !stale) {
            return;
        }

        try {
            List<BinanceKlineResponse> klines = binanceMarketHistoryClient
                    .getKlines(pair.getSymbol(), interval, Math.min(historyLimit + 1, 1000))
                    .stream()
                    .filter(kline -> isClosedKline(kline, now))
                    .toList();
            for (BinanceKlineResponse kline : klines) {
                upsertCandle(pair, interval, kline);
            }
        } catch (RuntimeException exception) {
            log.warn("Failed to backfill Binance candle history for {} {}", pair.getSymbol(), interval, exception);
        }
    }

    private boolean isClosedKline(BinanceKlineResponse kline, Instant now) {
        return kline.closeTime() != null && kline.closeTime().isBefore(now);
    }

    private boolean isStale(MarketCandle candle, String interval, Instant now) {
        Duration intervalDuration = intervalDuration(interval);
        return candle.getCloseTime() == null || candle.getCloseTime().plus(intervalDuration).isBefore(now);
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

    private void upsertCandle(MarketPair pair, String interval, BinanceKlineResponse kline) {
        MarketCandle candle = marketCandleRepository
                .findByPairIdAndIntervalNameAndOpenTime(pair.getId(), interval, kline.openTime())
                .orElseGet(() -> createCandle(pair, interval, kline));

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
        marketCandleRepository.save(candle);
    }

    private MarketCandle createCandle(MarketPair pair, String interval, BinanceKlineResponse kline) {
        return MarketCandle.create(pair, interval, kline.openTime());
    }
}