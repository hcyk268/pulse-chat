package backend.xxx.chat.market.service;

import java.util.List;
import java.util.Map;

import backend.xxx.chat.market.dto.CoinDetailResponse;
import backend.xxx.chat.market.dto.CoinMarketItemResponse;
import backend.xxx.chat.market.dto.MarketCandleResponse;
import backend.xxx.chat.market.dto.MarketPairResponse;
import backend.xxx.chat.market.dto.MarketTickerResponse;
import backend.xxx.chat.market.dto.PriceAlertResponse;
import backend.xxx.chat.market.dto.TrendingCoinResponse;
import backend.xxx.chat.market.dto.WatchlistAssetResponse;
import backend.xxx.chat.market.dto.WatchlistItemResponse;
import backend.xxx.chat.market.model.MarketAsset;
import backend.xxx.chat.market.model.MarketCandle;
import backend.xxx.chat.market.model.MarketPair;
import backend.xxx.chat.market.model.MarketTrending;
import backend.xxx.chat.market.model.PriceAlert;
import backend.xxx.chat.market.model.UserWatchlistItem;
import backend.xxx.chat.market.redis.model.MarketTickerLatestHash;
import org.springframework.stereotype.Component;

@Component
public class MarketMapper {

    public CoinMarketItemResponse toMarketItemResponse(MarketAsset asset, String pairSymbol) {
        return new CoinMarketItemResponse(
                asset.getId(),
                asset.getCoingeckoId(),
                asset.getSymbol(),
                asset.getName(),
                asset.getImageUrl(),
                asset.getMarketCapRank(),
                asset.getCurrentPriceUsd(),
                asset.getPriceChangePercentage24h(),
                asset.getHigh24h(),
                asset.getLow24h(),
                asset.getMarketCap(),
                asset.getTotalVolume(),
                asset.getLastSyncedAt(),
                pairSymbol
        );
    }

    public CoinDetailResponse toCoinDetailResponse(MarketAsset asset, MarketPairResponse binancePair) {
        return new CoinDetailResponse(
                asset.getId(),
                asset.getCoingeckoId(),
                asset.getSymbol(),
                asset.getName(),
                asset.getImageUrl(),
                asset.getMarketCapRank(),
                asset.getCurrentPriceUsd(),
                asset.getPriceChangePercentage24h(),
                asset.getHigh24h(),
                asset.getLow24h(),
                asset.getMarketCap(),
                asset.getTotalVolume(),
                asset.getCirculatingSupply(),
                asset.getTotalSupply(),
                asset.getMaxSupply(),
                asset.getLastSyncedAt(),
                binancePair
        );
    }

    public TrendingCoinResponse toTrendingResponse(MarketTrending trending) {
        MarketAsset asset = trending.getAsset();
        return new TrendingCoinResponse(
                trending.getId(),
                asset == null ? null : asset.getId(),
                trending.getCoingeckoId(),
                trending.getSymbol(),
                trending.getName(),
                trending.getThumbUrl(),
                trending.getMarketCapRank(),
                trending.getScore(),
                trending.getSnapshotAt()
        );
    }

    public MarketPairResponse toPairResponse(MarketPair pair) {
        return new MarketPairResponse(
                pair.getId(),
                pair.getExchange(),
                pair.getBaseSymbol(),
                pair.getQuoteSymbol(),
                pair.getSymbol(),
                pair.getLastSyncedAt()
        );
    }

    public MarketTickerResponse toTickerResponse(MarketTickerLatestHash ticker) {
        return toTickerResponse(ticker, List.of(), Map.of());
    }

    public MarketTickerResponse toTickerResponse(
            MarketTickerLatestHash ticker,
            List<MarketCandleResponse> candles,
            Map<String, List<MarketCandleResponse>> candlesByInterval
    ) {
        return new MarketTickerResponse(
                ticker.getPairId(),
                ticker.getExchange(),
                ticker.getSymbol(),
                ticker.getPrice(),
                ticker.getBidPrice(),
                ticker.getAskPrice(),
                ticker.getHigh24h(),
                ticker.getLow24h(),
                ticker.getVolume24h(),
                ticker.getQuoteVolume24h(),
                ticker.getPriceChange(),
                ticker.getPriceChangePercent(),
                ticker.getEventTime(),
                ticker.getUpdatedAt(),
                candles,
                candlesByInterval
        );
    }

    public MarketCandleResponse toCandleResponse(MarketCandle candle) {
        return new MarketCandleResponse(
                candle.getId(),
                candle.getPair().getId(),
                candle.getIntervalName(),
                candle.getOpenTime(),
                candle.getCloseTime(),
                candle.getOpen(),
                candle.getHigh(),
                candle.getLow(),
                candle.getClose(),
                candle.getVolume(),
                candle.getQuoteVolume(),
                candle.getTradeCount(),
                candle.isClosed()
        );
    }

    public WatchlistItemResponse toWatchlistItemResponse(UserWatchlistItem item) {
        return new WatchlistItemResponse(
                item.getId(),
                toWatchlistAssetResponse(item.getAsset()),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    public PriceAlertResponse toPriceAlertResponse(PriceAlert alert) {
        MarketPair pair = alert.getPair();
        return new PriceAlertResponse(
                alert.getId(),
                pair == null ? null : toPairResponse(pair),
                alert.getConditionType(),
                alert.getTargetPrice(),
                alert.getTargetPercent(),
                alert.isActive(),
                alert.getTriggeredAt(),
                alert.getLastCheckedAt(),
                alert.getLastTriggeredPrice(),
                alert.getCreatedAt(),
                alert.getUpdatedAt()
        );
    }

    private WatchlistAssetResponse toWatchlistAssetResponse(MarketAsset asset) {
        return new WatchlistAssetResponse(
                asset.getId(),
                asset.getCoingeckoId(),
                asset.getSymbol(),
                asset.getName(),
                asset.getImageUrl(),
                asset.getMarketCapRank(),
                asset.getCurrentPriceUsd(),
                asset.getPriceChangePercentage24h(),
                asset.getHigh24h(),
                asset.getLow24h(),
                asset.getMarketCap(),
                asset.getTotalVolume(),
                asset.getLastSyncedAt()
        );
    }
}
