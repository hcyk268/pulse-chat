package backend.xxx.chat.market.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.config.properties.BinanceStreamProperties;
import backend.xxx.chat.market.dto.CoinDetailResponse;
import backend.xxx.chat.market.dto.MarketTickerResponse;
import backend.xxx.chat.market.dto.OverviewMarketResponse;
import backend.xxx.chat.market.model.MarketAsset;
import backend.xxx.chat.market.model.MarketCandle;
import backend.xxx.chat.market.model.MarketPair;
import backend.xxx.chat.market.model.MarketTrending;
import backend.xxx.chat.market.redis.model.MarketTickerLatestHash;
import backend.xxx.chat.market.repository.MarketAssetRepository;
import backend.xxx.chat.market.repository.MarketCandleRepository;
import backend.xxx.chat.market.repository.MarketPairRepository;
import backend.xxx.chat.market.repository.MarketTrendingRepository;
import backend.xxx.chat.market.stream.BinanceMarketDataCache;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketService {

    private static final String BINANCE_EXCHANGE = "BINANCE";
    private static final int TICKER_CANDLE_HISTORY_LIMIT = 300;

    private final MarketAssetRepository marketAssetRepository;
    private final MarketPairRepository marketPairRepository;
    private final MarketTrendingRepository marketTrendingRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final BinanceMarketDataCache binanceMarketDataCache;
    private final BinanceStreamProperties binanceStreamProperties;


    @Transactional(readOnly = true)
    public OverviewMarketResponse getMarket() {
        List<OverviewMarketResponse.CoinMarketItemResponse> coins = marketAssetRepository
                .findAllByActiveTrueOrderByMarketCapRankAsc()
                .stream()
                .map(this::toMarketItemResponse)
                .toList();
        List<OverviewMarketResponse.TrendingCoinResponse> trending = marketTrendingRepository
                .findAllByOrderByScoreAsc()
                .stream()
                .map(this::toTrendingResponse)
                .toList();

        return new OverviewMarketResponse(coins, trending);
    }

    @Transactional(readOnly = true)
    public CoinDetailResponse getCoinDetail(String symbol) {
        MarketAsset asset = marketAssetRepository.findFirstBySymbolIgnoreCaseAndActiveTrue(symbol)
                .orElseThrow(() -> new NotFoundException("market.coin.not.found"));
        CoinDetailResponse.MarketPairResponse binancePair = marketPairRepository
                .findFirstByAsset_IdAndExchangeAndActiveTrue(asset.getId(), BINANCE_EXCHANGE)
                .map(this::toPairResponse)
                .orElse(null);

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

    @Transactional(readOnly = true)
    public List<MarketTickerResponse> getTickers() {
        return binanceMarketDataCache.getTickers(getCachedTickerSymbols())
                .stream()
                .map(MarketTickerResponse::from)
                .toList();
    }

    private List<String> getCachedTickerSymbols() {
        return binanceMarketDataCache.getTickerSymbols()
                .orElseGet(() -> {
                    List<String> symbols = marketPairRepository.findActivePairsWithAsset(BINANCE_EXCHANGE)
                            .stream()
                            .map(MarketPair::getSymbol)
                            .toList();
                    binanceMarketDataCache.cacheTickerSymbols(symbols);
                    return symbols;
                });
    }

    @Transactional(readOnly = true)
    public MarketTickerResponse getTicker(String symbol) {
        String pairSymbol = resolveBinancePairSymbol(symbol);
        MarketTickerLatestHash ticker = binanceMarketDataCache.getTicker(pairSymbol)
                .orElseThrow(() -> new NotFoundException("market.ticker.not.found"));
        Long pairId = resolvePairId(pairSymbol, ticker);
        Map<String, List<MarketTickerResponse.CandleResponse>> candlesByInterval = getRecentCandlesByInterval(pairId);
        List<MarketTickerResponse.CandleResponse> candles = candlesByInterval.values()
                .stream()
                .findFirst()
                .orElse(List.of());

        return MarketTickerResponse.from(ticker, candles, candlesByInterval);
    }

    private String resolveBinancePairSymbol(String symbol) {
        String normalizedSymbol = symbol.toUpperCase(Locale.ROOT);
        return marketPairRepository.findByExchangeAndSymbol(BINANCE_EXCHANGE, normalizedSymbol)
                .map(MarketPair::getSymbol)
                .orElseGet(() -> resolvePairSymbolFromAssetSymbol(normalizedSymbol));
    }

    private String resolvePairSymbolFromAssetSymbol(String symbol) {
        MarketAsset asset = marketAssetRepository.findFirstBySymbolIgnoreCaseAndActiveTrue(symbol)
                .orElseThrow(() -> new NotFoundException("market.coin.not.found"));

        return marketPairRepository.findFirstByAsset_IdAndExchangeAndActiveTrue(asset.getId(), BINANCE_EXCHANGE)
                .map(MarketPair::getSymbol)
                .orElseThrow(() -> new NotFoundException("market.pair.not.found"));
    }

    private Long resolvePairId(String pairSymbol, MarketTickerLatestHash ticker) {
        return marketPairRepository.findByExchangeAndSymbol(BINANCE_EXCHANGE, pairSymbol)
                .map(MarketPair::getId)
                .orElse(ticker.getPairId());
    }

    private Map<String, List<MarketTickerResponse.CandleResponse>> getRecentCandlesByInterval(Long pairId) {
        if (pairId == null) {
            return Map.of();
        }

        Map<String, List<MarketTickerResponse.CandleResponse>> candlesByInterval = new LinkedHashMap<>();
        binanceStreamProperties.getCandleIntervals()
                .stream()
                .filter(interval -> interval != null && !interval.isBlank())
                .distinct()
                .forEach(interval -> candlesByInterval.put(interval, getRecentCandles(pairId, interval)));
        return candlesByInterval;
    }

    private List<MarketTickerResponse.CandleResponse> getRecentCandles(Long pairId, String interval) {
        List<MarketCandle> candles = new ArrayList<>(marketCandleRepository
                .findByPairIdAndIntervalNameOrderByOpenTimeDesc(
                        pairId,
                        interval,
                        PageRequest.of(0, TICKER_CANDLE_HISTORY_LIMIT)
                ));
        Collections.reverse(candles);
        return candles.stream()
                .map(MarketTickerResponse.CandleResponse::from)
                .toList();
    }

    private OverviewMarketResponse.CoinMarketItemResponse toMarketItemResponse(MarketAsset asset) {
        return new OverviewMarketResponse.CoinMarketItemResponse(
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

    private OverviewMarketResponse.TrendingCoinResponse toTrendingResponse(MarketTrending trending) {
        MarketAsset asset = trending.getAsset();
        return new OverviewMarketResponse.TrendingCoinResponse(
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

    private CoinDetailResponse.MarketPairResponse toPairResponse(MarketPair pair) {
        return new CoinDetailResponse.MarketPairResponse(
                pair.getId(),
                pair.getExchange(),
                pair.getBaseSymbol(),
                pair.getQuoteSymbol(),
                pair.getSymbol(),
                pair.getLastSyncedAt()
        );
    }
}