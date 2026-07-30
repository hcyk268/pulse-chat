package backend.xxx.chat.market.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.config.properties.BinanceStreamProperties;
import backend.xxx.chat.market.dto.CoinDetailResponse;
import backend.xxx.chat.market.dto.CoinMarketItemResponse;
import backend.xxx.chat.market.dto.MarketCandleResponse;
import backend.xxx.chat.market.dto.MarketPairResponse;
import backend.xxx.chat.market.dto.MarketTickerResponse;
import backend.xxx.chat.market.dto.OverviewMarketResponse;
import backend.xxx.chat.market.dto.TrendingCoinResponse;
import backend.xxx.chat.market.model.MarketAsset;
import backend.xxx.chat.market.model.MarketCandle;
import backend.xxx.chat.market.model.MarketPair;
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
    private final MarketCandleHistoryBackfillService marketCandleHistoryBackfillService;
    private final MarketMapper marketMapper;

    @Transactional(readOnly = true)
    public OverviewMarketResponse getMarket() {
        Map<Long, String> supportedAssetPairSymbols = getTickerBackedAssetPairSymbols();
        Set<Long> supportedAssetIds = supportedAssetPairSymbols.keySet();
        List<CoinMarketItemResponse> coins = marketAssetRepository
                .findAllByActiveTrueOrderByMarketCapRankAsc()
                .stream()
                .filter(asset -> supportedAssetIds.contains(asset.getId()))
                .map(asset -> marketMapper.toMarketItemResponse(asset, supportedAssetPairSymbols.get(asset.getId())))
                .toList();
        List<TrendingCoinResponse> trending = marketTrendingRepository
                .findAllByOrderByScoreAsc()
                .stream()
                .filter(trendingCoin -> isSupportedTrendingCoin(trendingCoin.getAsset(), supportedAssetIds))
                .map(marketMapper::toTrendingResponse)
                .toList();

        return new OverviewMarketResponse(coins, trending);
    }

    private Map<Long, String> getTickerBackedAssetPairSymbols() {
        List<MarketPair> pairs = marketPairRepository.findActivePairsWithAsset(BINANCE_EXCHANGE);
        if (pairs.isEmpty()) {
            return Map.of();
        }

        List<String> pairSymbols = pairs.stream()
                .map(MarketPair::getSymbol)
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .toList();
        Set<String> tickerSymbols = binanceMarketDataCache.getTickers(pairSymbols)
                .stream()
                .map(MarketTickerLatestHash::getSymbol)
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .map(this::normalizeSymbol)
                .collect(Collectors.toSet());
        if (tickerSymbols.isEmpty()) {
            return Map.of();
        }

        return pairs.stream()
                .filter(pair -> tickerSymbols.contains(normalizeSymbol(pair.getSymbol())))
                .filter(pair -> pair.getAsset() != null && pair.getAsset().getId() != null)
                .collect(Collectors.toMap(
                        pair -> pair.getAsset().getId(),
                        MarketPair::getSymbol,
                        (current, ignored) -> current,
                        LinkedHashMap::new
                ));
    }

    @Transactional(readOnly = true)
    public CoinDetailResponse getCoinDetail(String symbol) {
        MarketAsset asset = marketAssetRepository.findFirstBySymbolIgnoreCaseAndActiveTrue(symbol)
                .orElseThrow(() -> new NotFoundException("market.coin.not.found"));
        MarketPairResponse binancePair = marketPairRepository
                .findFirstByAsset_IdAndExchangeAndActiveTrue(asset.getId(), BINANCE_EXCHANGE)
                .map(marketMapper::toPairResponse)
                .orElse(null);

        return marketMapper.toCoinDetailResponse(asset, binancePair);
    }

    @Transactional(readOnly = true)
    public List<MarketTickerResponse> getTickers() {
        return binanceMarketDataCache.getTickers(getCachedTickerSymbols())
                .stream()
                .map(marketMapper::toTickerResponse)
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

    @Transactional
    public MarketTickerResponse getTicker(String symbol) {
        String pairSymbol = resolveBinancePairSymbol(symbol);
        MarketTickerLatestHash ticker = binanceMarketDataCache.getTicker(pairSymbol)
                .orElseThrow(() -> new NotFoundException("market.ticker.not.found"));
        MarketPair pair = marketPairRepository.findByExchangeAndSymbol(BINANCE_EXCHANGE, pairSymbol)
                .filter(MarketPair::isActive)
                .orElse(null);
        Long pairId = pair == null ? ticker.getPairId() : pair.getId();
        if (pair != null) {
            marketCandleHistoryBackfillService.backfillIfNeeded(pair, binanceStreamProperties.getCandleIntervals());
        }
        Map<String, List<MarketCandleResponse>> candlesByInterval = getRecentCandlesByInterval(pairId);
        List<MarketCandleResponse> candles = candlesByInterval.values()
                .stream()
                .findFirst()
                .orElse(List.of());

        return marketMapper.toTickerResponse(ticker, candles, candlesByInterval);
    }

    @Transactional
    public List<MarketCandleResponse> getCandles(String symbol, String interval) {
        String pairSymbol = resolveBinancePairSymbol(symbol);
        String candleInterval = resolveCandleInterval(interval);
        MarketPair pair = marketPairRepository.findByExchangeAndSymbol(BINANCE_EXCHANGE, pairSymbol)
                .filter(MarketPair::isActive)
                .orElseThrow(() -> new NotFoundException("market.pair.not.found"));

        marketCandleHistoryBackfillService.backfillIfNeeded(pair, List.of(candleInterval));
        return getRecentCandles(pair.getId(), candleInterval);
    }

    private String resolveBinancePairSymbol(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        return marketPairRepository.findByExchangeAndSymbol(BINANCE_EXCHANGE, normalizedSymbol)
                .filter(MarketPair::isActive)
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

    private String resolveCandleInterval(String interval) {
        String normalizedInterval = interval == null ? "" : interval.trim().toLowerCase(Locale.ROOT);
        return binanceStreamProperties.getCandleIntervals()
                .stream()
                .filter(configuredInterval -> configuredInterval != null
                        && configuredInterval.equalsIgnoreCase(normalizedInterval))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("market.candle.interval.unsupported"));
    }

    private Map<String, List<MarketCandleResponse>> getRecentCandlesByInterval(Long pairId) {
        if (pairId == null) {
            return Map.of();
        }

        Map<String, List<MarketCandleResponse>> candlesByInterval = new LinkedHashMap<>();
        binanceStreamProperties.getCandleIntervals()
                .stream()
                .filter(interval -> interval != null && !interval.isBlank())
                .distinct()
                .forEach(interval -> candlesByInterval.put(interval, getRecentCandles(pairId, interval)));
        return candlesByInterval;
    }

    private List<MarketCandleResponse> getRecentCandles(Long pairId, String interval) {
        List<MarketCandle> candles = new ArrayList<>(marketCandleRepository
                .findByPairIdAndIntervalNameOrderByOpenTimeDesc(
                        pairId,
                        interval,
                        PageRequest.of(0, TICKER_CANDLE_HISTORY_LIMIT)
                ));
        Collections.reverse(candles);
        return candles.stream()
                .map(marketMapper::toCandleResponse)
                .toList();
    }

    private boolean isSupportedTrendingCoin(MarketAsset asset, Set<Long> supportedAssetIds) {
        return asset != null && supportedAssetIds.contains(asset.getId());
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return "";
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
