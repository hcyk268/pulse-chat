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
        MarketCandle marketCandle = new MarketCandle();
        marketCandle.setPair(pair);
        marketCandle.setIntervalName(candle.getIntervalName());
        marketCandle.setOpenTime(candle.getOpenTime());
        return marketCandle;
    }

    private void applyValues(MarketCandle marketCandle, MarketLiveCandleHash candle) {
        marketCandle.setCloseTime(candle.getCloseTime());
        marketCandle.setOpen(candle.getOpen());
        marketCandle.setHigh(candle.getHigh());
        marketCandle.setLow(candle.getLow());
        marketCandle.setClose(candle.getClose());
        marketCandle.setVolume(candle.getVolume());
        marketCandle.setQuoteVolume(candle.getQuoteVolume());
        marketCandle.setTradeCount(candle.getTradeCount() == null ? 0L : candle.getTradeCount());
        marketCandle.setClosed(true);
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
