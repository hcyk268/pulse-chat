package backend.xxx.chat.market.service;

import backend.xxx.chat.market.model.MarketCandle;
import backend.xxx.chat.market.model.MarketPair;
import backend.xxx.chat.market.redis.model.MarketLiveCandleHash;
import backend.xxx.chat.market.repository.MarketCandleRepository;
import backend.xxx.chat.market.repository.MarketPairRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketCandlePersistenceService {

    private final MarketCandleRepository marketCandleRepository;
    private final MarketPairRepository marketPairRepository;

    @Transactional
    public void saveClosedCandle(MarketLiveCandleHash candle) {
        if (!canPersist(candle)) {
            return;
        }

        MarketCandle marketCandle = marketCandleRepository
                .findByPairIdAndIntervalNameAndOpenTime(
                        candle.getPairId(),
                        candle.getIntervalName(),
                        candle.getOpenTime()
                )
                .orElseGet(() -> createCandle(candle));

        applyValues(marketCandle, candle);
        marketCandleRepository.save(marketCandle);
    }

    private MarketCandle createCandle(MarketLiveCandleHash candle) {
        MarketPair pair = marketPairRepository.getReferenceById(candle.getPairId());
        return MarketCandle.create(pair, candle.getIntervalName(), candle.getOpenTime());
    }

    private void applyValues(MarketCandle marketCandle, MarketLiveCandleHash candle) {
        marketCandle.updateOhlcv(
                candle.getCloseTime(),
                candle.getOpen(),
                candle.getHigh(),
                candle.getLow(),
                candle.getClose(),
                candle.getVolume(),
                candle.getQuoteVolume(),
                candle.getTradeCount() == null ? 0L : candle.getTradeCount(),
                true
        );
    }

    private boolean canPersist(MarketLiveCandleHash candle) {
        return candle != null
                && candle.isClosed()
                && candle.getPairId() != null
                && notBlank(candle.getIntervalName())
                && candle.getOpenTime() != null
                && candle.getCloseTime() != null
                && candle.getOpen() != null
                && candle.getHigh() != null
                && candle.getLow() != null
                && candle.getClose() != null
                && candle.getVolume() != null
                && candle.getQuoteVolume() != null;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}