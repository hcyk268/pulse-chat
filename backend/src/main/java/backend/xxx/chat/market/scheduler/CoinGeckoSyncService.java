package backend.xxx.chat.market.scheduler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import backend.xxx.chat.market.client.BinanceExchangeInfoClient;
import backend.xxx.chat.market.client.CoinGeckoClient;
import backend.xxx.chat.market.client.dto.CoinMarketResponse;
import backend.xxx.chat.market.client.dto.CoinTrendingEntry;
import backend.xxx.chat.market.client.dto.CoinTrendingItem;
import backend.xxx.chat.market.client.dto.CoinTrendingResponse;
import backend.xxx.chat.market.model.MarketAsset;
import backend.xxx.chat.market.model.MarketPair;
import backend.xxx.chat.market.model.MarketTrending;
import backend.xxx.chat.market.repository.MarketAssetRepository;
import backend.xxx.chat.market.repository.MarketPairRepository;
import backend.xxx.chat.market.repository.MarketTrendingRepository;
import backend.xxx.chat.market.stream.BinanceMarketDataCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoinGeckoSyncService {

    private static final String BINANCE_EXCHANGE = "BINANCE";
    private static final String USDT_SYMBOL = "USDT";

    private final CoinGeckoClient coinGeckoClient;
    private final BinanceExchangeInfoClient binanceExchangeInfoClient;
    private final MarketAssetRepository marketAssetRepository;
    private final MarketPairRepository marketPairRepository;
    private final MarketTrendingRepository marketTrendingRepository;
    private final BinanceMarketDataCache binanceMarketDataCache;

    @Transactional
    public int syncTopMarkets() {
        List<CoinMarketResponse> coins = coinGeckoClient.getTopMarkets();
        if (coins == null || coins.isEmpty()) {
            log.warn("CoinGecko /coins/markets returned no coins");
            return 0;
        }

        Instant syncedAt = Instant.now();
        List<CoinMarketResponse> validCoins = coins.stream()
                .filter(this::isValidMarketCoin)
                .toList();
        if (validCoins.isEmpty()) {
            return 0;
        }

        Collection<String> candidatePairSymbols = toBinancePairSymbols(validCoins);
        Optional<Set<String>> supportedPairSymbols = loadSupportedBinancePairSymbols(candidatePairSymbols);
        if (supportedPairSymbols.isEmpty()) {
            log.warn("Skipped market asset sync. Binance exchangeInfo is unavailable");
            return 0;
        }
        Set<String> supportedSymbols = supportedPairSymbols.get();

        Map<String, MarketAsset> assetsByCoingeckoId = marketAssetRepository
                .findAllByCoingeckoIdIn(toMarketCoinIds(validCoins))
                .stream()
                .collect(Collectors.toMap(MarketAsset::getCoingeckoId, Function.identity()));
        List<MarketAsset> assetsToSave = new ArrayList<>(validCoins.size());

        for (CoinMarketResponse coin : validCoins) {
            MarketAsset asset = assetsByCoingeckoId.getOrDefault(coin.id(), new MarketAsset());
            boolean supportedByBinance = supportedSymbols.contains(toBinancePairSymbol(coin.symbol()));
            applyMarketCoin(asset, coin, syncedAt, supportedByBinance);
            assetsToSave.add(asset);
        }

        List<MarketAsset> savedAssets = marketAssetRepository.saveAll(assetsToSave);
        Map<String, MarketPair> pairsBySymbol = marketPairRepository
                .findAllByExchangeAndSymbolIn(BINANCE_EXCHANGE, candidatePairSymbols)
                .stream()
                .collect(Collectors.toMap(MarketPair::getSymbol, Function.identity()));
        List<MarketPair> pairsToSave = new ArrayList<>();
        for (MarketAsset asset : savedAssets) {
            String pairSymbol = toBinancePairSymbol(asset.getSymbol());
            if (asset.isActive() && supportedSymbols.contains(pairSymbol)) {
                pairsToSave.add(buildBinanceUsdtPair(asset, pairsBySymbol, syncedAt));
            }
        }
        pairsBySymbol.values()
                .stream()
                .filter(pair -> !supportedSymbols.contains(pair.getSymbol()))
                .filter(MarketPair::isActive)
                .forEach(pair -> {
                    pair.deactivate(syncedAt);
                    pairsToSave.add(pair);
                });
        List<MarketPair> savedPairs = marketPairRepository.saveAll(pairsToSave);
        binanceMarketDataCache.cacheTickerSymbols(savedPairs.stream()
                .filter(MarketPair::isActive)
                .map(MarketPair::getSymbol)
                .toList());

        return savedAssets.size();
    }

    @Transactional
    public int syncTrending() {
        CoinTrendingResponse response = coinGeckoClient.getTrending();
        if (response == null || response.coins().isEmpty()) {
            log.warn("CoinGecko /search/trending returned no coins");
            return 0;
        }

        Instant snapshotAt = Instant.now();
        List<CoinTrendingItem> validItems = response.coins().stream()
                .map(CoinTrendingEntry::item)
                .filter(this::isValidTrendingItem)
                .toList();
        if (validItems.isEmpty()) {
            return 0;
        }

        Collection<String> coingeckoIds = toTrendingCoinIds(validItems);
        Map<String, MarketAsset> assetsByCoingeckoId = marketAssetRepository
                .findAllByCoingeckoIdIn(coingeckoIds)
                .stream()
                .collect(Collectors.toMap(MarketAsset::getCoingeckoId, Function.identity()));
        Map<String, MarketTrending> trendingByCoingeckoId = marketTrendingRepository
                .findAllByCoingeckoIdIn(coingeckoIds)
                .stream()
                .collect(Collectors.toMap(MarketTrending::getCoingeckoId, Function.identity()));

        List<MarketTrending> trendingToSave = validItems.stream()
                .map(item -> {
                    MarketTrending trending = trendingByCoingeckoId.getOrDefault(item.id(), new MarketTrending());
                    trending.syncSnapshot(
                            assetsByCoingeckoId.get(item.id()),
                            item.id(),
                            item.symbol(),
                            item.name(),
                            item.thumb(),
                            item.marketCapRank(),
                            item.score(),
                            snapshotAt
                    );
                    return trending;
                })
                .toList();

        return marketTrendingRepository.saveAll(trendingToSave).size();
    }

    private boolean isValidMarketCoin(CoinMarketResponse coin) {
        return coin != null && coin.id() != null && coin.symbol() != null && coin.name() != null;
    }

    private boolean isValidTrendingItem(CoinTrendingItem item) {
        return item != null && item.id() != null && item.symbol() != null && item.name() != null;
    }

    private Collection<String> toMarketCoinIds(List<CoinMarketResponse> coins) {
        return coins.stream()
                .map(CoinMarketResponse::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Collection<String> toTrendingCoinIds(List<CoinTrendingItem> items) {
        return items.stream()
                .map(CoinTrendingItem::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Collection<String> toBinancePairSymbols(List<CoinMarketResponse> coins) {
        return coins.stream()
                .map(CoinMarketResponse::symbol)
                .map(this::toBinancePairSymbol)
                .collect(Collectors.toSet());
    }

    private String toBinancePairSymbol(String symbol) {
        return symbol.toUpperCase(Locale.ROOT) + USDT_SYMBOL;
    }

    private Optional<Set<String>> loadSupportedBinancePairSymbols(Collection<String> candidatePairSymbols) {
        if (candidatePairSymbols == null || candidatePairSymbols.isEmpty()) {
            return Optional.of(Set.of());
        }

        try {
            Set<String> candidateSymbols = candidatePairSymbols.stream()
                    .map(symbol -> symbol.toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            Set<String> tradingSymbols = binanceExchangeInfoClient.getTradingSpotSymbolsByQuote(USDT_SYMBOL);
            if (tradingSymbols.isEmpty()) {
                log.warn("Binance exchangeInfo returned no trading {} symbols", USDT_SYMBOL);
                return Optional.empty();
            }

            Set<String> supportedSymbols = tradingSymbols.stream()
                    .filter(candidateSymbols::contains)
                    .collect(Collectors.toSet());
            return Optional.of(supportedSymbols);
        } catch (RuntimeException exception) {
            log.warn("Failed to load Binance exchangeInfo", exception);
            return Optional.empty();
        }
    }

    private void applyMarketCoin(
            MarketAsset asset,
            CoinMarketResponse coin,
            Instant syncedAt,
            boolean supportedByBinance
    ) {
        asset.syncMarketData(
                coin.id(),
                coin.symbol(),
                coin.name(),
                coin.image(),
                coin.marketCapRank(),
                coin.currentPrice(),
                coin.priceChangePercentage24h(),
                coin.high24h(),
                coin.low24h(),
                coin.marketCap(),
                coin.totalVolume(),
                coin.circulatingSupply(),
                coin.totalSupply(),
                coin.maxSupply(),
                supportedByBinance,
                syncedAt
        );
    }

    private MarketPair buildBinanceUsdtPair(
            MarketAsset asset,
            Map<String, MarketPair> pairsBySymbol,
            Instant syncedAt
    ) {
        String baseSymbol = asset.getSymbol().toUpperCase(Locale.ROOT);
        String pairSymbol = baseSymbol + USDT_SYMBOL;

        MarketPair pair = pairsBySymbol.getOrDefault(pairSymbol, new MarketPair());
        pair.syncPair(asset, BINANCE_EXCHANGE, baseSymbol, USDT_SYMBOL, pairSymbol, true, syncedAt);
        return pair;
    }
}


