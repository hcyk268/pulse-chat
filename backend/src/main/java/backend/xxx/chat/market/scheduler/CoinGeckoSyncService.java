package backend.xxx.chat.market.scheduler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import backend.xxx.chat.market.client.CoinGeckoClient;
import backend.xxx.chat.market.client.dto.CoinMarketResponse;
import backend.xxx.chat.market.client.dto.CoinTrendingResponse;
import backend.xxx.chat.market.model.MarketAsset;
import backend.xxx.chat.market.model.MarketPair;
import backend.xxx.chat.market.model.MarketTrending;
import backend.xxx.chat.market.repository.MarketAssetRepository;
import backend.xxx.chat.market.repository.MarketPairRepository;
import backend.xxx.chat.market.repository.MarketTrendingRepository;
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
    private final MarketAssetRepository marketAssetRepository;
    private final MarketPairRepository marketPairRepository;
    private final MarketTrendingRepository marketTrendingRepository;

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

        Map<String, MarketAsset> assetsByCoingeckoId = marketAssetRepository
                .findAllByCoingeckoIdIn(toMarketCoinIds(validCoins))
                .stream()
                .collect(Collectors.toMap(MarketAsset::getCoingeckoId, Function.identity()));
        List<MarketAsset> assetsToSave = new ArrayList<>(validCoins.size());

        for (CoinMarketResponse coin : validCoins) {
            MarketAsset asset = assetsByCoingeckoId.getOrDefault(coin.id(), new MarketAsset());
            applyMarketCoin(asset, coin, syncedAt);
            assetsToSave.add(asset);
        }

        List<MarketAsset> savedAssets = marketAssetRepository.saveAll(assetsToSave);
        Map<String, MarketPair> pairsBySymbol = marketPairRepository
                .findAllByExchangeAndSymbolIn(BINANCE_EXCHANGE, toBinancePairSymbols(savedAssets))
                .stream()
                .collect(Collectors.toMap(MarketPair::getSymbol, Function.identity()));
        List<MarketPair> pairsToSave = savedAssets.stream()
                .map(asset -> buildBinanceUsdtPair(asset, pairsBySymbol, syncedAt))
                .toList();
        marketPairRepository.saveAll(pairsToSave);

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
        List<CoinTrendingResponse.CoinTrendingItem> validItems = response.coins().stream()
                .map(CoinTrendingResponse.CoinTrendingEntry::item)
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
                    trending.setAsset(assetsByCoingeckoId.get(item.id()));
                    trending.setCoingeckoId(item.id());
                    trending.setSymbol(item.symbol().toUpperCase(Locale.ROOT));
                    trending.setName(item.name());
                    trending.setThumbUrl(item.thumb());
                    trending.setMarketCapRank(item.marketCapRank());
                    trending.setScore(item.score() == null ? 0 : item.score());
                    trending.setSnapshotAt(snapshotAt);
                    return trending;
                })
                .toList();

        return marketTrendingRepository.saveAll(trendingToSave).size();
    }

    private boolean isValidMarketCoin(CoinMarketResponse coin) {
        return coin != null && coin.id() != null && coin.symbol() != null && coin.name() != null;
    }

    private boolean isValidTrendingItem(CoinTrendingResponse.CoinTrendingItem item) {
        return item != null && item.id() != null && item.symbol() != null && item.name() != null;
    }

    private Collection<String> toMarketCoinIds(List<CoinMarketResponse> coins) {
        return coins.stream()
                .map(CoinMarketResponse::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Collection<String> toTrendingCoinIds(List<CoinTrendingResponse.CoinTrendingItem> items) {
        return items.stream()
                .map(CoinTrendingResponse.CoinTrendingItem::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Collection<String> toBinancePairSymbols(List<MarketAsset> assets) {
        return assets.stream()
                .map(asset -> asset.getSymbol().toUpperCase(Locale.ROOT) + USDT_SYMBOL)
                .collect(Collectors.toSet());
    }

    private void applyMarketCoin(MarketAsset asset, CoinMarketResponse coin, Instant syncedAt) {
        asset.setCoingeckoId(coin.id());
        asset.setSymbol(coin.symbol().toUpperCase(Locale.ROOT));
        asset.setName(coin.name());
        asset.setImageUrl(coin.image());
        asset.setMarketCapRank(coin.marketCapRank());
        asset.setCurrentPriceUsd(coin.currentPrice());
        asset.setPriceChangePercentage24h(coin.priceChangePercentage24h());
        asset.setHigh24h(coin.high24h());
        asset.setLow24h(coin.low24h());
        asset.setMarketCap(coin.marketCap());
        asset.setTotalVolume(coin.totalVolume());
        asset.setCirculatingSupply(coin.circulatingSupply());
        asset.setTotalSupply(coin.totalSupply());
        asset.setMaxSupply(coin.maxSupply());
        asset.setActive(true);
        asset.setLastSyncedAt(syncedAt);
    }

    private MarketPair buildBinanceUsdtPair(
            MarketAsset asset,
            Map<String, MarketPair> pairsBySymbol,
            Instant syncedAt
    ) {
        String baseSymbol = asset.getSymbol().toUpperCase(Locale.ROOT);
        String pairSymbol = baseSymbol + USDT_SYMBOL;

        MarketPair pair = pairsBySymbol.getOrDefault(pairSymbol, new MarketPair());
        pair.setAsset(asset);
        pair.setExchange(BINANCE_EXCHANGE);
        pair.setBaseSymbol(baseSymbol);
        pair.setQuoteSymbol(USDT_SYMBOL);
        pair.setSymbol(pairSymbol);
        pair.setActive(true);
        pair.setLastSyncedAt(syncedAt);
        return pair;
    }
}